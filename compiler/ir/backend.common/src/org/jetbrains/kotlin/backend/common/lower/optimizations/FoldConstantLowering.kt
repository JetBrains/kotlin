/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.optimizations

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isUnsigned
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateBinary
import org.jetbrains.kotlin.resolve.constants.evaluate.evaluateUnary
import kotlin.math.floor

val foldConstantLoweringPhase = makeIrFilePhase(
    { ctx: CommonBackendContext -> FoldConstantLowering(ctx) },
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
class FoldConstantLowering(
    private val context: CommonBackendContext,
    // In K/JS Float and Double are the same so Float constant should be fold similar to Double
    private val floatSpecial: Boolean = false) : IrElementTransformerVoid(), BodyLoweringPass {
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

    private fun fromFloatConstSafe(startOffset: Int, endOffset: Int, type: IrType, v: Any?): IrConst<*> =
        when {
            !floatSpecial -> IrConstImpl.float(startOffset, endOffset, type, (v as Number).toFloat())
            v is Float -> IrConstImpl.float(startOffset, endOffset, type, v)
            v is Double -> IrConstImpl.double(startOffset, endOffset, type, v)
            else -> error("Unexpected constant type")
        }

    private fun buildIrConstant(startOffset: Int, endOffset: Int, type: IrType, v: Any?): IrConst<*> {
        val constType = type.makeNotNull()
        return when {
            constType.isInt() -> IrConstImpl.int(startOffset, endOffset, constType, (v as Number).toInt())
            constType.isChar() -> IrConstImpl.char(startOffset, endOffset, constType, v as Char)
            constType.isBoolean() -> IrConstImpl.boolean(startOffset, endOffset, constType, v as Boolean)
            constType.isByte() -> IrConstImpl.byte(startOffset, endOffset, constType, (v as Number).toByte())
            constType.isShort() -> IrConstImpl.short(startOffset, endOffset, constType, (v as Number).toShort())
            constType.isLong() -> IrConstImpl.long(startOffset, endOffset, constType, (v as Number).toLong())
            constType.isDouble() -> IrConstImpl.double(startOffset, endOffset, constType, (v as Number).toDouble())
            constType.isFloat() -> fromFloatConstSafe(startOffset, endOffset, type, v)
            constType.isString() -> IrConstImpl.string(startOffset, endOffset, constType, v as String)
            else -> throw IllegalArgumentException("Unexpected IrCall return type")
        }
    }

    private fun tryFoldingUnaryOps(call: IrCall): IrExpression {
        val operand = call.dispatchReceiver as? IrConst<*> ?: return call
        val operationName = call.symbol.owner.name.toString()

        val evaluated = when {
            // Since there is no distinguish between signed and unsigned types a special handling for `toString` is required
            operationName == "toString" -> constToString(operand)
            // Disable toFloat folding on K/JS till `toFloat` is fixed (KT-35422)
            operationName == "toFloat" && floatSpecial -> return call
            else -> evaluateUnary(
                operationName,
                operand.kind.toString(),
                operand.value!!
            ) ?: return call
        }

        return buildIrConstant(call.startOffset, call.endOffset, call.type, evaluated)
    }

    private fun coerceToDouble(irConst: IrConst<*>): IrConst<*> {
        // TODO: for consistency with current K/JS implementation Float constant should be treated as a Double (KT-35422)
        if (!floatSpecial) return irConst
        if (irConst.kind == IrConstKind.Float) return irConst.run {
            IrConstImpl(startOffset, endOffset, context.irBuiltIns.doubleType, IrConstKind.Double, value.toString().toDouble())
        }
        return irConst
    }

    private fun IrType.typeConstructorName(): String {
        with(this as IrSimpleType) {
            with(classifier as IrClassSymbol) {
                return owner.name.asString()
            }
        }
    }

    private fun tryFoldingBinaryOps(call: IrCall): IrExpression {
        val lhs = coerceToDouble(call.dispatchReceiver as? IrConst<*> ?: return call)
        val rhs = coerceToDouble(call.getValueArgument(0) as? IrConst<*> ?: return call)

        val evaluated = try {
            evaluateBinary(
                call.symbol.owner.name.toString(),
                lhs.kind.toString(),
                lhs.value!!,
                // 1. Although some operators have nullable parameters, evaluators deals with non-nullable types only.
                //    The passed parameters are guaranteed to be non-null, since they are from IrConst.
                // 2. The operators are registered with prototype as if virtual member functions. They are identified by
                //    actual_receiver_type.operator_name(parameter_type_in_prototype).
                call.symbol.owner.valueParameters[0].type.typeConstructorName(),
                rhs.value!!
            ) ?: return call
        } catch (e: Exception) {
            // Don't cast a runtime exception into compile time. E.g., division by zero.
            return call
        }

        return buildIrConstant(call.startOffset, call.endOffset, call.type, evaluated)
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

        return buildIrConstant(call.startOffset, call.endOffset, call.type, evaluated)
    }

    // Unsigned constants are represented through signed constants with a different IrType.
    private fun constToString(const: IrConst<*>): String {
        if (floatSpecial) {
            when (val kind = const.kind) {
                is IrConstKind.Float -> {
                    val f = kind.valueOf(const)
                    if (!f.isInfinite()) {
                        if (floor(f) == f) {
                            return f.toInt().toString()
                        }
                    }
                }
                is IrConstKind.Double -> {
                    val d = kind.valueOf(const)
                    if (!d.isInfinite()) {
                        if (floor(d) == d) {
                            return d.toLong().toString()
                        }
                    }
                }
            }
        }

        if (const.type.isUnsigned()) {
            @OptIn(ExperimentalUnsignedTypes::class)
            when (val kind = const.kind) {
                is IrConstKind.Byte ->
                    return kind.valueOf(const).toUByte().toString()
                is IrConstKind.Short ->
                    return kind.valueOf(const).toUShort().toString()
                is IrConstKind.Int ->
                    return kind.valueOf(const).toUInt().toString()
                is IrConstKind.Long ->
                    return kind.valueOf(const).toULong().toString()
            }
        }
        return const.value.toString()
    }

    @ExperimentalUnsignedTypes
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()

                return when {
                    expression.extensionReceiver != null -> expression
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 0 -> tryFoldingUnaryOps(expression)
                    expression.dispatchReceiver != null && expression.valueArgumentsCount == 1 -> tryFoldingBinaryOps(expression)
                    expression.dispatchReceiver == null && expression.valueArgumentsCount == 2 -> tryFoldingBuiltinBinaryOps(expression)
                    else -> expression
                }
            }

            override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
                expression.transformChildrenVoid()
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

            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid()
                val argument = expression.argument
                return if (argument is IrConst<*> && expression.operator == IrTypeOperator.IMPLICIT_INTEGER_COERCION)
                    buildIrConstant(expression.startOffset, expression.endOffset, expression.type, argument.value)
                else
                    expression
            }
        })
    }
}