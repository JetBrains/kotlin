/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline


import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.createTemporaryVariableWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.CoroutineIntrinsicLambdaOrigin
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions

class FunctionInlining(val context: CommonBackendContext) : IrElementTransformerVoidWithContext() {

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

        val actualCallee = getFunctionDeclaration(callee.symbol)

        actualCallee.transformChildrenVoid(this)                            // Process recursive inline.

        val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().lastOrNull()
        val inliner = Inliner(expression, actualCallee, currentScope!!, parent, context)
        return inliner.inline()
    }

    private fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val descriptor = symbol.descriptor.original
        val languageVersionSettings = context.configuration.languageVersionSettings
        // TODO: Remove these hacks when coroutine intrinsics are fixed.
        return when {
            descriptor.isBuiltInIntercepted(languageVersionSettings) ->
                error("Continuation.intercepted is not available with release coroutines")

            descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings) ->
                context.ir.symbols.suspendCoroutineUninterceptedOrReturn.owner

            symbol == context.ir.symbols.coroutineContextGetter ->
                context.ir.symbols.coroutineGetContext.owner

            else -> (symbol.owner as? IrSimpleFunction)?.resolveFakeOverride() ?: symbol.owner
        }
    }

    private val IrFunction.needsInlining get() = (this.isInline && !this.isExternal)

    private inner class Inliner(val callSite: IrFunctionAccessExpression,
                                val callee: IrFunction,
                                val currentScope: ScopeWithIr,
                                val parent: IrDeclarationParent?,
                                val context: CommonBackendContext) {

        val copyIrElement = run {
            val typeParameters =
                if (callee is IrConstructor)
                    callee.parentAsClass.typeParameters
                else callee.typeParameters
            val typeArguments =
                (0 until callSite.typeArgumentsCount).map {
                    typeParameters[it].symbol to callSite.getTypeArgument(it)
                }.associate { it }
            DeepCopyIrTreeWithSymbolsForInliner(context, typeArguments, parent)
        }

        val substituteMap = mutableMapOf<IrValueParameter, IrExpression>()

        fun inline() = inlineFunction(callSite, callee)

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

        private fun inlineFunction(callSite: IrFunctionAccessExpression, callee: IrFunction): IrReturnableBlock {
            val copiedCallee = copyIrElement.copy(callee) as IrFunction

            val evaluationStatements = evaluateArguments(callSite, copiedCallee)
            val statements = (copiedCallee.body as IrBlockBody).statements

            val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl(copiedCallee.descriptor.original)
            val startOffset = callee.startOffset
            val endOffset = callee.endOffset
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, startOffset, endOffset)

            val transformer = ParameterSubstitutor()
            statements.transform { it.transform(transformer, data = null) }
            statements.addAll(0, evaluationStatements)

            val isCoroutineIntrinsicCall = callSite.descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(
                context.configuration.languageVersionSettings)

            return IrReturnableBlockImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = callSite.type,
                symbol = irReturnableBlockSymbol,
                origin = if (isCoroutineIntrinsicCall) CoroutineIntrinsicLambdaOrigin else null,
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
            }
        }

        //---------------------------------------------------------------------//

        private inner class ParameterSubstitutor : IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val newExpression = super.visitGetValue(expression) as IrGetValue
                val argument = substituteMap[newExpression.symbol.owner] ?: return newExpression

                argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.
                return copyIrElement.copy(argument) as IrExpression
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
                    val function = functionArgument.symbol.owner
                    val functionParameters = function.explicitParameters
                    val boundFunctionParameters = functionArgument.getArgumentsWithIr()
                    val unboundFunctionParameters = functionParameters - boundFunctionParameters.map { it.first }
                    val boundFunctionParametersMap = boundFunctionParameters.associate { it.first to it.second }

                    var unboundIndex = 0
                    val unboundArgsSet = unboundFunctionParameters.toSet()
                    val valueParameters = expression.getArgumentsWithIr().drop(1) // Skip dispatch receiver.

                    val immediateCall = with(expression) {
                        if (function is IrConstructor) {
                            IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, function.symbol)
                        } else {
                            IrCallImpl(startOffset, endOffset, type, functionArgument.symbol)
                        }
                    }.apply {
                        functionParameters.forEach {
                            val argument =
                                if (it !in unboundArgsSet)
                                    boundFunctionParametersMap[it]!!
                                else
                                    valueParameters.getOrNull(unboundIndex++)?.second
                            when (it) {
                                function.dispatchReceiverParameter -> this.dispatchReceiver = argument
                                function.extensionReceiverParameter -> this.extensionReceiver = argument
                                else -> putValueArgument(it.index, argument)
                            }
                        }
                        assert(unboundIndex >= valueParameters.size) { "Not all arguments of <invoke> are used" }
                        for (index in 0 until functionArgument.typeArgumentsCount)
                            putTypeArgument(index, functionArgument.getTypeArgument(index))
                    }

                    return this@FunctionInlining.visitExpression(super.visitExpression(immediateCall))
                }
                if (functionArgument !is IrBlock)
                    return super.visitCall(expression)

                val functionDeclaration = functionArgument.statements[0] as IrFunction
                val newExpression = inlineFunction(expression, functionDeclaration) // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
                return newExpression.transform(this, null)                          // Substitute lambda arguments with target function arguments.
            }

            //-----------------------------------------------------------------//

            override fun visitElement(element: IrElement) = element.accept(this, null)
        }

        private fun isLambdaCall(irCall: IrFunctionAccessExpression): Boolean {
            val callee = irCall.symbol.owner
            val dispatchReceiver = callee.dispatchReceiverParameter ?: return false
            assert(!dispatchReceiver.type.isKFunction())

            return (dispatchReceiver.type.isFunction() || dispatchReceiver.type.isSuspendFunction())
                    && callee.name == OperatorNameConventions.INVOKE
                    && irCall.dispatchReceiver is IrGetValue
        }

        //-------------------------------------------------------------------------//

        private fun IrValueParameter.isInlineParameter() =
            !isNoinline && !type.isNullable() && type.isFunctionOrKFunction()

        private inner class ParameterToArgument(val parameter: IrValueParameter,
                                                val argumentExpression: IrExpression) {

            val isInlinableLambdaArgument: Boolean
                get() {
                    if (!parameter.isInlineParameter()) return false
                    if (argumentExpression is IrFunctionReference) return true

                    // Do pattern-matching on IR.
                    if (argumentExpression !is IrBlock) return false
                    if (argumentExpression.origin != IrStatementOrigin.LAMBDA &&
                        argumentExpression.origin != IrStatementOrigin.ANONYMOUS_FUNCTION) return false
                    val statements = argumentExpression.statements
                    val irFunction = statements[0]
                    val irCallableReference = statements[1]
                    if (irFunction !is IrFunction) return false
                    if (irCallableReference !is IrCallableReference) return false
                    return true
                }

            val isImmutableVariableLoad: Boolean
                get() = argumentExpression.let {
                    it is IrGetValue && !it.symbol.owner.let { it is IrVariable && it.isVar }
                }
        }

        //-------------------------------------------------------------------------//

        private fun buildParameterToArgument(callSite: IrFunctionAccessExpression, callee: IrFunction): List<ParameterToArgument> {

            val parameterToArgument = mutableListOf<ParameterToArgument>()

            if (callSite.dispatchReceiver != null &&                 // Only if there are non null dispatch receivers both
                callee.dispatchReceiverParameter != null)        // on call site and in function declaration.
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
                        val message = "Incomplete expression: call to ${callee.descriptor} " +
                                "has no argument at index ${parameter.index}"
                        throw Error(message)
                    }
                }
            }
            return parameterToArgument + parametersWithDefaultToArgument  // All arguments except default are evaluated at callsite,
            // but default arguments are evaluated inside callee.
        }

        //-------------------------------------------------------------------------//

        private fun evaluateArguments(callSite: IrFunctionAccessExpression, callee: IrFunction): List<IrStatement> {

            val parameterToArgumentOld = buildParameterToArgument(callSite, callee)
            val evaluationStatements = mutableListOf<IrStatement>()
            val substitutor = ParameterSubstitutor()
            parameterToArgumentOld.forEach {
                /*
                 * We need to create temporary variable for each argument except inlinable lambda arguments.
                 * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
                 * not only for those referring to inlinable lambdas.
                 */
                if (it.isInlinableLambdaArgument) {
                    substituteMap[it.parameter] = it.argumentExpression
                    return@forEach
                }

                if (it.isImmutableVariableLoad) {
                    substituteMap[it.parameter] = it.argumentExpression.transform(substitutor, data = null)   // Arguments may reference the previous ones - substitute them.
                    return@forEach
                }

                val newVariable = currentScope.scope.createTemporaryVariableWithWrappedDescriptor(  // Create new variable and init it with the parameter expression.
                    irExpression = it.argumentExpression.transform(substitutor, data = null),   // Arguments may reference the previous ones - substitute them.
                    nameHint = callee.symbol.owner.name.toString(),
                    isMutable = false)

                evaluationStatements.add(newVariable)
                val getVal = IrGetValueImpl(
                    startOffset = currentScope.irElement.startOffset,
                    endOffset = currentScope.irElement.endOffset,
                    type = newVariable.type,
                    symbol = newVariable.symbol
                )
                substituteMap[it.parameter] = getVal
            }
            return evaluationStatements
        }
    }
}
