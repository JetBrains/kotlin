/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.Name

class NumberOperatorCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val intrinsics = context.intrinsics
    private val irBuiltIns = context.irBuiltIns

    private fun buildInt(v: Int) = JsIrBuilder.buildInt(irBuiltIns.intType, v)

    private val memberToTransformer = MemberToTransformer().apply {

        val primitiveNumbers =
            irBuiltIns.run { listOf(intType, shortType, byteType, floatType, doubleType) }

        for (type in primitiveNumbers) {
            add(type, OperatorNames.UNARY_PLUS, intrinsics.jsUnaryPlus)
            add(type, OperatorNames.UNARY_MINUS, ::transformUnaryMinus)
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

        for (type in primitiveNumbers) {
            add(type, Name.identifier("rangeTo"), ::transformRangeTo)
        }

        for (type in primitiveNumbers) {
            add(type, OperatorNames.INC, ::transformIncrement)
            add(type, OperatorNames.DEC, ::transformDecrement)
        }

        for (type in primitiveNumbers) {
            add(type, OperatorNames.ADD, withLongCoercion(::transformAdd))
            add(type, OperatorNames.SUB, withLongCoercion(::transformSub))
            add(type, OperatorNames.MUL, withLongCoercion(::transformMul))
            add(type, OperatorNames.DIV, withLongCoercion(::transformDiv))
            add(type, OperatorNames.MOD, withLongCoercion(::transformRem))
            add(type, OperatorNames.REM, withLongCoercion(::transformRem))
        }
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

    private fun irBinaryOp(
        call: IrCall,
        intrinsic: IrFunction,
        toInt32: Boolean = false
    ): IrExpression {
        val newCall = irCall(call, intrinsic, dispatchReceiverAsFirstArgument = true)
        if (toInt32)
            return toInt32(newCall)
        return newCall
    }

    class BinaryOp(call: IrCall) {
        val function = call.symbol.owner
        val name = function.name
        val lhs = function.dispatchReceiverParameter!!.type
        val rhs = function.valueParameters[0].type
        val result = function.returnType

        fun canAddOrSubOverflow() =
            result.isInt() && (lhs.isInt() || rhs.isInt())
    }

    private fun transformAdd(call: IrCall) =
        irBinaryOp(call, intrinsics.jsPlus, toInt32 = BinaryOp(call).canAddOrSubOverflow())

    private fun transformSub(call: IrCall) =
        irBinaryOp(call, intrinsics.jsMinus, toInt32 = BinaryOp(call).canAddOrSubOverflow())

    private fun transformMul(call: IrCall) = BinaryOp(call).run {
        when {
            result.isInt() -> when {

                lhs.isInt() && rhs.isInt() ->
                    irBinaryOp(call, intrinsics.jsImul.owner)

                else ->
                    irBinaryOp(call, intrinsics.jsMult, toInt32 = true)
            }

            else -> irBinaryOp(call, intrinsics.jsMult, toInt32 = false)
        }
    }

    private fun transformDiv(call: IrCall) =
        irBinaryOp(call, intrinsics.jsDiv, toInt32 = BinaryOp(call).result.isInt())

    private fun transformRem(call: IrCall) =
        irBinaryOp(call, intrinsics.jsMod)

    private fun transformIncrement(call: IrCall) =
        transformCrement(call, intrinsics.jsPlus)

    private fun transformDecrement(call: IrCall) =
        transformCrement(call, intrinsics.jsMinus)

    private fun transformCrement(call: IrCall, correspondingBinaryOp: IrFunction): IrExpression {
        val operation = irCall(call, correspondingBinaryOp.symbol, dispatchReceiverAsFirstArgument = true).apply {
            putValueArgument(1, buildInt(1))
        }

        return convertResultToPrimitiveType(operation, call.type)
    }

    private fun transformUnaryMinus(call: IrCall) =
        convertResultToPrimitiveType(
            irCall(call, intrinsics.jsUnaryMinus, dispatchReceiverAsFirstArgument = true),
            call.type
        )

    private fun convertResultToPrimitiveType(e: IrExpression, type: IrType) = when {
        type.isInt() -> toInt32(e)
        type.isByte() -> intrinsics.jsNumberToByte.call(e)
        type.isShort() -> intrinsics.jsNumberToShort.call(e)
        else -> e
    }

    private fun withLongCoercion(default: (IrCall) -> IrExpression): (IrCall) -> IrExpression = { call ->
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
            default(call)
        }
    }

    fun IrFunctionSymbol.call(vararg arguments: IrExpression) =
        JsIrBuilder.buildCall(this, owner.returnType).apply {
            for ((idx, arg) in arguments.withIndex()) {
                putValueArgument(idx, arg)
            }
        }

    private fun toInt32(e: IrExpression) =
        JsIrBuilder.buildCall(intrinsics.jsBitOr.symbol, irBuiltIns.intType).apply {
            putValueArgument(0, e)
            putValueArgument(1, buildInt(0))
        }
}
