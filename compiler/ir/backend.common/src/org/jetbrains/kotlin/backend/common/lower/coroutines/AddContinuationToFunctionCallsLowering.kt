/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger.Severity
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.irMessageLogger
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Add continuation to suspend function calls.
 *
 * Additionally materialize continuation for `getContinuation` intrinsic calls.
 */
abstract class AbstractAddContinuationToFunctionCallsLowering : BodyLoweringPass {
    protected abstract val context: CommonBackendContext
    protected abstract val partialLinkageEnabled: Boolean

    protected abstract fun IrSimpleFunction.isContinuationItself(): Boolean

    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val continuation: IrValueParameter? by lazy {
            (container as IrSimpleFunction).getContinuationParameter()
        }

        val builder by lazy { context.createIrBuilder(container.symbol) }
        fun getContinuation(): IrGetValue? = continuation?.let(builder::irGet)

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBody(body: IrBody): IrBody {
                // Nested bodies are covered by separate `lower` invocation
                return body
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                if (!expression.isSuspend) {
                    if (expression.symbol == context.ir.symbols.getContinuation)
                        return getContinuation() ?: expression.throwLinkageError()
                    return expression
                }

                val oldFun = expression.symbol.owner
                val newFun: IrSimpleFunction = oldFun.getOrCreateFunctionWithContinuationStub(context)

                return irCall(
                    expression,
                    newFun.symbol,
                    newReturnType = newFun.returnType,
                    newSuperQualifierSymbol = expression.superQualifierSymbol
                ).also {
                    it.putValueArgument(it.valueArgumentsCount - 1, getContinuation() ?: return expression.throwLinkageError())
                }
            }
        })
    }

    // IMPORTANT: May return null only if partial linkage is turned on.
    private fun IrSimpleFunction.getContinuationParameter(): IrValueParameter? {
        if (isContinuationItself())
            return dispatchReceiverParameter!!
        else {
            val isLoweredSuspendFunction = origin == IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION
            if (!isLoweredSuspendFunction) {
                return if (partialLinkageEnabled)
                    null
                else
                    throw IllegalArgumentException("Continuation parameter only exists in lowered suspend functions, but function origin is $origin")
            }

            val continuation = valueParameters.lastOrNull()
            require(continuation != null && continuation.origin == IrDeclarationOrigin.CONTINUATION) {
                "Continuation parameter is expected to be the last one"
            }
            return continuation
        }
    }

    private fun IrExpression.throwLinkageError(): IrCall {
        val errorMessage = "Suspend expression can be called only from a coroutine or another suspend function" // TODO: compute verbose error message
        val locationInSourceCode: IrMessageLogger.Location? = null // TODO: compute location

        val messageLogger = context.configuration.irMessageLogger
        messageLogger.report(Severity.WARNING, errorMessage, locationInSourceCode)

        return IrCallImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = context.irBuiltIns.nothingType,
            symbol = context.irBuiltIns.linkageErrorSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 1,
            origin = IrStatementOrigin.PARTIAL_LINKAGE_RUNTIME_ERROR
        ).apply {
            putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, errorMessage))
        }
    }
}


