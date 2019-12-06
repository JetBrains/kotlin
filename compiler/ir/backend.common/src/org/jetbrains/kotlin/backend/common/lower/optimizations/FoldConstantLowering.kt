/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateBinary
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateUnary

val foldConstantLoweringPhase = makeIrFilePhase(
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
class FoldConstantLowering(private val context: CommonBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
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

    @Suppress("unused")
    private data class PrimitiveType<T>(val name: String)

    companion object {
        private val INT = PrimitiveType<Int>("Int")
        private val LONG = PrimitiveType<Long>("Long")
        private val DOUBLE = PrimitiveType<Double>("Double")
        private val FLOAT = PrimitiveType<Float>("Float")

        private val BINARY_OP_TO_EVALUATOR = HashMap<BinaryOp, Function2<Any?, Any?, Any>>()

        @Suppress("UNCHECKED_CAST")
        private fun <T> registerBuiltinBinaryOp(operandType: PrimitiveType<T>, operatorName: String, f: (T, T) -> Any) {
            BINARY_OP_TO_EVALUATOR[BinaryOp(operandType.name, operandType.name, operatorName)] = f as Function2<Any?, Any?, Any>
        }

        init {
            // IrBuiltins
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
        if (call.symbol.owner.fqNameWhenAvailable?.parent() != IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)
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

    @ExperimentalUnsignedTypes
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                return when {
                    expression.extensionReceiver != null -> expression
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 0 -> tryFoldingUnaryOps(expression)
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 1 -> tryFoldingBinaryOps(expression)
                    expression.dispatchReceiver == null && expression.valueArgumentsCount == 2 -> tryFoldingBuiltinBinaryOps(expression)
                    else -> expression
                }
            }

            // Unsigned constants are represented through signed constants with a different IrType.
            private fun constToString(const: IrConst<*>): String {
                if (const.type.isUnsigned()) {
                    when (val kind = const.kind) {
                        is IrConstKind.Byte -> return kind.valueOf(const).toUByte().toString()
                        is IrConstKind.Short -> return kind.valueOf(const).toUShort().toString()
                        is IrConstKind.Int -> return kind.valueOf(const).toUInt().toString()
                        is IrConstKind.Long -> return kind.valueOf(const).toULong().toString()
                    }
                }
                return const.value.toString()
            }

            override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
                expression.transformChildrenVoid(this)
                val folded = mutableListOf<IrExpression>()
                for (next in expression.arguments) {
                    val last = folded.lastOrNull()
                    when {
                        next !is IrConst<*> -> folded += next
                        last !is IrConst<*> -> folded += IrConstImpl.string(
                            next.startOffset, next.endOffset, context.irBuiltIns.stringType, constToString(next)
                        )
                        else -> folded[folded.size - 1] = IrConstImpl.string(
                            last.startOffset, next.endOffset, context.irBuiltIns.stringType,
                            constToString(last) + constToString(next)
                        )
                    }
                }
                return folded.singleOrNull() as? IrConst<*>
                    ?: IrStringConcatenationImpl(expression.startOffset, expression.endOffset, expression.type, folded)
            }
        })
    }
}