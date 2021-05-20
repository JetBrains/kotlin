/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class WasmTryCatchLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitTry(aTry: IrTry): IrExpression {
                aTry.transformChildrenVoid()
                return lowerTry(aTry, container)
            }
        })
    }

    fun lowerTry(aTry: IrTry, container: IrDeclaration): IrExpression {
        val tryResult = aTry.tryResult
        val finallyExpression = aTry.finallyExpression

        // TODO: Support catching exceptions
        // TODO: Support proper try-finally control flow (e.g. cases with return, break in try {} block).
        if (finallyExpression == null)
            return tryResult
        else {
            val builder = context.createIrBuilder(container.symbol)

            if (aTry.type.isUnit()) {
                return builder.irComposite {
                    +tryResult
                    +finallyExpression
                }
            } else {
                return builder.irComposite {
                    val tmp = irTemporary(
                        tryResult,
                        irType = aTry.type,
                        nameHint = "result_of_try_expression"
                    )
                    +finallyExpression
                    +irGet(tmp)
                }
            }
        }
    }
}