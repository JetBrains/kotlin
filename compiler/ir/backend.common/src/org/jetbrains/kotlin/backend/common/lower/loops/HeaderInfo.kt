/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

// TODO: Handle withIndex()
// TODO: Handle reversed()
// TODO: Handle Strings/CharSequences
// TODO: Handle UIntProgression, ULongProgression

/** Represents a progression type in the Kotlin stdlib. */
enum class ProgressionType(val elementCastFunctionName: Name, val stepCastFunctionName: Name) {
    INT_PROGRESSION(Name.identifier("toInt"), Name.identifier("toInt")),
    LONG_PROGRESSION(Name.identifier("toLong"), Name.identifier("toLong")),
    CHAR_PROGRESSION(Name.identifier("toChar"), Name.identifier("toInt"));

    /** Returns the [IrType] of the `first`/`last` properties and elements in the progression. */
    fun elementType(builtIns: IrBuiltIns): IrType = when (this) {
        ProgressionType.INT_PROGRESSION -> builtIns.intType
        ProgressionType.LONG_PROGRESSION -> builtIns.longType
        ProgressionType.CHAR_PROGRESSION -> builtIns.charType
    }

    /** Returns the [IrType] of the `step` property in the progression. */
    fun stepType(builtIns: IrBuiltIns): IrType = when (this) {
        ProgressionType.INT_PROGRESSION, ProgressionType.CHAR_PROGRESSION -> builtIns.intType
        ProgressionType.LONG_PROGRESSION -> builtIns.longType
    }

    companion object {
        fun fromIrType(irType: IrType, symbols: Symbols<CommonBackendContext>): ProgressionType? = when {
            irType.isSubtypeOfClass(symbols.charProgression) -> ProgressionType.CHAR_PROGRESSION
            irType.isSubtypeOfClass(symbols.intProgression) -> ProgressionType.INT_PROGRESSION
            irType.isSubtypeOfClass(symbols.longProgression) -> ProgressionType.LONG_PROGRESSION
            else -> null
        }
    }
}

internal enum class ProgressionDirection { DECREASING, INCREASING, UNKNOWN }

/** Information about a loop that is required by [HeaderProcessor] to build a [ForLoopHeader]. */
internal sealed class HeaderInfo(
    val progressionType: ProgressionType,
    val first: IrExpression,
    val last: IrExpression,
    val step: IrExpression,
    val isLastInclusive: Boolean
) {
    val direction: ProgressionDirection by lazy {
        // If step is a constant (either Int or Long), then we can determine the direction.
        val stepValue = (step as? IrConst<*>)?.value as? Number
        val stepValueAsLong = stepValue?.toLong()
        when {
            stepValueAsLong == null -> ProgressionDirection.UNKNOWN
            stepValueAsLong < 0L -> ProgressionDirection.DECREASING
            stepValueAsLong > 0L -> ProgressionDirection.INCREASING
            else -> ProgressionDirection.UNKNOWN
        }
    }
}

/** Information about a for-loop over a progression. */
internal class ProgressionHeaderInfo(
    progressionType: ProgressionType,
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    isLastInclusive: Boolean = true,
    canOverflow: Boolean? = null,
    val additionalVariables: List<IrVariable> = listOf()
) : HeaderInfo(progressionType, first, last, step, isLastInclusive) {

    private val _canOverflow: Boolean? = canOverflow
    val canOverflow: Boolean by lazy {
        if (_canOverflow != null) return@lazy _canOverflow

        // Induction variable can overflow if it is not a const, or is MAX/MIN_VALUE (depending on direction).
        val lastValue = (last as? IrConst<*>)?.value
        val lastValueAsLong = when (lastValue) {
            is Number -> lastValue.toLong()
            is Char -> lastValue.toLong()
            else -> return@lazy true  // If "last" is not a const Number or Char.
        }
        val constLimitAsLong = when (direction) {
            ProgressionDirection.UNKNOWN ->
                // If we don't know the direction, we can't be sure which limit to use.
                return@lazy true
            ProgressionDirection.DECREASING ->
                when (progressionType) {
                    ProgressionType.INT_PROGRESSION -> Int.MIN_VALUE.toLong()
                    ProgressionType.CHAR_PROGRESSION -> Char.MIN_VALUE.toLong()
                    ProgressionType.LONG_PROGRESSION -> Long.MIN_VALUE
                }
            ProgressionDirection.INCREASING ->
                when (progressionType) {
                    ProgressionType.INT_PROGRESSION -> Int.MAX_VALUE.toLong()
                    ProgressionType.CHAR_PROGRESSION -> Char.MAX_VALUE.toLong()
                    ProgressionType.LONG_PROGRESSION -> Long.MAX_VALUE
                }
        }
        constLimitAsLong == lastValueAsLong
    }
}

/** Information about a for-loop over an array. The internal induction variable used is an Int. */
internal class ArrayHeaderInfo(
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    val arrayVariable: IrVariable
) : HeaderInfo(
    ProgressionType.INT_PROGRESSION,
    first,
    last,
    step,
    isLastInclusive = false
)

/** Matches a call to `iterator()` and builds a [HeaderInfo] out of the call's context. */
internal interface HeaderInfoHandler<T> {
    val matcher: IrCallMatcher

    fun build(call: IrCall, data: T): HeaderInfo?

    fun handle(irCall: IrCall, data: T) = if (matcher(irCall)) {
        build(irCall, data)
    } else {
        null
    }
}
internal typealias ProgressionHandler = HeaderInfoHandler<ProgressionType>

/**
 * Handles a call to `iterator()` on more specialized forms of progressions, built using extension
 * and member functions/properties in the stdlib (e.g., `.indices`, `downTo`).
 */
private class ProgressionHeaderInfoBuilder(val context: CommonBackendContext) : IrElementVisitor<HeaderInfo?, Nothing?> {

    private val symbols = context.ir.symbols

    // TODO: Include unsigned types
    private val progressionElementTypes = symbols.integerClassesTypes + context.irBuiltIns.char

    private val progressionHandlers = listOf(
        IndicesHandler(context),
        UntilHandler(context, progressionElementTypes),
        DownToHandler(context, progressionElementTypes),
        RangeToHandler(context, progressionElementTypes)
    )

    override fun visitElement(element: IrElement, data: Nothing?): HeaderInfo? = null

    override fun visitCall(expression: IrCall, data: Nothing?): HeaderInfo? {
        // Return the HeaderInfo from the first successful match.
        val progressionType = ProgressionType.fromIrType(expression.type, symbols)
            ?: return null
        return progressionHandlers.firstNotNullResult { it.handle(expression, progressionType) }
    }
}

internal class HeaderInfoBuilder(context: CommonBackendContext) {
    private val progressionHeaderInfoBuilder = ProgressionHeaderInfoBuilder(context)
    private val arrayIterationHandler = ArrayIterationHandler(context)
    private val defaultProgressionHandler = DefaultProgressionHandler(context)

    fun build(variable: IrVariable): HeaderInfo? {
        // TODO: Merge DefaultProgressionHandler into ProgressionHeaderInfoBuilder. Not
        // straightforward because ProgressionHeaderInfoBuilder works only on calls and not just
        // any progression expression (i.e., progression may not be a call result).

        // DefaultProgressionHandler must come AFTER ProgressionHeaderInfoBuilder, which handles
        // more specialized forms of progressions.
        val initializer = variable.initializer as IrCall
        return arrayIterationHandler.handle(initializer, null)
            ?: initializer.dispatchReceiver?.accept(progressionHeaderInfoBuilder, null)
            ?: defaultProgressionHandler.handle(initializer, null)
    }
}