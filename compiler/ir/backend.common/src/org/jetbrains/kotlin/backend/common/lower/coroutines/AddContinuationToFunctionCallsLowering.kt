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
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageCase.SuspendableFunctionCallWithoutCoroutineContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageUtils.File as PLFile

/**
 * Add continuation to suspend function calls.
 *
 * Additionally materialize continuation for `getContinuation` intrinsic calls.
 */
abstract class AbstractAddContinuationToFunctionCallsLowering : BodyLoweringPass {
    protected abstract val context: CommonBackendContext

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

        val plFile: PLFile by lazy { PLFile.determineFileFor(container) }

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBody(body: IrBody): IrBody {
                // Nested bodies are covered by separate `lower` invocation
                return body
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                if (!expression.isSuspend) {
                    if (expression.symbol == context.ir.symbols.getContinuation)
                        return getContinuation() ?: expression.throwLinkageError(plFile)
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
                    it.putValueArgument(it.valueArgumentsCount - 1, getContinuation() ?: return expression.throwLinkageError(plFile))
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
                return if (context.partialLinkageSupport.isEnabled)
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

    private fun IrCall.throwLinkageError(file: PLFile): IrCall =
        context.partialLinkageSupport.throwLinkageError(
            SuspendableFunctionCallWithoutCoroutineContext(this),
            element = this,
            file
        )
}
