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
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Add continuation to suspend function calls.
 *
 * Additionally materialize continuation for `getContinuation` intrinsic calls.
 */
abstract class AbstractAddContinuationToFunctionCallsLowering : BodyLoweringPass {
    abstract val context: CommonBackendContext
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    abstract fun IrSimpleFunction.getContinuationParameter() : IrValueParameter


    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val continuation: IrValueParameter by lazy {
            (container as IrSimpleFunction).getContinuationParameter()
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


