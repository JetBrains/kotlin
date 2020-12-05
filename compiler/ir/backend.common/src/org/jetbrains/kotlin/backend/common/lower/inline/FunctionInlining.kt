/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline


import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.getNewWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.coroutinesIntrinsicsPackageFqName
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

fun IrValueParameter.isInlineParameter(type: IrType = this.type) =
    index >= 0 && !isNoinline && !type.isNullable() && (type.isFunction() || type.isSuspendFunction())

interface InlineFunctionResolver {
    fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction
}

fun IrFunction.isTopLevelInPackage(name: String, packageName: String): Boolean {
    if (name != this.name.asString()) return false

    val containingDeclaration = parent as? IrPackageFragment ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}

fun IrFunction.isBuiltInIntercepted(languageVersionSettings: LanguageVersionSettings): Boolean =
    !languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines) &&
            isTopLevelInPackage("intercepted", languageVersionSettings.coroutinesIntrinsicsPackageFqName().asString())

fun IrFunction.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings: LanguageVersionSettings): Boolean =
    isTopLevelInPackage(
        "suspendCoroutineUninterceptedOrReturn",
        languageVersionSettings.coroutinesIntrinsicsPackageFqName().asString()
    )

open class DefaultInlineFunctionResolver(open val context: CommonBackendContext) : InlineFunctionResolver {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val function = symbol.owner
        val languageVersionSettings = context.configuration.languageVersionSettings
        // TODO: Remove these hacks when coroutine intrinsics are fixed.
        return when {
            function.isBuiltInIntercepted(languageVersionSettings) ->
                error("Continuation.intercepted is not available with release coroutines")

            function.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings) ->
                context.ir.symbols.suspendCoroutineUninterceptedOrReturn.owner

            symbol == context.ir.symbols.coroutineContextGetter ->
                context.ir.symbols.coroutineGetContext.owner

            else -> (symbol.owner as? IrSimpleFunction)?.resolveFakeOverride() ?: symbol.owner
        }
    }
}

