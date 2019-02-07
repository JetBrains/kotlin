/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FoldInitializerAndIfToElvis")

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.reportWarning
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.fileEntry
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.irTypeKotlinBuiltIns
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil

typealias Context = JsIrBackendContext

// backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/FunctionInlining.kt
internal class FunctionInlining(val context: Context): IrElementTransformerVoidWithContext() {
    //-------------------------------------------------------------------------//

    fun inline(irModule: IrModuleFragment): IrElement {
        irTypeKotlinBuiltIns = irModule.irBuiltins.builtIns
        return irModule.accept(this, data = null)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callSite = super.visitCall(expression) as IrCall
        val functionDescriptor = callSite.descriptor
        if (!functionDescriptor.needsInlining)
            return callSite                                // This call does not need inlining.

        val callee = getFunctionDeclaration(callSite.symbol)                   // Get declaration of the function to be inlined.
        callee.transformChildrenVoid(this)                            // Process recursive inline.

        val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().lastOrNull()
        val inliner = Inliner(callSite, callee, currentScope!!, parent, context)
        // Create inliner for this scope.
        return inliner.inline()                                  // Return newly created IrInlineBody instead of IrCall.
    }

    //-------------------------------------------------------------------------//

    private fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {

        val descriptor = symbol.descriptor.original
//        val originalDescriptor = functionDescriptor.resolveFakeOverride().original

        val languageVersionSettings = context.configuration.languageVersionSettings

        // TODO: Remove these hacks when coroutine intrinsics are fixed.
        return when {
//            descriptor.isBuiltInIntercepted(languageVersionSettings) ->
//                error("Continuation.intercepted is not available with release coroutines")
//
//            descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings) ->
//                context.ir.symbols.konanSuspendCoroutineUninterceptedOrReturn.owner
//
//            descriptor == context.ir.symbols.coroutineContextGetter ->
//                context.ir.symbols.konanCoroutineContextGetter.owner

            else -> (symbol.owner as? IrSimpleFunction)?.resolveFakeOverride() ?: symbol.owner
        }
    }


    // TODO: should we keep this at all?
    private val inlineConstructor = FqName("kotlin.native.internal.InlineConstructor")
    private val FunctionDescriptor.isInlineConstructor get() = annotations.hasAnnotation(inlineConstructor)

//-----------------------------------------------------------------------------//

