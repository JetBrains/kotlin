/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin.*
import org.jetbrains.kotlin.ir.util.isDynamic
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal

class DynamicCallsTransformer(private val context: JsIrBackendContext) : CallsTransformer {

    private val originToIrFunction =
        context.intrinsics.run {
            mapOf(
                EXCL to jsNot,
                LT to jsLt,
                GT to jsGt,
                LTEQ to jsLtEq,
                GTEQ to jsGtEq,
                EQEQ to jsEqeq,
                EQEQEQ to jsEqeqeq,
                EXCLEQ to jsNotEq,
                EXCLEQEQ to jsNotEqeq,
                ANDAND to jsAnd,
                OROR to jsOr,
                UMINUS to jsUnaryMinus,
                UPLUS to jsUnaryPlus,
                PLUS to jsPlus,
                MINUS to jsMinus,
                MUL to jsMult,
                DIV to jsDiv,
                PERC to jsMod,
                PLUSEQ to jsPlusAssign,
                MINUSEQ to jsMinusAssign,
                MULTEQ to jsMultAssign,
                DIVEQ to jsDivAssign,
                PERCEQ to jsModAssign,
                PREFIX_INCR to jsPrefixInc,
                PREFIX_DECR to jsPrefixDec,
                POSTFIX_INCR to jsPostfixInc,
                POSTFIX_DECR to jsPostfixDec,
                GET_ARRAY_ELEMENT to jsArrayGet,
                // TODO add a special statement origin, e.g. SET_ARRAY_ELEMENT
                EQ to jsArraySet
            )
        }

    override fun transformCall(call: IrCall): IrExpression {
        val symbol = call.symbol
        val function = call.symbol.owner

        if (function.isDynamic()) {
            when (call.origin) {
                GET_PROPERTY -> {
                    val fieldSymbol = context.symbolTable.lazyWrapper.referenceField(
                        (symbol.descriptor as PropertyAccessorDescriptor).correspondingProperty
                    )
                    return JsIrBuilder.buildGetField(fieldSymbol, call.dispatchReceiver, type = call.type)
                }

                // assignment to a property
                EQ -> {
                    if (symbol.descriptor is PropertyAccessorDescriptor) {
                        val fieldSymbol = context.symbolTable.lazyWrapper.referenceField(
                            (symbol.descriptor as PropertyAccessorDescriptor).correspondingProperty
                        )
                        return call.run {
                            JsIrBuilder.buildSetField(fieldSymbol, dispatchReceiver, getValueArgument(0)!!, type)
                        }
                    }
                }
            }
        }

        if (function.isDynamic()) {
            originToIrFunction[call.origin]?.let {
                return irCall(call, it.symbol, dispatchReceiverAsFirstArgument = true)
            }
        }
        return call
    }
}
