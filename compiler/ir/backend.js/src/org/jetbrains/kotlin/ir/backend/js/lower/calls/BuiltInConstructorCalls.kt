/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.irConstructorCall


class BuiltInConstructorCalls(val context: JsIrBackendContext) : CallsTransformer {
    val intrinsics = context.intrinsics

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression =
        if (call is IrConstructorCall) {
            // Do not transform Delegation calls
            when (call.symbol) {
                intrinsics.stringConstructorSymbol -> JsIrBuilder.buildString(context.irBuiltIns.stringType, "")
                intrinsics.anyConstructorSymbol -> irConstructorCall(call, intrinsics.jsObjectConstructorSymbol)
                else -> call
            }
        } else call
}
