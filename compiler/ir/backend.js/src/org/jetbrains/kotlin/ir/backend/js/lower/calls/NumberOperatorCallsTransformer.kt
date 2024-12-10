/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

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
            // shifting of a negative int to 0 bytes returns the unsigned int, therefore we have to cast it back to the signed int
            add(it, OperatorNames.SHRU) { call -> irBinaryOp(call, intrinsics.jsBitShiftRU, toInt32 = true) }
            add(it, OperatorNames.AND, intrinsics.jsBitAnd)
            add(it, OperatorNames.OR, intrinsics.jsBitOr)
            add(it, OperatorNames.XOR, intrinsics.jsBitXor)
            add(it, OperatorNames.INV, intrinsics.jsBitNot)
        }

        irBuiltIns.booleanType.let {
            // These operators are not short-circuit -- using bitwise operators '&', '|', '^' followed by coercion to boolean
            add(it, OperatorNames.AND) { call -> toBoolean(irCall(call, intrinsics.jsBitAnd, receiversAsArguments = true)) }
            add(it, OperatorNames.OR) { call -> toBoolean(irCall(call, intrinsics.jsBitOr, receiversAsArguments = true)) }
            add(it, OperatorNames.XOR) { call -> toBoolean(irCall(call, intrinsics.jsBitXor, receiversAsArguments = true)) }

            add(it, OperatorNames.NOT, intrinsics.jsNot)

            add(it, OperatorNameConventions.HASH_CODE, intrinsics.jsGetBooleanHashCode)
        }

        for (type in primitiveNumbers) {
            add(type, OperatorNameConventions.RANGE_TO, ::transformRangeTo)
            add(type, OperatorNameConventions.RANGE_UNTIL, ::transformRangeUntil)
            add(type, OperatorNameConventions.HASH_CODE, ::transformHashCode)
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
            add(type, OperatorNames.REM, withLongCoercion(::transformRem))
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

    private fun transformRangeTo(call: IrFunctionAccessExpression): IrExpression {
        if (call.arguments.size != 2) return call
        return with(call.symbol.owner.parameters[1].type) {
            when {
                isByte() || isShort() || isInt() ->
                    irCall(call, intrinsics.jsNumberRangeToNumber, receiversAsArguments = true)
                isLong() ->
                    irCall(call, intrinsics.jsNumberRangeToLong, receiversAsArguments = true)
                else -> call
            }
        }
    }

    private fun transformRangeUntil(call: IrFunctionAccessExpression): IrExpression {
        if (call.arguments.size != 2) return call
        with(call.symbol.owner) {
            val function = intrinsics.rangeUntilFunctions[parameters[0].type to parameters[1].type]
                ?: irError("No 'until' function found for descriptor") {
                    withIrEntry("call.symbol.owner", call.symbol.owner)
                }
            return IrCallImpl(
                call.startOffset,
                call.endOffset,
                call.type,
                function,
                call.typeArguments.size,
                call.origin,
            ).apply {
                copyTypeArgumentsFrom(call)
                arguments.assignFrom(call.arguments)
            }
        }
    }

    private fun transformHashCode(call: IrFunctionAccessExpression): IrExpression {
        return with(call.symbol.owner.dispatchReceiverParameter!!.type) {
            when {
                isByte() || isShort() || isInt() ->
                    call.dispatchReceiver!!
                isFloat() || isDouble() ->
                    // TODO introduce doubleToHashCode?
                    irCall(call, intrinsics.jsGetNumberHashCode, receiversAsArguments = true)
                else -> call
            }
        }
    }

    private fun irBinaryOp(
        call: IrFunctionAccessExpression,
        intrinsic: IrSimpleFunctionSymbol,
        toInt32: Boolean = false
    ): IrExpression {
        val newCall = irCall(call, intrinsic, receiversAsArguments = true)
        if (toInt32)
            return toInt32(newCall)
        return newCall
    }

    class BinaryOp(call: IrFunctionAccessExpression) {
        val function = call.symbol.owner
        val name = function.name
        val lhs = function.parameters[0].type
        val rhs = function.parameters[1].type
        val result = function.returnType

        fun canAddOrSubOverflow() =
            result.isInt() && (lhs.isInt() || rhs.isInt())
    }

    private fun transformAdd(call: IrFunctionAccessExpression) =
        irBinaryOp(call, intrinsics.jsPlus, toInt32 = BinaryOp(call).canAddOrSubOverflow())

    private fun transformSub(call: IrFunctionAccessExpression) =
        irBinaryOp(call, intrinsics.jsMinus, toInt32 = BinaryOp(call).canAddOrSubOverflow())

    private fun transformMul(call: IrFunctionAccessExpression) = BinaryOp(call).run {
        when {
            result.isInt() -> when {

                lhs.isInt() && rhs.isInt() ->
                    irBinaryOp(call, intrinsics.jsImul)

                else ->
                    irBinaryOp(call, intrinsics.jsMult, toInt32 = true)
            }

            else -> irBinaryOp(call, intrinsics.jsMult, toInt32 = false)
        }
    }

    private fun transformDiv(call: IrFunctionAccessExpression) =
        irBinaryOp(call, intrinsics.jsDiv, toInt32 = BinaryOp(call).result.isInt())

    private fun transformRem(call: IrFunctionAccessExpression) =
        irBinaryOp(call, intrinsics.jsMod, toInt32 = BinaryOp(call).result.isInt())

    private fun transformIncrement(call: IrFunctionAccessExpression) =
        transformCrement(call, intrinsics.jsPlus)

    private fun transformDecrement(call: IrFunctionAccessExpression) =
        transformCrement(call, intrinsics.jsMinus)

    private fun transformCrement(call: IrFunctionAccessExpression, correspondingBinaryOp: IrSimpleFunctionSymbol): IrExpression {
        val operation = irCall(call, correspondingBinaryOp, receiversAsArguments = true).apply {
            arguments[1] = buildInt(1)
        }

        return convertResultToPrimitiveType(operation, call.type)
    }

    private fun transformUnaryMinus(call: IrFunctionAccessExpression) =
        convertResultToPrimitiveType(
            irCall(call, intrinsics.jsUnaryMinus, receiversAsArguments = true),
            call.type
        )

    private fun convertResultToPrimitiveType(e: IrExpression, type: IrType) = when {
        type.isInt() -> toInt32(e)
        type.isByte() -> intrinsics.jsNumberToByte.call(e)
        type.isShort() -> intrinsics.jsNumberToShort.call(e)
        else -> e
    }

    private fun withLongCoercion(default: (IrFunctionAccessExpression) -> IrExpression): (IrFunctionAccessExpression) -> IrExpression =
        { call ->
            check(call.arguments.size == 2)
            val arg = call.arguments[1]!!

            var actualCall = call

            if (arg.type.isLong()) {
                val receiverType = call.arguments[0]!!.type

                when {
                    // Double OP Long => Double OP Long.toDouble()
                    receiverType.isDouble() -> {
                        call.arguments[1] = IrCallImpl(
                            call.startOffset,
                            call.endOffset,
                            intrinsics.longToDouble.owner.returnType,
                            intrinsics.longToDouble,
                            typeArgumentsCount = 0
                        ).apply {
                            arguments[0] = arg
                        }
                    }
                    // Float OP Long => Float OP Long.toFloat()
                    receiverType.isFloat() -> {
                        call.arguments[1] = IrCallImpl(
                            call.startOffset,
                            call.endOffset,
                            intrinsics.longToFloat.owner.returnType,
                            intrinsics.longToFloat,
                            typeArgumentsCount = 0
                        ).apply {
                            arguments[0] = arg
                        }
                    }
                    // {Byte, Short, Int} OP Long => {Byte, Sort, Int}.toLong() OP Long
                    !receiverType.isLong() -> {
                        call.arguments[0] = IrCallImpl(
                            call.startOffset,
                            call.endOffset,
                            intrinsics.jsNumberToLong.owner.returnType,
                            intrinsics.jsNumberToLong,
                            typeArgumentsCount = 0
                        ).apply {
                            arguments[0] = call.arguments[0]
                        }

                        // Replace {Byte, Short, Int}.OP with corresponding Long.OP
                        val declaration = call.symbol.owner as IrSimpleFunction
                        val nonDispatchParameters = declaration.nonDispatchParameters
                        val replacement = intrinsics
                            .longClassSymbol
                            .owner
                            .declarations
                            .filterIsInstance<IrSimpleFunction>()
                            .single { member ->
                                member.name == declaration.name &&
                                        member.hasShape(
                                            dispatchReceiver = true,
                                            regularParameters = nonDispatchParameters.size,
                                            parameterTypes = nonDispatchParameters.mapTo(mutableListOf(null)) { it.type }
                                        )
                            }.symbol

                        actualCall = irCall(call, replacement)
                    }
                }
            }

            if (actualCall.arguments[0]!!.type.isLong()) {
                actualCall
            } else {
                default(actualCall)
            }
        }

    private fun IrSimpleFunctionSymbol.call(vararg arguments: IrExpression) =
        JsIrBuilder.buildCall(this, owner.returnType).apply {
            this.arguments.assignFrom(arguments.toList())
        }

    private fun booleanNegate(e: IrExpression) =
        JsIrBuilder.buildCall(intrinsics.jsNot, irBuiltIns.booleanType).apply {
            arguments[0] = e
        }

    private fun toBoolean(e: IrExpression) =
        booleanNegate(booleanNegate(e))

    private fun toInt32(e: IrExpression) =
        JsIrBuilder.buildCall(intrinsics.jsBitOr, irBuiltIns.intType).apply {
            arguments[0] = e
            arguments[1] = buildInt(0)
        }
}
