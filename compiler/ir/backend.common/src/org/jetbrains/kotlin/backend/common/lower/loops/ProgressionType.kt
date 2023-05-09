/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irLong
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.defaultType

/** Represents a progression type in the Kotlin stdlib. */
sealed class ProgressionType(
    val elementClass: IrClass,
    val stepClass: IrClass,
    val minValueAsLong: Long,
    val maxValueAsLong: Long,
    val getProgressionLastElementFunction: IrSimpleFunctionSymbol?
) {
    abstract fun DeclarationIrBuilder.zeroStepExpression(): IrExpression

    fun IrExpression.asElementType() = castIfNecessary(elementClass)

    fun IrExpression.asStepType() = castIfNecessary(stepClass)

    companion object {
        fun fromIrType(irType: IrType, symbols: Symbols, allowUnsignedBounds: Boolean): ProgressionType? =
            when {
                irType.isSubtypeOfClass(symbols.charProgression) -> CharProgressionType(symbols)
                irType.isSubtypeOfClass(symbols.intProgression) -> IntProgressionType(symbols)
                irType.isSubtypeOfClass(symbols.longProgression) -> LongProgressionType(symbols)
                symbols.uIntProgression != null && irType.isSubtypeOfClass(symbols.uIntProgression) ->
                    UIntProgressionType(symbols, allowUnsignedBounds)
                symbols.uLongProgression != null && irType.isSubtypeOfClass(symbols.uLongProgression) ->
                    ULongProgressionType(symbols, allowUnsignedBounds)
                else -> null
            }
    }
}

internal class IntProgressionType(symbols: Symbols) :
    ProgressionType(
        elementClass = symbols.int.owner,
        stepClass = symbols.int.owner,
        minValueAsLong = Int.MIN_VALUE.toLong(),
        maxValueAsLong = Int.MAX_VALUE.toLong(),
        // Uses `getProgressionLastElement(Int, Int, Int): Int`
        getProgressionLastElementFunction = symbols.getProgressionLastElementByReturnType[symbols.int]
    ) {

    override fun DeclarationIrBuilder.zeroStepExpression() = irInt(0)
}

internal class LongProgressionType(symbols: Symbols) :
    ProgressionType(
        elementClass = symbols.long.owner,
        stepClass = symbols.long.owner,
        minValueAsLong = Long.MIN_VALUE,
        maxValueAsLong = Long.MAX_VALUE,
        // Uses `getProgressionLastElement(Long, Long, Long): Long`
        getProgressionLastElementFunction = symbols.getProgressionLastElementByReturnType[symbols.long]
    ) {

    override fun DeclarationIrBuilder.zeroStepExpression() = irLong(0)
}

internal class CharProgressionType(symbols: Symbols) :
    ProgressionType(
        elementClass = symbols.char.owner,
        stepClass = symbols.int.owner,
        minValueAsLong = Char.MIN_VALUE.code.toLong(),
        maxValueAsLong = Char.MAX_VALUE.code.toLong(),
        // Uses `getProgressionLastElement(Int, Int, Int): Int`
        getProgressionLastElementFunction = symbols.getProgressionLastElementByReturnType[symbols.int]
    ) {

    override fun DeclarationIrBuilder.zeroStepExpression() = irInt(0)
}

// A note on how ForLoopsLowering handles unsigned progressions:
//
// We use signed numbers for the elements (induction variable and bounds) to limit calls to UInt/ULong constructors. For example,
// `inductionVar += step.toUInt()` would cause instantiation during `toUInt()` (conversion necessary since `step` is signed) and on
// assignment to `inductionVar`. There are a few places where we _have_ to convert either the induction variable or bounds to unsigned:
//
//   1. When comparing in the loop conditions (e.g., `inductionVar <= last`). This ensures that the correct comparison function is used
//      (`UInt/ULongCompare`) instead of regular Int/Long comparisons.
//   2. When assigning to the loop variable, which should have an unsigned type.
//   3. When calling `getProgressionLastElement` for stepped progressions. There are overloads which take unsigned numbers, which should
//      be used to ensure the calculation is correct.
//
// We use the `<unsafe-coerce>` intrinsic if available (currently JVM-only) to perform the conversions, and fallback to calling
// `to(U)Int/(U)Long()` functions otherwise.

