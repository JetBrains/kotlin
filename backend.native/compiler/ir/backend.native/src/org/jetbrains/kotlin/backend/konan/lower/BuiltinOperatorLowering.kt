/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.containsNull
import org.jetbrains.kotlin.backend.konan.ir.isSubtypeOf
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This lowering pass lowers some calls to [IrBuiltinOperatorDescriptor]s.
 */
internal class BuiltinOperatorLowering(val context: Context) : FileLoweringPass, IrBuildingTransformer(context) {

    private val irBuiltins = context.irModule!!.irBuiltins
    private val symbols = context.ir.symbols

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.symbol) {
            irBuiltins.eqeqSymbol, in ieee754EqualsSymbols() -> lowerEqeq(expression)

            irBuiltins.eqeqeqSymbol -> lowerEqeqeq(expression)

            irBuiltins.checkNotNullSymbol -> lowerCheckNotNull(expression)

            irBuiltins.noWhenBranchMatchedExceptionSymbol -> IrCallImpl(expression.startOffset, expression.endOffset,
                    context.ir.symbols.throwNoWhenBranchMatchedException.owner.returnType,
                    context.ir.symbols.throwNoWhenBranchMatchedException)

            else -> expression
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.argument.type.isNothing()) {
            return expression.argument
        }
        return expression
    }

    private fun ieee754EqualsSymbols(): List<IrSimpleFunctionSymbol> =
            irBuiltins.ieee754equalsFunByOperandType.values.toList()

    private fun lowerEqeqeq(expression: IrCall): IrExpression {
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!

        return if (lhs.type.isInlinedNative() && rhs.type.isInlinedNative()) {
            // Achieve the same behavior as with JVM BE: if both sides of `===` are values, then compare by value:
            lowerEqeq(expression)
            // Note: such comparisons are deprecated.
        } else {
            expression
        }
    }

    private fun IrBuilderWithScope.reinterpret(expression: IrExpression, toType: IrType) =
            reinterpret(expression, expression.type, toType)

    private fun IrBuilderWithScope.reinterpret(expression: IrExpression, fromType: IrType, toType: IrType) =
            irCall(symbols.reinterpret.owner, listOf(fromType, toType)).apply {
                extensionReceiver = expression
            }

    private val anyEquals = irBuiltins.anyClass.owner.simpleFunctions().single { it.name.asString() == "equals" }

    private fun lowerEqeq(expression: IrCall): IrExpression {
        // TODO: optimize boxing?

        builder.at(expression).run {
            val lhs = expression.getValueArgument(0)!!
            val rhs = expression.getValueArgument(1)!!

            if (rhs.isNullConst()) {
                return irEqeqNull(lhs)
            }

            if (lhs.isNullConst()) {
                return irEqeqNull(rhs)
            }

            if (expression.symbol == irBuiltins.eqeqSymbol) {
                lhs.type.getInlinedClassNative()?.let {
                    if (it == rhs.type.getInlinedClassNative() && inlinedClassHasDefaultEquals(it)) {
                        return genInlineClassEquals(expression.symbol, rhs, lhs)
                    }
                }
            }

            return genFloatingOrReferenceEquals(expression.symbol, lhs, rhs)
        }
    }

    private fun inlinedClassHasDefaultEquals(irClass: IrClass): Boolean {
        if (!irClass.descriptor.isInline) {
            // Implicitly-inlined class, e.g. primitive one.
            return true
        }

        val equals = irClass.simpleFunctions()
                .single { it.name.asString() == "equals" && it.valueParameters.size == 1 && it.overrides(anyEquals) }

        return equals.origin == IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER
    }

    fun IrBuilderWithScope.genInlineClassEquals(
            symbol: IrFunctionSymbol,
            rhs: IrExpression,
            lhs: IrExpression
    ): IrExpression {
        val lhsBinaryType = lhs.type.computeBinaryType()
        return when (lhsBinaryType) {
            is BinaryType.Primitive -> {
                val areEqualByValue = symbols.areEqualByValue[lhsBinaryType.type]!!.owner
                irCall(areEqualByValue).apply {
                    putValueArgument(0, reinterpret(lhs, areEqualByValue.valueParameters[0].type))
                    putValueArgument(1, reinterpret(rhs, areEqualByValue.valueParameters[1].type))
                }
            }

            is BinaryType.Reference -> {
                // TODO: don't use binaryType.nullable.
                val lhsRawType = irBuiltins.anyClass.owner.defaultOrNullableType(lhsBinaryType.nullable)
                val rhsBinaryType = rhs.type.computeBinaryType() as BinaryType.Reference<*>
                val rhsRawType = irBuiltins.anyClass.owner.defaultOrNullableType(rhsBinaryType.nullable)

                genFloatingOrReferenceEquals(
                        symbol,
                        reinterpret(lhs, lhsRawType),
                        reinterpret(rhs, rhsRawType)
                )
            }
        }
    }

    private fun IrBuilderWithScope.irEqeqNull(expression: IrExpression): IrExpression {
        val type = expression.type.makeNullable()
        return when (val primitiveBinaryTypeOrNull = type.computePrimitiveBinaryTypeOrNull()) {
            null -> irEqeqeq(reinterpret(expression, type, irBuiltins.anyNType), irNull())
            PrimitiveBinaryType.POINTER -> irCall(symbols.areEqualByValue[PrimitiveBinaryType.POINTER]!!.owner).apply {
                putValueArgument(0, reinterpret(expression, type, symbols.nativePtrType))
                putValueArgument(1, reinterpret(irNull(), type, symbols.nativePtrType))
            }
            else -> error("Nullable type ${type.render()} is $primitiveBinaryTypeOrNull")
        }
    }

    private fun lowerCheckNotNull(expression: IrCall) = builder.at(expression).irBlock(resultType = expression.type) {
        val temp = irTemporary(expression.getValueArgument(0)!!)
        +irIfThen(context.irBuiltIns.unitType, irEqeqNull(irGet(temp)), irCall(symbols.throwNullPointerException))
        +irGet(temp)
    }

    private fun IrBuilderWithScope.irLogicalAnd(lhs: IrExpression, rhs: IrExpression) = context.andand(lhs, rhs)
    private fun IrBuilderWithScope.irIsNull(exp: IrExpression) = irEqeqeq(exp, irNull())
    private fun IrBuilderWithScope.irIsNotNull(exp: IrExpression) = irNot(irEqeqeq(exp, irNull()))

    private fun IrBuilderWithScope.genFloatingOrReferenceEquals(symbol: IrFunctionSymbol, lhs: IrExpression, rhs: IrExpression): IrExpression {
        // TODO: areEqualByValue and ieee754Equals intrinsics are specially treated by code generator
        // and thus can be declared synthetically in the compiler instead of explicitly in the runtime.
        fun callEquals(lhs: IrExpression, rhs: IrExpression) =
                if (symbol in ieee754EqualsSymbols())
                // Find a type-compatible `konan.internal.ieee754Equals` intrinsic:
                    irCall(selectIntrinsic(symbols.ieee754Equals, lhs.type, rhs.type, true)!!).apply {
                        putValueArgument(0, lhs)
                        putValueArgument(1, rhs)
                    }
                else
                    irCall(symbols.equals).apply {
                        dispatchReceiver = lhs
                        putValueArgument(0, rhs)
                    }

        val lhsIsNotNullable = !lhs.type.containsNull()
        val rhsIsNotNullable = !rhs.type.containsNull()

        return if (symbol in ieee754EqualsSymbols()) {
            if (lhsIsNotNullable && rhsIsNotNullable)
                callEquals(lhs, rhs)
            else irBlock {
                val lhsTemp = irTemporary(lhs)
                val rhsTemp = irTemporary(rhs)
                if (lhsIsNotNullable xor rhsIsNotNullable) { // Exactly one nullable.
                    +irLogicalAnd(
                            irIsNotNull(irGet(if (lhsIsNotNullable) rhsTemp else lhsTemp)),
                            callEquals(irGet(lhsTemp), irGet(rhsTemp))
                    )
                } else { // Both are nullable.
                    +irIfThenElse(context.irBuiltIns.booleanType, irIsNull(irGet(lhsTemp)),
                            irIsNull(irGet(rhsTemp)),
                            irLogicalAnd(
                                    irIsNotNull(irGet(rhsTemp)),
                                    callEquals(irGet(lhsTemp), irGet(rhsTemp))
                            )
                    )
                }
            }
        } else {
            if (lhsIsNotNullable)
                callEquals(lhs, rhs)
            else {
                irBlock {
                    val lhsTemp = irTemporary(lhs)
                    if (rhsIsNotNullable)
                        +irLogicalAnd(irIsNotNull(irGet(lhsTemp)), callEquals(irGet(lhsTemp), rhs))
                    else {
                        val rhsTemp = irTemporary(rhs)
                        +irIfThenElse(irBuiltins.booleanType, irIsNull(irGet(lhsTemp)),
                                irIsNull(irGet(rhsTemp)),
                                callEquals(irGet(lhsTemp), irGet(rhsTemp))
                        )
                    }
                }
            }
        }
    }

    private fun selectIntrinsic(from: List<IrSimpleFunctionSymbol>, lhsType: IrType, rhsType: IrType, allowNullable: Boolean) =
            from.atMostOne {
                val leftParamType = it.owner.valueParameters[0].type
                val rightParamType = it.owner.valueParameters[1].type
                (lhsType.isSubtypeOf(leftParamType) || (allowNullable && lhsType.isSubtypeOf(leftParamType.makeNullable())))
                        && (rhsType.isSubtypeOf(rightParamType) || (allowNullable && rhsType.isSubtypeOf(rightParamType.makeNullable())))
            }
}
