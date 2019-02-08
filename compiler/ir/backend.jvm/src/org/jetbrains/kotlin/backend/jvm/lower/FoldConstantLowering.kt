/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateBinary
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateUnary

internal val foldConstantLoweringPhase = makeIrFilePhase(
    ::FoldConstantLowering,
    name = "FoldConstantLowering",
    description = "Constant Folding"
)

/**
 * A pass to fold constant expressions of most common types.
 *
 * For example, the expression "O" + 'K' + (1.toLong() + 2.0) will be folded to "OK3.0" at compile time.
 *
 * TODO: constant fields (e.g. Double.NaN)
 */
class FoldConstantLowering(private val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {

    /**
     * ID of an unary operator / method.
     *
     * An unary operator / method can be identified by its operand type (in full qualified name) and its name.
     */
    private data class UnaryOp(
        val operandType: String,
        val operatorName: String
    )

    /**
     * ID of an binary operator / method.
     *
     * An binary operator / method can be identified by its operand types (in full qualified names) and its name.
     */
    private data class BinaryOp(
        val lhsType: String,
        val rhsType: String,
        val operatorName: String
    )

    private data class PrimitiveType<T>(val name: String)

    companion object {
        private val BYTE = PrimitiveType<Byte>("Byte")
        private val SHORT = PrimitiveType<Short>("Short")
        private val INT = PrimitiveType<Int>("Int")
        private val LONG = PrimitiveType<Long>("Long")
        private val DOUBLE = PrimitiveType<Double>("Double")
        private val FLOAT = PrimitiveType<Float>("Float")
        private val CHAR = PrimitiveType<Char>("Char")
        private val BOOLEAN = PrimitiveType<Boolean>("Boolean")
        private val STRING = PrimitiveType<String>("String")

        private val UNARY_OP_TO_EVALUATOR = HashMap<UnaryOp, Function1<Any?, Any>>()
        private val BINARY_OP_TO_EVALUATOR = HashMap<BinaryOp, Function2<Any?, Any?, Any>>()

        @Suppress("UNCHECKED_CAST")
        private fun <T> registerBuiltinUnaryOp(operandType: PrimitiveType<T>, operatorName: String, f: (T) -> Any) {
            UNARY_OP_TO_EVALUATOR[UnaryOp(operandType.name, operatorName)] = f as Function1<Any?, Any>
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> registerBuiltinBinaryOp(operandType: PrimitiveType<T>, operatorName: String, f: (T, T) -> Any) {
            BINARY_OP_TO_EVALUATOR[BinaryOp(operandType.name, operandType.name, operatorName)] = f as Function2<Any?, Any?, Any>
        }

        init {
            // IrBuiltins
            registerBuiltinUnaryOp(BOOLEAN, IrBuiltIns.OperatorNames.NOT) { !it }

            registerBuiltinBinaryOp(DOUBLE, IrBuiltIns.OperatorNames.LESS) { a, b -> a < b }
            registerBuiltinBinaryOp(DOUBLE, IrBuiltIns.OperatorNames.LESS_OR_EQUAL) { a, b -> a <= b }
            registerBuiltinBinaryOp(DOUBLE, IrBuiltIns.OperatorNames.GREATER) { a, b -> a > b }
            registerBuiltinBinaryOp(DOUBLE, IrBuiltIns.OperatorNames.GREATER_OR_EQUAL) { a, b -> a >= b }
            registerBuiltinBinaryOp(DOUBLE, IrBuiltIns.OperatorNames.IEEE754_EQUALS) { a, b -> a == b }

            registerBuiltinBinaryOp(FLOAT, IrBuiltIns.OperatorNames.LESS) { a, b -> a < b }
            registerBuiltinBinaryOp(FLOAT, IrBuiltIns.OperatorNames.LESS_OR_EQUAL) { a, b -> a <= b }
            registerBuiltinBinaryOp(FLOAT, IrBuiltIns.OperatorNames.GREATER) { a, b -> a > b }
            registerBuiltinBinaryOp(FLOAT, IrBuiltIns.OperatorNames.GREATER_OR_EQUAL) { a, b -> a >= b }
            registerBuiltinBinaryOp(FLOAT, IrBuiltIns.OperatorNames.IEEE754_EQUALS) { a, b -> a == b }

            registerBuiltinBinaryOp(INT, IrBuiltIns.OperatorNames.LESS) { a, b -> a < b }
            registerBuiltinBinaryOp(INT, IrBuiltIns.OperatorNames.LESS_OR_EQUAL) { a, b -> a <= b }
            registerBuiltinBinaryOp(INT, IrBuiltIns.OperatorNames.GREATER) { a, b -> a > b }
            registerBuiltinBinaryOp(INT, IrBuiltIns.OperatorNames.GREATER_OR_EQUAL) { a, b -> a >= b }
            registerBuiltinBinaryOp(INT, IrBuiltIns.OperatorNames.EQEQ) { a, b -> a == b }

            registerBuiltinBinaryOp(LONG, IrBuiltIns.OperatorNames.LESS) { a, b -> a < b }
            registerBuiltinBinaryOp(LONG, IrBuiltIns.OperatorNames.LESS_OR_EQUAL) { a, b -> a <= b }
            registerBuiltinBinaryOp(LONG, IrBuiltIns.OperatorNames.GREATER) { a, b -> a > b }
            registerBuiltinBinaryOp(LONG, IrBuiltIns.OperatorNames.GREATER_OR_EQUAL) { a, b -> a >= b }
            registerBuiltinBinaryOp(LONG, IrBuiltIns.OperatorNames.EQEQ) { a, b -> a == b }
        }
    }

    private fun buildIrConstant(call: IrCall, v: Any): IrExpression {
        return when {
            call.type.isInt() -> IrConstImpl.int(call.startOffset, call.endOffset, call.type, v as Int)
            call.type.isChar() -> IrConstImpl.char(call.startOffset, call.endOffset, call.type, v as Char)
            call.type.isBoolean() -> IrConstImpl.boolean(call.startOffset, call.endOffset, call.type, v as Boolean)
            call.type.isByte() -> IrConstImpl.byte(call.startOffset, call.endOffset, call.type, v as Byte)
            call.type.isShort() -> IrConstImpl.short(call.startOffset, call.endOffset, call.type, v as Short)
            call.type.isLong() -> IrConstImpl.long(call.startOffset, call.endOffset, call.type, v as Long)
            call.type.isDouble() -> IrConstImpl.double(call.startOffset, call.endOffset, call.type, v as Double)
            call.type.isFloat() -> IrConstImpl.float(call.startOffset, call.endOffset, call.type, v as Float)
            call.type.isString() -> IrConstImpl.string(call.startOffset, call.endOffset, call.type, v as String)
            else -> throw IllegalArgumentException("Unexpected IrCall return type")
        }
    }

    private fun tryFoldingUnaryOps(call: IrCall): IrExpression {
        val operand = call.dispatchReceiver as? IrConst<*> ?: return call
        val evaluated = evaluateUnary(
            call.symbol.owner.name.toString(),
            operand.kind.toString(),
            operand.value!!
        ) ?: return call
        return buildIrConstant(call, evaluated)
    }

    private fun tryFoldingBuiltinUnaryOps(call: IrCall): IrExpression {
        if (call.symbol.owner.origin != IrDeclarationOrigin.IR_BUILTINS_STUB)
            return call

        val operand = call.getValueArgument(0) as? IrConst<*> ?: return call
        val evaluator = UNARY_OP_TO_EVALUATOR[UnaryOp(operand.kind.toString(), call.symbol.owner.name.toString())] ?: return call

        return buildIrConstant(call, evaluator(operand.value!!))
    }

    private fun tryFoldingBinaryOps(call: IrCall): IrExpression {
        val lhs = call.dispatchReceiver as? IrConst<*> ?: return call
        val rhs = call.getValueArgument(0) as? IrConst<*> ?: return call

        val evaluated = try {
            fun String.toNonNullable() = if (this.endsWith('?')) this.dropLast(1) else this
            evaluateBinary(
                call.symbol.owner.name.toString(),
                lhs.kind.toString(),
                lhs.value!!,
                // 1. Although some operators have nullable parameters, evaluators deals with non-nullable types only.
                //    The passed parameters are guaranteed to be non-null, since they are from IrConst.
                // 2. The operators are registered with prototype as if virtual member functions. They are identified by
                //    actual_receiver_type.operator_name(parameter_type_in_prototype).
                call.symbol.owner.valueParameters[0].type.toKotlinType().toString().toNonNullable(),
                rhs.value!!
            ) ?: return call
        } catch (e: Exception) {
            // Don't cast a runtime exception into compile time. E.g., division by zero.
            return call
        }

        return buildIrConstant(call, evaluated)
    }

    private fun tryFoldingBuiltinBinaryOps(call: IrCall): IrExpression {
        // Make sure that this is a IrBuiltIn
        if (call.symbol.owner.origin != IrDeclarationOrigin.IR_BUILTINS_STUB)
            return call

        val lhs = call.getValueArgument(0) as? IrConst<*> ?: return call
        val rhs = call.getValueArgument(1) as? IrConst<*> ?: return call

        val evaluated = try {
            val evaluator =
                BINARY_OP_TO_EVALUATOR[BinaryOp(lhs.kind.toString(), rhs.kind.toString(), call.symbol.owner.name.toString())] ?: return call
            evaluator(lhs.value!!, rhs.value!!)
        } catch (e: Exception) {
            return call
        }

        return buildIrConstant(call, evaluated)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                return when {
                    expression.extensionReceiver != null -> expression
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 0 -> tryFoldingUnaryOps(expression)
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 1 -> tryFoldingBinaryOps(expression)
                    expression.dispatchReceiver == null && expression.valueArgumentsCount == 1 -> tryFoldingBuiltinUnaryOps(expression)
                    expression.dispatchReceiver == null && expression.valueArgumentsCount == 2 -> tryFoldingBuiltinBinaryOps(expression)
                    else -> expression
                }
            }
        })
    }
}