internal abstract class UnsignedProgressionType(
    symbols: Symbols,
    elementClass: IrClass,
    stepClass: IrClass,
    minValueAsLong: Long,
    maxValueAsLong: Long,
    getProgressionLastElementFunction: IrSimpleFunctionSymbol?,
    val unsignedType: IrType,
    private val unsignedConversionFunction: IrSimpleFunctionSymbol
) : ProgressionType(elementClass, stepClass, minValueAsLong, maxValueAsLong, getProgressionLastElementFunction) {

    private val unsafeCoerceIntrinsic = symbols.unsafeCoerceIntrinsic

    fun IrExpression.asUnsigned(): IrExpression {
        val fromType = type
        if (type == unsignedType) return this

        return if (unsafeCoerceIntrinsic != null) {
            IrCallImpl(
                startOffset, endOffset,
                unsignedType,
                unsafeCoerceIntrinsic,
                typeArgumentsCount = 2,
                valueArgumentsCount = 1
            ).apply {
                putTypeArgument(0, fromType)
                putTypeArgument(1, unsignedType)
                putValueArgument(0, this@asUnsigned)
            }
        } else {
            // Fallback to calling `toUInt/ULong()` extension function.
            IrCallImpl(
                startOffset, endOffset, unsignedConversionFunction.owner.returnType,
                unsignedConversionFunction,
                typeArgumentsCount = 0,
                valueArgumentsCount = 0
            ).apply {
                extensionReceiver = this@asUnsigned
            }
        }
    }

    fun IrExpression.asSigned(): IrExpression {
        val toType = elementClass.defaultType
        if (type == toType) return this

        return if (unsafeCoerceIntrinsic != null) {
            IrCallImpl(
                startOffset, endOffset, toType,
                unsafeCoerceIntrinsic,
                typeArgumentsCount = 2,
                valueArgumentsCount = 1
            ).apply {
                putTypeArgument(0, unsignedType)
                putTypeArgument(1, toType)
                putValueArgument(0, this@asSigned)
            }
        } else {
            // Fallback to calling `toInt/Long()` function.
            asElementType()
        }
    }
}

internal class UIntProgressionType(symbols: Symbols, allowUnsignedBounds: Boolean) :
    UnsignedProgressionType(
        symbols,
        elementClass = if (allowUnsignedBounds) symbols.uInt!!.owner else symbols.int.owner,
        stepClass = symbols.int.owner,
        minValueAsLong = UInt.MIN_VALUE.toLong(),
        maxValueAsLong = UInt.MAX_VALUE.toLong(),
        // Uses `getProgressionLastElement(UInt, UInt, Int): UInt`
        getProgressionLastElementFunction = symbols.getProgressionLastElementByReturnType[symbols.uInt!!],
        unsignedType = symbols.uInt.defaultType,
        unsignedConversionFunction = symbols.toUIntByExtensionReceiver.getValue(symbols.int)
    ) {

    override fun DeclarationIrBuilder.zeroStepExpression() = irInt(0)
}

internal class ULongProgressionType(symbols: Symbols, allowUnsignedBounds: Boolean) :
    UnsignedProgressionType(
        symbols,
        elementClass = if (allowUnsignedBounds) symbols.uLong!!.owner else symbols.long.owner,
        stepClass = symbols.long.owner,
        minValueAsLong = ULong.MIN_VALUE.toLong(),
        maxValueAsLong = ULong.MAX_VALUE.toLong(),
        // Uses `getProgressionLastElement(ULong, ULong, Long): ULong`
        getProgressionLastElementFunction = symbols.getProgressionLastElementByReturnType[symbols.uLong!!],
        unsignedType = symbols.uLong.defaultType,
        unsignedConversionFunction = symbols.toULongByExtensionReceiver.getValue(symbols.long)
    ) {

    override fun DeclarationIrBuilder.zeroStepExpression() = irLong(0)
}
