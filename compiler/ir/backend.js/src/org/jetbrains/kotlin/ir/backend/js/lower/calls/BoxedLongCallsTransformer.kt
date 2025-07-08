/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.primaryConstructorReplacement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Depending on the target ES edition, replaces some [Long]-related calls with calls to functions from either
 * the `kotlin.js.internal.boxedLong` or `kotlin.js.internal.longAsBigInt` package.
 *
 * TODO(KT-70480): Delete this transformer when we drop the ES5 target
 */
internal class BoxedLongCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val longAsBigInt = context.configuration.compileLongAsBigint
    private val longLowField = intrinsics.longClassSymbol.fields.single { it.owner.name.asString() == "low" }
    private val longHighField = intrinsics.longClassSymbol.fields.single { it.owner.name.asString() == "high" }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        if (call.symbol == intrinsics.jsLongToString) {
            return irCall(call, intrinsics.longToStringImpl)
        }
        if (longAsBigInt && call.symbol == intrinsics.longClassSymbol.owner.primaryConstructor?.symbol) {
            return irCall(call, intrinsics.longFromTwoInts!!)
        }
        if (longAsBigInt && call.symbol == intrinsics.longClassSymbol.owner.primaryConstructorReplacement?.symbol) {
            return irCall(call, intrinsics.longFromTwoInts!!).apply {
                // The first parameter of the primary constructor replacement function is actually `this`.
                arguments.assignFrom(call.arguments.drop(1))
            }
        }
        return call
    }

    override fun transformFieldAccess(access: IrFieldAccessExpression): IrExpression {
        if (intrinsics.longLowBits != null && access.symbol == longLowField) {
            return IrCallImpl(access.startOffset, access.endOffset, longLowField.owner.type, intrinsics.longLowBits).apply {
                arguments[0] = access.receiver
            }
        }
        if (intrinsics.longHighBits != null && access.symbol == longHighField) {
            return IrCallImpl(access.startOffset, access.endOffset, longHighField.owner.type, intrinsics.longHighBits).apply {
                arguments[0] = access.receiver
            }
        }
        return access
    }
}
