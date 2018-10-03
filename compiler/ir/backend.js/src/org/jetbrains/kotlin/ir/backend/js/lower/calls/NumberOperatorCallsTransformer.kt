/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.irCall
import org.jetbrains.kotlin.ir.backend.js.utils.ConversionNames
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name

class NumberOperatorCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    private val primitiveNumbers =
        irBuiltIns.run { listOf(intType, shortType, byteType, floatType, doubleType) }

    private val memberToTransformer = MemberToTransformer().apply {
        for (type in primitiveNumbers) {
            add(type, OperatorNames.UNARY_PLUS, intrinsics.jsUnaryPlus)
            add(type, OperatorNames.UNARY_MINUS, intrinsics.jsUnaryMinus)
        }

        add(irBuiltIns.stringType, OperatorNames.ADD, intrinsics.jsPlus)

        irBuiltIns.intType.let {
            add(it, OperatorNames.SHL, intrinsics.jsBitShiftL)
            add(it, OperatorNames.SHR, intrinsics.jsBitShiftR)
            add(it, OperatorNames.SHRU, intrinsics.jsBitShiftRU)
            add(it, OperatorNames.AND, intrinsics.jsBitAnd)
            add(it, OperatorNames.OR, intrinsics.jsBitOr)
            add(it, OperatorNames.XOR, intrinsics.jsBitXor)
            add(it, OperatorNames.INV, intrinsics.jsBitNot)
        }

        irBuiltIns.booleanType.let {
            add(it, OperatorNames.AND, intrinsics.jsBitAnd)
            add(it, OperatorNames.OR, intrinsics.jsBitOr)
            add(it, OperatorNames.NOT, intrinsics.jsNot)
            add(it, OperatorNames.XOR, intrinsics.jsBitXor)
        }

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

        for (type in primitiveNumbers) {
            add(type, Name.identifier("rangeTo"), ::transformRangeTo)
        }

        for (type in primitiveNumbers) {
            // TODO: use increment and decrement when it's possible
            add(type, OperatorNames.INC) {
                irCall(it, intrinsics.jsPlus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                    putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, 1))
                }
            }
            add(type, OperatorNames.DEC) {
                irCall(it, intrinsics.jsMinus.symbol, dispatchReceiverAsFirstArgument = true).apply {
                    putValueArgument(1, JsIrBuilder.buildInt(irBuiltIns.intType, 1))
                }
            }
        }

        for (type in primitiveNumbers) {
            add(type, OperatorNames.ADD, withLongCoercion(intrinsics.jsPlus))
            add(type, OperatorNames.SUB, withLongCoercion(intrinsics.jsMinus))
            add(type, OperatorNames.MUL, withLongCoercion(intrinsics.jsMult))
            add(type, OperatorNames.DIV, withLongCoercion(intrinsics.jsDiv))
            add(type, OperatorNames.MOD, withLongCoercion(intrinsics.jsMod))
            add(type, OperatorNames.REM, withLongCoercion(intrinsics.jsMod))
        }

        for (type in arrayOf(irBuiltIns.byteType, irBuiltIns.intType)) {
            add(type, ConversionNames.TO_CHAR) {
                irCall(it, intrinsics.charClassSymbol.constructors.single(), dispatchReceiverAsFirstArgument = true)
            }
        }

        for (type in arrayOf(irBuiltIns.floatType, irBuiltIns.doubleType)) {
            add(type, ConversionNames.TO_CHAR) {
                JsIrBuilder.buildCall(intrinsics.charClassSymbol.constructors.single()).apply {
                    putValueArgument(0, irCall(it, intrinsics.jsNumberToInt, dispatchReceiverAsFirstArgument = true))
                }
            }
        }

        add(irBuiltIns.charType, ConversionNames.TO_CHAR) { it.dispatchReceiver!! }
    }

    override fun transformCall(call: IrCall): IrExpression {
        val function = call.symbol.owner
        function.dispatchReceiverParameter?.also {
            val key = SimpleMemberKey(it.type, function.name)
            memberToTransformer[key]?.also {
                return it(call)
            }
        }
        return call
    }

    private fun useDispatchReceiver(call: IrCall): IrExpression {
        return call.dispatchReceiver!!
    }

    private fun transformRangeTo(call: IrCall): IrExpression {
        if (call.valueArgumentsCount != 1) return call
        return with(call.symbol.owner.valueParameters[0].type) {
            when {
                isByte() || isShort() || isInt() ->
                    irCall(call, intrinsics.jsNumberRangeToNumber, dispatchReceiverAsFirstArgument = true)
                isLong() ->
                    irCall(call, intrinsics.jsNumberRangeToLong, dispatchReceiverAsFirstArgument = true)
                else -> call
            }
        }
    }

    private fun withLongCoercion(intrinsic: IrSimpleFunction): (IrCall) -> IrExpression = { call ->
        assert(call.valueArgumentsCount == 1)
        val arg = call.getValueArgument(0)!!

        if (arg.type.isLong()) {
            val receiverType = call.dispatchReceiver!!.type

            when {
                // Double OP Long => Double OP Long.toDouble()
                receiverType.isDouble() -> {
                    call.putValueArgument(0, IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        intrinsics.longToDouble.owner.returnType,
                        intrinsics.longToDouble
                    ).apply {
                        dispatchReceiver = arg
                    })
                }
                // Float OP Long => Float OP Long.toFloat()
                receiverType.isFloat() -> {
                    call.putValueArgument(0, IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        intrinsics.longToFloat.owner.returnType,
                        intrinsics.longToFloat
                    ).apply {
                        dispatchReceiver = arg
                    })
                }
                // {Byte, Short, Int} OP Long => {Byte, Sort, Int}.toLong() OP Long
                !receiverType.isLong() -> {
                    call.dispatchReceiver = IrCallImpl(
                        call.startOffset,
                        call.endOffset,
                        intrinsics.jsNumberToLong.owner.returnType,
                        intrinsics.jsNumberToLong
                    ).apply {
                        putValueArgument(0, call.dispatchReceiver)
                    }
                }
            }
        }

        if (call.dispatchReceiver!!.type.isLong()) {
            // LHS is Long => use as is
            call
        } else {
            irCall(call, intrinsic.symbol, dispatchReceiverAsFirstArgument = true)
        }
    }
}