class FunctionInlining(
    val context: CommonBackendContext,
    val inlineFunctionResolver: InlineFunctionResolver
) : IrElementTransformerVoidWithContext(), BodyLoweringPass {

    constructor(context: CommonBackendContext) : this(context, DefaultInlineFunctionResolver(context))

    private var containerScope: ScopeWithIr? = null

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO container: IrSymbolDeclaration
        containerScope = createScope(container as IrSymbolOwner)
        irBody.accept(this, null)
        containerScope = null

        irBody.patchDeclarationParents(container as? IrDeclarationParent ?: container.parent)
    }

    fun inline(irModule: IrModuleFragment) = irModule.accept(this, data = null)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid(this)
        val callee = when (expression) {
            is IrCall -> expression.symbol.owner
            is IrConstructorCall -> expression.symbol.owner
            else -> return expression
        }
        if (!callee.needsInlining)
            return expression
        if (Symbols.isLateinitIsInitializedPropertyGetter(callee.symbol))
            return expression
        if (Symbols.isTypeOfIntrinsic(callee.symbol))
            return expression

        val actualCallee = inlineFunctionResolver.getFunctionDeclaration(callee.symbol)

        val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().lastOrNull()
            ?: allScopes.map { it.irElement }.filterIsInstance<IrDeclaration>().lastOrNull()?.parent
            ?: containerScope?.irElement as? IrDeclarationParent
            ?: (containerScope?.irElement as? IrDeclaration)?.parent

        val inliner = Inliner(expression, actualCallee, currentScope ?: containerScope!!, parent, context)
        return inliner.inline()
    }

    private val IrFunction.needsInlining get() = this.isInline && !this.isExternal

    private inner class Inliner(
        val callSite: IrFunctionAccessExpression,
        val callee: IrFunction,
        val currentScope: ScopeWithIr,
        val parent: IrDeclarationParent?,
        val context: CommonBackendContext
    ) {

        val copyIrElement = run {
            val typeParameters =
                if (callee is IrConstructor)
                    callee.parentAsClass.typeParameters
                else callee.typeParameters
            val typeArguments =
                (0 until callSite.typeArgumentsCount).associate {
                    typeParameters[it].symbol to callSite.getTypeArgument(it)
                }
            DeepCopyIrTreeWithSymbolsForInliner(typeArguments, parent)
        }

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()

        fun inline() = inlineFunction(callSite, callee, true)

        /**
         * TODO: JVM inliner crashed on attempt inline this function from transform.kt with:
         *  j.l.IllegalStateException: Couldn't obtain compiled function body for
         *  public inline fun <reified T : org.jetbrains.kotlin.ir.IrElement> kotlin.collections.MutableList<T>.transform...
         */
        private inline fun <reified T : IrElement> MutableList<T>.transform(transformation: (T) -> IrElement) {
            forEachIndexed { i, item ->
                set(i, transformation(item) as T)
            }
        }

        private fun inlineFunction(
            callSite: IrFunctionAccessExpression,
            callee: IrFunction,
            performRecursiveInline: Boolean
        ): IrReturnableBlock {
            val copiedCallee = (copyIrElement.copy(callee) as IrFunction).apply {
                parent = callee.parent
                if (performRecursiveInline) {
                    body?.transformChildrenVoid()
                    valueParameters.forEachIndexed { index, param ->
                        if (callSite.getValueArgument(index) == null) {
                            // Default values can recursively reference [callee] - transform only needed.
                            param.defaultValue = param.defaultValue?.transform(this@FunctionInlining, null)
                        }
                    }
                }
            }

            val evaluationStatements = evaluateArguments(callSite, copiedCallee)
            val statements = (copiedCallee.body as IrBlockBody).statements

            val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl(callee.getNewWrappedDescriptor())
            val endOffset = callee.endOffset
            /* creates irBuilder appending to the end of the given returnable block: thus why we initialize
             * irBuilder with (..., endOffset, endOffset).
             */
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, endOffset, endOffset)

            val transformer = ParameterSubstitutor()
            statements.transform { it.transform(transformer, data = null) }
            statements.addAll(0, evaluationStatements)

            return IrReturnableBlockImpl(
                startOffset = callSite.startOffset,
                endOffset = callSite.endOffset,
                type = callSite.type,
                symbol = irReturnableBlockSymbol,
                origin = null,
                statements = statements,
                inlineFunctionSymbol = callee.symbol
            ).apply {
                transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid(this)

                        if (expression.returnTargetSymbol == copiedCallee.symbol)
                            return irBuilder.irReturn(expression.value)
                        return expression
                    }
                })
                patchDeclarationParents(parent) // TODO: Why it is not enough to just run SetDeclarationsParentVisitor?
            }
        }

        //---------------------------------------------------------------------//

        private inner class ParameterSubstitutor : IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val newExpression = super.visitGetValue(expression) as IrGetValue
                val argument = substituteMap[newExpression.symbol.owner] ?: return newExpression

                argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.

                return if (argument is IrGetValueWithoutLocation)
                    argument.withLocation(newExpression.startOffset, newExpression.endOffset)
                else (copyIrElement.copy(argument) as IrExpression)
            }

            //-----------------------------------------------------------------//

            override fun visitCall(expression: IrCall): IrExpression {
                if (!isLambdaCall(expression))
                    return super.visitCall(expression)

                val dispatchReceiver = expression.dispatchReceiver as IrGetValue
                val functionArgument = substituteMap[dispatchReceiver.symbol.owner] ?: return super.visitCall(expression)
                if ((dispatchReceiver.symbol.owner as? IrValueParameter)?.isNoinline == true)
                    return super.visitCall(expression)

                if (functionArgument is IrFunctionReference) {
                    functionArgument.transformChildrenVoid(this)

                    val function = functionArgument.symbol.owner
                    val functionParameters = function.explicitParameters
                    val boundFunctionParameters = functionArgument.getArgumentsWithIr()
                    val unboundFunctionParameters = functionParameters - boundFunctionParameters.map { it.first }
                    val boundFunctionParametersMap = boundFunctionParameters.associate { it.first to it.second }

                    var unboundIndex = 0
                    val unboundArgsSet = unboundFunctionParameters.toSet()
                    val valueParameters = expression.getArgumentsWithIr().drop(1) // Skip dispatch receiver.

                    val superType = functionArgument.type as IrSimpleType
                    val superTypeArgumentsMap = expression.symbol.owner.parentAsClass.typeParameters.associate { typeParam ->
                        typeParam.symbol to superType.arguments[typeParam.index].typeOrNull!!
                    }

                    val immediateCall = with(expression) {
                        when (function) {
                            is IrConstructor -> {
                                val classTypeParametersCount = function.parentAsClass.typeParameters.size
                                IrConstructorCallImpl.fromSymbolOwner(
                                    startOffset,
                                    endOffset,
                                    function.returnType,
                                    function.symbol,
                                    classTypeParametersCount
                                )
                            }
                            is IrSimpleFunction ->
                                IrCallImpl(
                                    startOffset,
                                    endOffset,
                                    function.returnType,
                                    function.symbol,
                                    function.typeParameters.size,
                                    function.valueParameters.size
                                )
                            else ->
                                error("Unknown function kind : ${function.render()}")
                        }
                    }.apply {
                        for (parameter in functionParameters) {
                            val argument =
                                if (parameter !in unboundArgsSet) {
                                    val arg = boundFunctionParametersMap[parameter]!!
                                    if (arg is IrGetValueWithoutLocation)
                                        arg.withLocation(expression.startOffset, expression.endOffset)
                                    else arg
                                } else {
                                    if (unboundIndex == valueParameters.size && parameter.defaultValue != null)
                                        copyIrElement.copy(parameter.defaultValue!!.expression) as IrExpression
                                    else if (!parameter.isVararg) {
                                        assert(unboundIndex < valueParameters.size) {
                                            "Attempt to use unbound parameter outside of the callee's value parameters"
                                        }
                                        valueParameters[unboundIndex++].second
                                    } else {
                                        val elements = mutableListOf<IrVarargElement>()
                                        while (unboundIndex < valueParameters.size) {
                                            val (param, value) = valueParameters[unboundIndex++]
                                            val substitutedParamType = param.type.substitute(superTypeArgumentsMap)
                                            if (substitutedParamType == parameter.varargElementType!!)
                                                elements += value
                                            else
                                                elements += IrSpreadElementImpl(expression.startOffset, expression.endOffset, value)
                                        }
                                        IrVarargImpl(
                                            expression.startOffset, expression.endOffset,
                                            parameter.type,
                                            parameter.varargElementType!!,
                                            elements
                                        )
                                    }
                                }
                            when (parameter) {
                                function.dispatchReceiverParameter ->
                                    this.dispatchReceiver = argument.implicitCastIfNeededTo(function.dispatchReceiverParameter!!.type)

                                function.extensionReceiverParameter ->
                                    this.extensionReceiver = argument.implicitCastIfNeededTo(function.extensionReceiverParameter!!.type)

                                else ->
                                    putValueArgument(
                                        parameter.index,
                                        argument.implicitCastIfNeededTo(function.valueParameters[parameter.index].type)
                                    )
                            }
                        }
                        assert(unboundIndex == valueParameters.size) { "Not all arguments of the callee are used" }
                        for (index in 0 until functionArgument.typeArgumentsCount)
                            putTypeArgument(index, functionArgument.getTypeArgument(index))
                    }.implicitCastIfNeededTo(expression.type)
                    return this@FunctionInlining.visitExpression(super.visitExpression(immediateCall))
                }
                if (functionArgument !is IrFunctionExpression)
                    return super.visitCall(expression)

                // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
                val newExpression = inlineFunction(
                    expression,
                    functionArgument.function,
                    false
                )
                // Substitute lambda arguments with target function arguments.
                return newExpression.transform(
                    this,
                    null
                )
            }

            //-----------------------------------------------------------------//

            override fun visitElement(element: IrElement) = element.accept(this, null)
        }

        private fun IrExpression.implicitCastIfNeededTo(type: IrType) =
            if (type == this.type)
                this
            else
                IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, this)

        private fun isLambdaCall(irCall: IrCall): Boolean {
            val callee = irCall.symbol.owner
            val dispatchReceiver = callee.dispatchReceiverParameter ?: return false
            assert(!dispatchReceiver.type.isKFunction())

            return (dispatchReceiver.type.isFunction() || dispatchReceiver.type.isSuspendFunction())
                    && callee.name == OperatorNameConventions.INVOKE
                    && irCall.dispatchReceiver is IrGetValue
        }

        //-------------------------------------------------------------------------//

        private inner class ParameterToArgument(
            val parameter: IrValueParameter,
            val argumentExpression: IrExpression
        ) {

            val isInlinableLambdaArgument: Boolean
                get() = parameter.isInlineParameter() &&
                        (argumentExpression is IrFunctionReference
                                || argumentExpression is IrFunctionExpression)

            val isImmutableVariableLoad: Boolean
                get() = argumentExpression.let { argument ->
                    argument is IrGetValue && !argument.symbol.owner.let { it is IrVariable && it.isVar }
                }
        }

        // callee might be a copied version of callsite.symbol.owner
        private fun buildParameterToArgument(callSite: IrFunctionAccessExpression, callee: IrFunction): List<ParameterToArgument> {

            val parameterToArgument = mutableListOf<ParameterToArgument>()

            if (callSite.dispatchReceiver != null && callee.dispatchReceiverParameter != null)
                parameterToArgument += ParameterToArgument(
                    parameter = callee.dispatchReceiverParameter!!,
                    argumentExpression = callSite.dispatchReceiver!!
                )

            val valueArguments =
                callSite.symbol.owner.valueParameters.map { callSite.getValueArgument(it.index) }.toMutableList()

            if (callee.extensionReceiverParameter != null) {
                parameterToArgument += ParameterToArgument(
                    parameter = callee.extensionReceiverParameter!!,
                    argumentExpression = if (callSite.extensionReceiver != null) {
                        callSite.extensionReceiver!!
                    } else {
                        // Special case: lambda with receiver is called as usual lambda:
                        valueArguments.removeAt(0)!!
                    }
                )
            } else if (callSite.extensionReceiver != null) {
                // Special case: usual lambda is called as lambda with receiver:
                valueArguments.add(0, callSite.extensionReceiver!!)
            }

            val parametersWithDefaultToArgument = mutableListOf<ParameterToArgument>()
            for (parameter in callee.valueParameters) {
                val argument = valueArguments[parameter.index]
                when {
                    argument != null -> {
                        parameterToArgument += ParameterToArgument(
                            parameter = parameter,
                            argumentExpression = argument
                        )
                    }

                    // After ExpectDeclarationsRemoving pass default values from expect declarations
                    // are represented correctly in IR.
                    parameter.defaultValue != null -> {  // There is no argument - try default value.
                        parametersWithDefaultToArgument += ParameterToArgument(
                            parameter = parameter,
                            argumentExpression = parameter.defaultValue!!.expression
                        )
                    }

                    parameter.varargElementType != null -> {
                        val emptyArray = IrVarargImpl(
                            startOffset = callSite.startOffset,
                            endOffset = callSite.endOffset,
                            type = parameter.type,
                            varargElementType = parameter.varargElementType!!
                        )
                        parameterToArgument += ParameterToArgument(
                            parameter = parameter,
                            argumentExpression = emptyArray
                        )
                    }

                    else -> {
                        val message = "Incomplete expression: call to ${callee.render()} " +
                                "has no argument at index ${parameter.index}"
                        throw Error(message)
                    }
                }
            }
            // All arguments except default are evaluated at callsite,
            // but default arguments are evaluated inside callee.
            return parameterToArgument + parametersWithDefaultToArgument
        }

        //-------------------------------------------------------------------------//

        private fun evaluateArguments(functionReference: IrFunctionReference): List<IrStatement> {
            val arguments = functionReference.getArgumentsWithIr().map { ParameterToArgument(it.first, it.second) }
            val evaluationStatements = mutableListOf<IrStatement>()
            val substitutor = ParameterSubstitutor()
            val referenced = functionReference.symbol.owner
            arguments.forEach {
                val newArgument = if (it.isImmutableVariableLoad) {
                    it.argumentExpression.transform( // Arguments may reference the previous ones - substitute them.
                        substitutor,
                        data = null
                    )
                } else {
                    val newVariable =
                        currentScope.scope.createTemporaryVariable(
                            irExpression = it.argumentExpression.transform( // Arguments may reference the previous ones - substitute them.
                                substitutor,
                                data = null
                            ),
                            nameHint = callee.symbol.owner.name.toString(),
                            isMutable = false
                        )

                    evaluationStatements.add(newVariable)

                    IrGetValueWithoutLocation(newVariable.symbol)
                }
                when (it.parameter) {
                    referenced.dispatchReceiverParameter -> functionReference.dispatchReceiver = newArgument
                    referenced.extensionReceiverParameter -> functionReference.extensionReceiver = newArgument
                    else -> functionReference.putValueArgument(it.parameter.index, newArgument)
                }
            }
            return evaluationStatements
        }

        private fun evaluateArguments(callSite: IrFunctionAccessExpression, callee: IrFunction): List<IrStatement> {
            val arguments = buildParameterToArgument(callSite, callee)
            val evaluationStatements = mutableListOf<IrStatement>()
            val substitutor = ParameterSubstitutor()
            arguments.forEach { argument ->
                /*
                 * We need to create temporary variable for each argument except inlinable lambda arguments.
                 * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
                 * not only for those referring to inlinable lambdas.
                 */
                if (argument.isInlinableLambdaArgument) {
                    substituteMap[argument.parameter] = argument.argumentExpression
                    (argument.argumentExpression as? IrFunctionReference)?.let { evaluationStatements += evaluateArguments(it) }
                    return@forEach
                }

                if (argument.isImmutableVariableLoad) {
                    substituteMap[argument.parameter] =
                        argument.argumentExpression.transform( // Arguments may reference the previous ones - substitute them.
                            substitutor,
                            data = null
                        )
                    return@forEach
                }

                // Arguments may reference the previous ones - substitute them.
                val variableInitializer = argument.argumentExpression.transform(substitutor, data = null)

                val newVariable =
                    currentScope.scope.createTemporaryVariable(
                        irExpression = IrBlockImpl(
                            variableInitializer.startOffset,
                            variableInitializer.endOffset,
                            variableInitializer.type,
                            InlinerExpressionLocationHint((currentScope.irElement as IrSymbolOwner).symbol)
                        ).apply {
                            statements.add(variableInitializer)
                        },
                        nameHint = callee.symbol.owner.name.toString(),
                        isMutable = false
                    )

                evaluationStatements.add(newVariable)
                substituteMap[argument.parameter] = IrGetValueWithoutLocation(newVariable.symbol)
            }
            return evaluationStatements
        }
    }

    private class IrGetValueWithoutLocation(
        override val symbol: IrValueSymbol,
        override val origin: IrStatementOrigin? = null
    ) : IrGetValue() {
        override val startOffset: Int get() = UNDEFINED_OFFSET
        override val endOffset: Int get() = UNDEFINED_OFFSET

        override var type: IrType
            get() = symbol.owner.type
            set(value) {
                symbol.owner.type = value
            }

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D) =
            visitor.visitGetValue(this, data)

        override fun copy(): IrGetValue {
            TODO("not implemented")
        }

        fun withLocation(startOffset: Int, endOffset: Int) =
            IrGetValueImpl(startOffset, endOffset, type, symbol, origin)
    }
}

class InlinerExpressionLocationHint(val inlineAtSymbol: IrSymbol) : IrStatementOrigin {
    override fun toString(): String =
        "(${this.javaClass.simpleName} : $functionNameOrDefaultToString @${functionFileOrNull?.fileEntry?.name})"

    private val functionFileOrNull: IrFile?
        get() = (inlineAtSymbol as? IrFunction)?.file

    private val functionNameOrDefaultToString: String
        get() = (inlineAtSymbol as? IrFunction)?.name?.asString() ?: inlineAtSymbol.toString()
}
