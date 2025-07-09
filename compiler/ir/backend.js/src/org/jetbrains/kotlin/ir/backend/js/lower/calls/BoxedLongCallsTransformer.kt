/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.irCall

/**
 * Depending on the target ES edition, replaces calls to `kotlin.js.jsLongToString` with either
 * `kotlin.js.internal.boxedLong.toStringImpl`, or `kotlin.js.internal.longAsBigInt.toStringImpl` calls.
 *
 * TODO(KT-70480): Delete this transformer when we drop the ES5 target
 */
internal class BoxedLongCallsTransformer(context: JsIrBackendContext): CallsTransformer {
    private val intrinsics = context.intrinsics

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        if (call.symbol == intrinsics.jsLongToString) {
            return irCall(call, intrinsics.longToStringImpl)
        }
        return call
    }
}
