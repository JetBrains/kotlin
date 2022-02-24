/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Add continuation to suspend function calls. Requires [AddContinuationToLocalSuspendFunctionsLowering] and
 * [AddContinuationToNonLocalSuspendFunctionsLowering] to transform function declarations first.
 *
 * Additionally materialize continuation for `getContinuation` intrinsic calls.
 */
class AddContinuationToFunctionCallsLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val continuation: IrValueParameter by lazy {
            val function = container as IrSimpleFunction
            if (function.overriddenSymbols
                    .any { it.owner.name.asString() == "doResume" && it.owner.parent == context.coroutineSymbols.coroutineImpl.owner }
            ) {
                function.dispatchReceiverParameter!!
            } else {
                function.valueParameters.last()
            }
        }

        val builder by lazy { context.createIrBuilder(container.symbol) }
        fun getContinuation() = builder.irGet(continuation)

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBody(body: IrBody): IrBody {
                // Nested bodies are covered by separate `lower` invocation
                return body
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                if (!expression.isSuspend) {
                    if (expression.symbol == context.ir.symbols.getContinuation)
                        return getContinuation()
                    return expression
                }

                val oldFun = expression.symbol.owner
                // TODO: investigate why mapping might be unavailable for SuspendFunction4.invoke
                val newFun: IrSimpleFunction = oldFun.getOrCreateFunctionWithContinuationStub(context)

                return irCall(
                    expression,
                    newFun.symbol,
                    newReturnType = newFun.returnType,
                    newSuperQualifierSymbol = expression.superQualifierSymbol
                ).also {
                    it.putValueArgument(it.valueArgumentsCount - 1, getContinuation())
                }
            }
        })
    }
}

