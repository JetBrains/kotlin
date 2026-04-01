/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.util.OperatorNameConventions

class NumberConversionCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val symbols = context.symbols
    private val irBuiltIns = context.irBuiltIns

    private val memberToTransformer = MemberToTransformer().apply {
        // Conversion rules are ported from NumberAndCharConversionFIF
        // TODO: Add Char and Number conversions

        irBuiltIns.byteType.let {
            add(it, OperatorNameConventions.TO_BYTE, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_DOUBLE, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_FLOAT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_INT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_SHORT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_LONG, symbols.longFromInt)
        }

        for (type in listOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
            add(type, OperatorNameConventions.TO_BYTE, symbols.jsNumberToByte)
            add(type, OperatorNameConventions.TO_DOUBLE, ::useDispatchReceiver)
            add(type, OperatorNameConventions.TO_FLOAT, ::useDispatchReceiver)
            add(type, OperatorNameConventions.TO_INT, symbols.jsNumberToInt)
            add(type, OperatorNameConventions.TO_SHORT, symbols.jsNumberToShort)
            add(type, OperatorNameConventions.TO_LONG, symbols.jsNumberToLong)
        }

        irBuiltIns.intType.let {
            add(it, OperatorNameConventions.TO_BYTE, symbols.jsToByte)
            add(it, OperatorNameConventions.TO_DOUBLE, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_FLOAT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_INT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_SHORT, symbols.jsToShort)
            add(it, OperatorNameConventions.TO_LONG, symbols.longFromInt)
        }

        irBuiltIns.shortType.let {
            add(it, OperatorNameConventions.TO_BYTE, symbols.jsToByte)
            add(it, OperatorNameConventions.TO_DOUBLE, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_FLOAT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_INT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_SHORT, ::useDispatchReceiver)
            add(it, OperatorNameConventions.TO_LONG, symbols.longFromInt)
        }


        irBuiltIns.numberType.let {
            add(it, OperatorNameConventions.TO_BYTE, symbols.jsNumberToByte)
            add(it, OperatorNameConventions.TO_DOUBLE, symbols.jsNumberToDouble)
            add(it, OperatorNameConventions.TO_FLOAT, symbols.jsNumberToDouble)
            add(it, OperatorNameConventions.TO_INT, symbols.jsNumberToInt)
            add(it, OperatorNameConventions.TO_SHORT, symbols.jsNumberToShort)
            add(it, OperatorNameConventions.TO_LONG, symbols.jsNumberToLong)
        }

        irBuiltIns.longType.let {
            add(it, OperatorNameConventions.TO_BYTE, symbols.longToByte)
            add(it, OperatorNameConventions.TO_CHAR, symbols.longToChar)
            add(it, OperatorNameConventions.TO_DOUBLE, symbols.longToNumber)
            add(it, OperatorNameConventions.TO_FLOAT, symbols.longToNumber)
            add(it, OperatorNameConventions.TO_INT, symbols.longToInt)
            add(it, OperatorNameConventions.TO_SHORT, symbols.longToShort)
            add(it, OperatorNameConventions.TO_LONG, ::useDispatchReceiver)
        }

        for (type in arrayOf(irBuiltIns.byteType, irBuiltIns.shortType, irBuiltIns.intType)) {
            add(type, OperatorNameConventions.TO_CHAR, symbols.jsNumberToChar)
        }

        for (type in arrayOf(irBuiltIns.floatType, irBuiltIns.doubleType, irBuiltIns.numberType)) {
            add(type, OperatorNameConventions.TO_CHAR, symbols.jsNumberToChar)
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
