/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.asInlinableFunctionReference
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.util.OperatorNameConventions

internal val directInvokeLowering = makeIrFilePhase(
    ::DirectInvokeLowering,
    name = "DirectInvokes",
    description = "Inline directly invoked lambdas and replace invoked function references with calls"
)

private class DirectInvokeLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitCall(expression: IrCall): IrExpression {
        val function = expression.symbol.owner
        val receiver = expression.dispatchReceiver
        if (receiver == null || function.name != OperatorNameConventions.INVOKE)
            return super.visitCall(expression)

        val result = when {
            // TODO deal with type parameters somehow?
            // It seems we can't encounter them in the code written by user,
            // but this might be important later if we actually perform inlining and optimizations on IR.
            receiver is IrFunctionReference && receiver.symbol.owner.typeParameters.isEmpty() ->
                visitFunctionReferenceInvoke(expression, receiver)

            receiver is IrBlock ->
                receiver.asInlinableFunctionReference()?.takeIf { it.extensionReceiver == null }?.let { reference ->
                    visitLambdaInvoke(expression, reference)
                } ?: expression

            else ->
                expression
        }

        result.transformChildrenVoid()
        return result
    }

    private fun visitLambdaInvoke(expression: IrCall, reference: IrFunctionReference): IrExpression {
        val scope = currentScope!!.scope
        val declarationParent = scope.getLocalDeclarationParent()
        val function = reference.symbol.owner
        if (expression.valueArgumentsCount == 0) {
            return function.inline(declarationParent)
        }
        return context.createIrBuilder(scope.scopeOwnerSymbol).run {
            at(expression)
            irBlock {
                val arguments = function.explicitParameters.mapIndexed { index, parameter ->
                    val argument = expression.getValueArgument(index)!!
                    IrVariableImpl(
                        argument.startOffset, argument.endOffset, IrDeclarationOrigin.DEFINED, IrVariableSymbolImpl(), parameter.name,
                        parameter.type, isVar = false, isConst = false, isLateinit = false
                    ).apply {
                        parent = declarationParent
                        initializer = argument
                        +this
                    }
                }
                +function.inline(declarationParent, arguments)
            }
        }
    }

    private fun visitFunctionReferenceInvoke(expression: IrCall, receiver: IrFunctionReference): IrExpression =
        when (val irFun = receiver.symbol.owner) {
            is IrSimpleFunction ->
                IrCallImpl(
                    expression.startOffset, expression.endOffset, expression.type, irFun.symbol,
                    typeArgumentsCount = irFun.typeParameters.size, valueArgumentsCount = irFun.valueParameters.size
                ).apply {
                    copyReceiverAndValueArgumentsForDirectInvoke(receiver, expression)
                }

            is IrConstructor ->
                IrConstructorCallImpl(
                    expression.startOffset, expression.endOffset, expression.type, irFun.symbol,
                    typeArgumentsCount = irFun.typeParameters.size,
                    constructorTypeArgumentsCount = 0,
                    valueArgumentsCount = irFun.valueParameters.size
                ).apply {
                    copyReceiverAndValueArgumentsForDirectInvoke(receiver, expression)
                }

            else ->
                throw AssertionError("Simple function or constructor expected: ${irFun.render()}")
        }

    private fun IrFunctionAccessExpression.copyReceiverAndValueArgumentsForDirectInvoke(
        irFunRef: IrFunctionReference,
        irInvokeCall: IrFunctionAccessExpression
    ) {
        val irFun = irFunRef.symbol.owner
        var invokeArgIndex = 0
        if (irFun.dispatchReceiverParameter != null) {
            dispatchReceiver = irFunRef.dispatchReceiver ?: irInvokeCall.getValueArgument(invokeArgIndex++)
        }
        if (irFun.extensionReceiverParameter != null) {
            extensionReceiver = irFunRef.extensionReceiver ?: irInvokeCall.getValueArgument(invokeArgIndex++)
        }
        if (invokeArgIndex + valueArgumentsCount != irInvokeCall.valueArgumentsCount) {
            throw AssertionError("Mismatching value arguments: $invokeArgIndex arguments used for receivers\n${irInvokeCall.dump()}")
        }
        for (i in 0 until valueArgumentsCount) {
            putValueArgument(i, irInvokeCall.getValueArgument(invokeArgIndex++))
        }
    }
}
