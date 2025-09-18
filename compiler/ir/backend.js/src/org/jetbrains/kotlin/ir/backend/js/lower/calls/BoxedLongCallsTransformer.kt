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
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Depending on the target ES edition, replaces some [Long]-related calls with calls to functions from either
 * the `kotlin.js.internal.boxedLong` or `kotlin.js.internal.longAsBigInt` package.
 *
 * TODO(KT-70480): Delete this transformer when we drop the ES5 target
 */
internal class BoxedLongCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.symbols
    private val longAsBigInt = context.configuration.compileLongAsBigint
    private val longLowGetter = symbols.longClassSymbol.getPropertyGetter("low")
    private val longHighGetter = symbols.longClassSymbol.getPropertyGetter("high")
    private val longLowField = symbols.longClassSymbol.fields.single { it.owner.name.asString() == "low" }
    private val longHighField = symbols.longClassSymbol.fields.single { it.owner.name.asString() == "high" }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        if (call.symbol == symbols.jsLongToString) {
            return irCall(call, symbols.longToStringImpl)
        }
        if (longAsBigInt && call.symbol == symbols.longClassSymbol.owner.primaryConstructor?.symbol) {
            return irCall(call, symbols.longFromTwoInts!!)
        }
        if (longAsBigInt && call.symbol == symbols.longClassSymbol.owner.primaryConstructorReplacement?.symbol) {
            return irCall(call, symbols.longFromTwoInts!!).apply {
                // The first parameter of the primary constructor replacement function is actually `this`.
                arguments.assignFrom(call.arguments.drop(1))
            }
        }
        if (longAsBigInt && call.symbol == longLowGetter) {
            return irCall(call, symbols.longLowBits!!)
        }
        if (longAsBigInt && call.symbol == longHighGetter) {
            return irCall(call, symbols.longHighBits!!)
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
            call.arguments[numberParameter] = JsIrBuilder.buildCall(symbols.jsNumberToDouble).apply {
                arguments[0] = arg
            }
        }
        return call
    }

    override fun transformFieldAccess(access: IrFieldAccessExpression): IrExpression {
        if (symbols.longLowBits != null && access.symbol == longLowField) {
            return IrCallImpl(access.startOffset, access.endOffset, longLowField.owner.type, symbols.longLowBits).apply {
                arguments[0] = access.receiver
            }
        }
        if (symbols.longHighBits != null && access.symbol == longHighField) {
            return IrCallImpl(access.startOffset, access.endOffset, longHighField.owner.type, symbols.longHighBits).apply {
                arguments[0] = access.receiver
            }
        }
        return access
    }
}