    private inner class Inliner(
        val callSite: IrCall,
        val callee: IrFunction,
        val currentScope: ScopeWithIr,
        val parent: IrDeclarationParent?,
        val context: Context
    ) {

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

        val substituteMap = mutableMapOf<ValueDescriptor, IrExpression>()

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

        private fun inlineFunction(callSite: IrCall, callee: IrFunction): IrReturnableBlockImpl {
            val copiedCallee = copyIrElement.copy(callee) as IrFunction

            val evaluationStatements = evaluateArguments(callSite, copiedCallee)
            val statements = (copiedCallee.body as IrBlockBody).statements

            val irReturnableBlockSymbol = IrReturnableBlockSymbolImpl(copiedCallee.descriptor.original)
            val descriptor = callee.descriptor.original
            val startOffset = callee.startOffset
            val endOffset = callee.endOffset
            val irBuilder = context.createIrBuilder(irReturnableBlockSymbol, startOffset, endOffset)

            if (descriptor.isInlineConstructor) {
                val delegatingConstructorCall = statements[0] as IrDelegatingConstructorCall
                irBuilder.run {
                    val constructorDescriptor = delegatingConstructorCall.descriptor.original
                    val constructorCall = irCall(delegatingConstructorCall.symbol, callSite.type,
                                                 constructorDescriptor.typeParameters.map { delegatingConstructorCall.getTypeArgument(it)!! }).apply {
                        constructorDescriptor.valueParameters.forEach {
                            putValueArgument(
                                it,
                                delegatingConstructorCall.getValueArgument(it)
                            )
                        }
                    }
                    val oldThis = delegatingConstructorCall.descriptor.constructedClass.thisAsReceiverParameter
                    val newThis = currentScope.scope.createTemporaryVariable(
                        irExpression = constructorCall,
                        nameHint = delegatingConstructorCall.descriptor.fqNameSafe.toString() + ".this"
                    )
                    statements[0] = newThis
                    substituteMap[oldThis] = irGet(newThis)
                    statements.add(irReturn(irGet(newThis)))
                }
            }

            val transformer = ParameterSubstitutor()
            statements.transform { it.transform(transformer, data = null) }
            statements.addAll(0, evaluationStatements)

            return IrReturnableBlockImpl(
                startOffset    = startOffset,
                endOffset      = endOffset,
                type           = copiedCallee.returnType,
                symbol         = irReturnableBlockSymbol,
                origin         = null,
                statements     = statements,
                sourceFileSymbol = callee.file.symbol
            ).apply {
                transformChildrenVoid(object: IrElementTransformerVoid() {
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
                val descriptor = newExpression.descriptor
                val argument = substituteMap[descriptor] ?: return newExpression

                argument.transformChildrenVoid(this) // Default argument can contain subjects for substitution.
                return copyIrElement.copy(argument) as IrExpression
            }

            //-----------------------------------------------------------------//

            private val IrFunctionReference.isLambda: Boolean
                get() {
                    return symbol.owner.visibility == Visibilities.LOCAL && origin == IrStatementOrigin.LAMBDA
                }

            override fun visitCall(expression: IrCall): IrExpression {
                if (!isLambdaCall(expression))
                    return super.visitCall(expression)

                val dispatchReceiver = expression.dispatchReceiver as IrGetValue
                val functionArgument = substituteMap[dispatchReceiver.descriptor]
                if (functionArgument == null)
                    return super.visitCall(expression)
                val dispatchDescriptor = dispatchReceiver.descriptor
                if (dispatchDescriptor is ValueParameterDescriptor &&
                    dispatchDescriptor.isNoinline
                ) return super.visitCall(expression)

                if (functionArgument is IrFunctionReference) {
                    if (!functionArgument.isLambda) return super.visitCall(expression)

                    val functionDescriptor = functionArgument.descriptor
                    val functionParameters = functionDescriptor.explicitParameters
                    val boundFunctionParameters = functionArgument.getArguments()
                    val unboundFunctionParameters = functionParameters - boundFunctionParameters.map { it.first }
                    val boundFunctionParametersMap = boundFunctionParameters.associate { it.first to it.second }

                    var unboundIndex = 0
                    val unboundArgsSet = unboundFunctionParameters.toSet()
                    val valueParameters = expression.getArguments().drop(1) // Skip dispatch receiver.

                    val immediateCall = IrCallImpl(
                        startOffset = expression.startOffset,
                        endOffset = expression.endOffset,
                        type = expression.type,
                        symbol = functionArgument.symbol,
                        descriptor = functionArgument.descriptor
                    ).apply {
                        functionParameters.forEach {
                            val argument =
                                if (!unboundArgsSet.contains(it))
                                    boundFunctionParametersMap[it]!!
                                else
                                    valueParameters[unboundIndex++].second
                            when (it) {
                                functionDescriptor.dispatchReceiverParameter -> this.dispatchReceiver = argument
                                functionDescriptor.extensionReceiverParameter -> this.extensionReceiver = argument
                                else -> putValueArgument((it as ValueParameterDescriptor).index, argument)
                            }
                        }
                        assert(unboundIndex == valueParameters.size) { "Not all arguments of <invoke> are used" }
                    }
                    return this@FunctionInlining.visitCall(super.visitCall(immediateCall) as IrCall)
                }
                if (functionArgument !is IrBlock)
                    return super.visitCall(expression)

                val functionDeclaration = functionArgument.statements[0] as IrFunction
                val newExpression = inlineFunction(
                    expression,
                    functionDeclaration
                )             // Inline the lambda. Lambda parameters will be substituted with lambda arguments.
                return newExpression.transform(
                    this,
                    null
                )                                      // Substitute lambda arguments with target function arguments.
            }

            //-----------------------------------------------------------------//

            override fun visitElement(element: IrElement) = element.accept(this, null)
        }

        private fun isLambdaCall(irCall: IrCall) = irCall.symbol.owner.isFunctionInvoke && irCall.dispatchReceiver is IrGetValue

        //-------------------------------------------------------------------------//

        private inner class ParameterToArgument(
            val parameter: IrValueParameter,
            val argumentExpression: IrExpression
        ) {

            val isInlinableLambdaArgument: Boolean
                get() {
                    if (!InlineUtil.isInlineParameter(parameter.descriptor)) return false
                    if (argumentExpression is IrFunctionReference
                        && !argumentExpression.descriptor.isSuspend
                    ) return true // Skip suspend functions for now since it's not supported by FE anyway.

                    // Do pattern-matching on IR.
                    if (argumentExpression !is IrBlock) return false
                    if (argumentExpression.origin != IrStatementOrigin.LAMBDA &&
                        argumentExpression.origin != IrStatementOrigin.ANONYMOUS_FUNCTION
                    ) return false
                    val statements = argumentExpression.statements
                    val irFunction = statements[0]
                    val irCallableReference = statements[1]
                    if (irFunction !is IrFunction) return false
                    if (irCallableReference !is IrCallableReference) return false
                    return true
                }

            val isImmutableVariableLoad: Boolean
                get() = argumentExpression.let {
                    it is IrGetValue && !it.symbol.owner.let { v -> v is IrVariable && v.isVar }
                }
        }

        //-------------------------------------------------------------------------//

        private fun buildParameterToArgument(callSite: IrCall, callee: IrFunction): List<ParameterToArgument> {

            val parameterToArgument = mutableListOf<ParameterToArgument>()

            if (callSite.dispatchReceiver != null &&                 // Only if there are non null dispatch receivers both
                callee.dispatchReceiverParameter != null
            )            // on call site and in function declaration.
                parameterToArgument += ParameterToArgument(
                    parameter = callee.dispatchReceiverParameter!!,
                    argumentExpression = callSite.dispatchReceiver!!
                )

            val valueArguments =
                callSite.descriptor.valueParameters.map { callSite.getValueArgument(it) }.toMutableList()

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

        private fun evaluateArguments(callSite: IrCall, callee: IrFunction): List<IrStatement> {

            val parameterToArgumentOld = buildParameterToArgument(callSite, callee)
            val evaluationStatements = mutableListOf<IrStatement>()
            val substitutor = ParameterSubstitutor()
            parameterToArgumentOld.forEach {
                val parameterDescriptor = it.parameter.descriptor

                /*
             * We need to create temporary variable for each argument except inlinable lambda arguments.
             * For simplicity and to produce simpler IR we don't create temporaries for every immutable variable,
             * not only for those referring to inlinable lambdas.
             */
                if (it.isInlinableLambdaArgument) {
                    substituteMap[parameterDescriptor] = it.argumentExpression
                    return@forEach
                }

                if (it.isImmutableVariableLoad) {
                    substituteMap[parameterDescriptor] = it.argumentExpression.transform(
                        substitutor,
                        data = null
                    )   // Arguments may reference the previous ones - substitute them.
                    return@forEach
                }

                val newVariable = currentScope.scope.createTemporaryVariable(
                    irExpression = it.argumentExpression.transform(
                        substitutor,
                        data = null
                    ),   // Arguments may reference the previous ones - substitute them.
                    nameHint = callee.descriptor.name.toString(),
                    isMutable = false
                )

                evaluationStatements.add(newVariable)
                val getVal = IrGetValueImpl(
                    startOffset = currentScope.irElement.startOffset,
                    endOffset = currentScope.irElement.endOffset,
                    type = newVariable.type,
                    symbol = newVariable.symbol
                )
                substituteMap[parameterDescriptor] = getVal
            }
            return evaluationStatements
        }
    }
}