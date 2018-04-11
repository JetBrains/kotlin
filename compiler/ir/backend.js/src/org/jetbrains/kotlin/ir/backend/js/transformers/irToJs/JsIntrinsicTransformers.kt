/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIntrinsics
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.backend.ast.*

typealias IrCallTransformer = (IrCall, List<JsExpression>) -> JsExpression

class JsIntrinsicTransformers(intrinsics: JsIntrinsics) {
    private val transformers: Map<IrSymbol, IrCallTransformer>

    init {
        transformers = mutableMapOf()

        transformers.apply {
            binOp(intrinsics.jsEqeqeq, JsBinaryOperator.REF_EQ)
            binOp(intrinsics.jsNotEqeq, JsBinaryOperator.REF_NEQ)
            binOp(intrinsics.jsEqeq, JsBinaryOperator.EQ)
            binOp(intrinsics.jsNotEq, JsBinaryOperator.NEQ)

            prefixOp(intrinsics.jsNot, JsUnaryOperator.NOT)

            binOp(intrinsics.jsPlus, JsBinaryOperator.ADD)
            binOp(intrinsics.jsMinus, JsBinaryOperator.SUB)
            binOp(intrinsics.jsMult, JsBinaryOperator.MUL)
            binOp(intrinsics.jsDiv, JsBinaryOperator.DIV)
            binOp(intrinsics.jsMod, JsBinaryOperator.MOD)
        }
    }

    operator fun get(symbol: IrSymbol): IrCallTransformer? = transformers[symbol]
}

private fun MutableMap<IrSymbol, IrCallTransformer>.binOp(function: IrFunction, op: JsBinaryOperator) {
    put(function.symbol, { _, args -> JsBinaryOperation(op, args[0], args[1]) })
}

private fun MutableMap<IrSymbol, IrCallTransformer>.prefixOp(function: IrFunction, op: JsUnaryOperator) {
    put(function.symbol, { _, args -> JsPrefixOperation(op, args[0]) })
}
