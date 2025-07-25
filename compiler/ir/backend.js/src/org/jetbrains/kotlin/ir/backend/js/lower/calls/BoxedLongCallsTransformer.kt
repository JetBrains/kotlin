/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.primaryConstructorReplacement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Depending on the target ES edition, replaces some [Long]-related calls with calls to functions from either
 * the `kotlin.js.internal.boxedLong` or `kotlin.js.internal.longAsBigInt` package.
 *
 * TODO(KT-70480): Delete this transformer when we drop the ES5 target
 */
internal class BoxedLongCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val irBuiltIns = context.irBuiltIns
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
        insertNumberConversionInDateConstructorCall(call)?.let { return it }
        return call
    }

    private fun insertNumberConversionInDateConstructorCall(call: IrFunctionAccessExpression): IrExpression? {
        if (!longAsBigInt) return null
        if (call.symbol.owner.parentClassOrNull?.classId?.outermostClassId != JsStandardClassIds.Date) return null
        // This is to avoid breaking passing Long values to Date constructors.
        // When Long was a regular class, passing it to a Number parameter of the Date constructor worked fine,
        // because JavaScript would just call `valueOf` on the passed Long object, which returned a Double value
        //
        // However, when Long is compiled as JS BigInt, passing a Long value as is to a Date constructor would result in a runtime
        // error. To avoid breaking changes, we insert a conversion to Double.
        // See KT-79162
        val parametersWithNumberType = call.symbol.owner.parameters.filter { it.type == irBuiltIns.numberType }
        if (parametersWithNumberType.isEmpty()) return null
        for (numberParameter in parametersWithNumberType) {
            val arg = call.arguments[numberParameter] ?: continue
            val argPrimitiveType = arg.type.getPrimitiveType()
            if (argPrimitiveType in PrimitiveType.NUMBER_TYPES && argPrimitiveType != PrimitiveType.LONG) {
                // If the type of the argument can be expressed as JS `number`, don't insert the conversion
                continue
            }
            call.arguments[numberParameter] = JsIrBuilder.buildCall(intrinsics.jsNumberToDouble).apply {
                arguments[0] = arg
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
