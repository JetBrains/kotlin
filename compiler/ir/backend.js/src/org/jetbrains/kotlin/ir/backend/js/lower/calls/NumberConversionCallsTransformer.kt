/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.ConversionNames
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

class NumberConversionCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    private val memberToTransformer = MemberToTransformer().apply {
        // Conversion rules are ported from NumberAndCharConversionFIF
        // TODO: Add Char and Number conversions

        irBuiltIns.byteType.let {
            add(it, ConversionNames.TO_BYTE, ::useDispatchReceiver)
            add(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
            add(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_INT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_SHORT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
        }

        for (type in listOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
            add(type, ConversionNames.TO_BYTE, intrinsics.jsNumberToByte)
            add(type, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
            add(type, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
            add(type, ConversionNames.TO_INT, intrinsics.jsNumberToInt)
            add(type, ConversionNames.TO_SHORT, intrinsics.jsNumberToShort)
            add(type, ConversionNames.TO_LONG, intrinsics.jsNumberToLong)
        }

        irBuiltIns.intType.let {
            add(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
            add(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
            add(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_INT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_SHORT, intrinsics.jsToShort)
            add(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
        }

        irBuiltIns.shortType.let {
            add(it, ConversionNames.TO_BYTE, intrinsics.jsToByte)
            add(it, ConversionNames.TO_DOUBLE, ::useDispatchReceiver)
            add(it, ConversionNames.TO_FLOAT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_INT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_SHORT, ::useDispatchReceiver)
            add(it, ConversionNames.TO_LONG, intrinsics.jsToLong)
        }


        irBuiltIns.numberType.let {
            add(it, ConversionNames.TO_BYTE, intrinsics.jsNumberToByte)
            add(it, ConversionNames.TO_DOUBLE, intrinsics.jsNumberToDouble)
            add(it, ConversionNames.TO_FLOAT, intrinsics.jsNumberToDouble)
            add(it, ConversionNames.TO_INT, intrinsics.jsNumberToInt)
            add(it, ConversionNames.TO_SHORT, intrinsics.jsNumberToShort)
            add(it, ConversionNames.TO_LONG, intrinsics.jsNumberToLong)
        }

        for (type in arrayOf(irBuiltIns.byteType, irBuiltIns.shortType, irBuiltIns.intType)) {
            add(type, ConversionNames.TO_CHAR, intrinsics.jsNumberToChar)
        }

        for (type in arrayOf(irBuiltIns.floatType, irBuiltIns.doubleType, irBuiltIns.numberType)) {
            add(type, ConversionNames.TO_CHAR, intrinsics.jsNumberToChar)
        }
    }

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val function = call.symbol.owner
        function.dispatchReceiverParameter?.also {
            val key = SimpleMemberKey(it.type, function.name)
            memberToTransformer[key]?.also {
                return it(call)
            }
        }
        return call
    }

    private fun useDispatchReceiver(call: IrFunctionAccessExpression): IrExpression {
        return JsIrBuilder.buildReinterpretCast(call.dispatchReceiver!!, call.type)
    }
}
