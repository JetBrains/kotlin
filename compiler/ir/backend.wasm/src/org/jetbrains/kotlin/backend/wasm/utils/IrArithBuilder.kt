/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.ArithBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression

class WasmIrArithBuilder(val context: WasmBackendContext): ArithBuilder {
    val symbols = context.wasmSymbols

    fun and(l: IrExpression, r: IrExpression) = JsIrBuilder.buildCall(symbols.booleanAnd).apply {
        putValueArgument(0, l)
        putValueArgument(1, r)
    }

    fun or(l: IrExpression, r: IrExpression) = JsIrBuilder.buildCall(symbols.booleanOr).apply {
        putValueArgument(0, l)
        putValueArgument(1, r)
    }
    override fun not(v: IrExpression): IrExpression = JsIrBuilder.buildCall(symbols.booleanNot).apply {
        putValueArgument(0, v)
    }

    override fun andand(l: IrExpression, r: IrExpression) = // if (l) r else false
        JsIrBuilder.buildIfElse(context.irBuiltIns.booleanType, l, r, JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, false))
    override fun oror(l: IrExpression, r: IrExpression) = // if (l) true else r
        JsIrBuilder.buildIfElse(context.irBuiltIns.booleanType, l, JsIrBuilder.buildBoolean(context.irBuiltIns.booleanType, true), r)
}