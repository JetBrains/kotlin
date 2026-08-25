/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.AutoboxingTransformer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

class WasmAutoboxingTransformer(val ctx: WasmBackendContext) : AutoboxingTransformer(ctx) {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol == ctx.wasmSymbols.consumeAnyIntoVoid ||
            expression.symbol == ctx.wasmSymbols.createBoxIntrinsic
        ) {
            expression.apply { transformChildrenVoid() }
            return expression
        }

        val handledCall = super.visitCall(expression)
        if (handledCall !is IrCall) return handledCall
        return expression.useAs(expression.type)
    }
}
