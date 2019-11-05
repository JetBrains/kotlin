/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

// TODO: Handle withIndex()
// TODO: Handle UIntProgression, ULongProgression

/** Represents a progression type in the Kotlin stdlib. */
internal enum class ProgressionType(val elementCastFunctionName: Name, val stepCastFunctionName: Name) {
    INT_PROGRESSION(Name.identifier("toInt"), Name.identifier("toInt")),
    LONG_PROGRESSION(Name.identifier("toLong"), Name.identifier("toLong")),
    CHAR_PROGRESSION(Name.identifier("toChar"), Name.identifier("toInt"));

    /** Returns the [IrType] of the `first`/`last` properties and elements in the progression. */
    fun elementType(builtIns: IrBuiltIns): IrType = when (this) {
        INT_PROGRESSION -> builtIns.intType
        LONG_PROGRESSION -> builtIns.longType
        CHAR_PROGRESSION -> builtIns.charType
    }

    /** Returns the [IrType] of the `step` property in the progression. */
    fun stepType(builtIns: IrBuiltIns): IrType = when (this) {
        INT_PROGRESSION, CHAR_PROGRESSION -> builtIns.intType
        LONG_PROGRESSION -> builtIns.longType
    }

    companion object {
        fun fromIrType(irType: IrType, symbols: Symbols<CommonBackendContext>): ProgressionType? = when {
            irType.isSubtypeOfClass(symbols.charProgression) -> CHAR_PROGRESSION
            irType.isSubtypeOfClass(symbols.intProgression) -> INT_PROGRESSION
            irType.isSubtypeOfClass(symbols.longProgression) -> LONG_PROGRESSION
            else -> null
        }
    }
}

internal enum class ProgressionDirection {
    DECREASING {
        override fun asReversed() = INCREASING
    },
    INCREASING {
        override fun asReversed() = DECREASING
    },
    UNKNOWN {
        override fun asReversed() = UNKNOWN
    };

    abstract fun asReversed(): ProgressionDirection
}

/** Information about a loop that is required by [HeaderProcessor] to build a [ForLoopHeader]. */
internal sealed class HeaderInfo {
    /**
     * Returns a copy of this [HeaderInfo] with the values reversed.
     * I.e., first and last are swapped, step is negated.
     * Returns null if the iterable cannot be iterated in reverse.
     */
    abstract fun asReversed(): HeaderInfo?
}

internal sealed class NumericHeaderInfo(
    val progressionType: ProgressionType,
    val first: IrExpression,
    val last: IrExpression,
    val step: IrExpression,
    val canCacheLast: Boolean,
    val isReversed: Boolean,
    val direction: ProgressionDirection,
    val additionalNotEmptyCondition: IrExpression?
) : HeaderInfo()

/** Information about a for-loop over a progression. */
internal class ProgressionHeaderInfo(
    progressionType: ProgressionType,
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    isReversed: Boolean = false,
    canOverflow: Boolean? = null,
    direction: ProgressionDirection,
    additionalNotEmptyCondition: IrExpression? = null,
    val additionalVariables: List<IrVariable> = listOf()
) : NumericHeaderInfo(
    progressionType, first, last, step,
    canCacheLast = true,
    isReversed = isReversed,
    direction = direction,
    additionalNotEmptyCondition = additionalNotEmptyCondition
) {

    val canOverflow: Boolean by lazy {
        if (canOverflow != null) return@lazy canOverflow

        // We can't determine the safe limit at compile-time if "step" is not const.
        val stepValueAsLong = step.constLongValue ?: return@lazy true

        // Induction variable can NOT overflow if "last" is const and is <= (MAX/MIN_VALUE - step) (depending on direction).
        //
        // Examples that can NOT overflow:
        //   - `0..10` cannot overflow (10 <= MAX_VALUE - 1)
        //   - `0..MAX_VALUE - 1` cannot overflow (MAX_VALUE - 1 <= MAX_VALUE - 1)
        //   - `0..MAX_VALUE - 3 step 3` cannot overflow (MAX_VALUE - 3 <= MAX_VALUE - 3)
        //   - `0 downTo -10` cannot overflow (-10 >= MIN_VALUE - (-1))
        //   - `0 downTo MIN_VALUE + 1` (step is -1) cannot overflow (MIN_VALUE + 1 >= MIN_VALUE - (-1))
        //   - `0 downTo MIN_VALUE + 3 step 3` (step is -3) cannot overflow (MIN_VALUE + 3 >= MIN_VALUE - (-3))
        //
        // Examples that CAN overflow:
        //   - `0..MAX_VALUE` CAN overflow (MAX_VALUE > MAX_VALUE - 1)
        //   - `0..MAX_VALUE - 2 step 3` cannot overflow (MAX_VALUE - 2 > MAX_VALUE - 3)
        //   - `0 downTo MIN_VALUE` (step is -1) CAN overflow (MIN_VALUE < MIN_VALUE - (-1))
        //   - `0 downTo MIN_VALUE + 2 step 3` (step is -3) cannot overflow (MIN_VALUE + 2 < MIN_VALUE - (-3))
        //   - `0..10 step someStep()` CAN overflow (we don't know the step and hence can't determine the safe limit)
        //   - `0..someLast()` CAN overflow (we don't know the direction)
        //   - `someProgression()` CAN overflow (we don't know the direction)
        val lastValueAsLong = last.constLongValue ?: return@lazy true  // If "last" is not a const Number or Char.
        when (direction) {
            ProgressionDirection.UNKNOWN ->
                // If we don't know the direction, we can't be sure which limit to use.
                true
            ProgressionDirection.DECREASING -> {
                val constLimitAsLong = when (progressionType) {
                    ProgressionType.INT_PROGRESSION -> Int.MIN_VALUE.toLong()
                    ProgressionType.CHAR_PROGRESSION -> Char.MIN_VALUE.toLong()
                    ProgressionType.LONG_PROGRESSION -> Long.MIN_VALUE
                }
                lastValueAsLong < (constLimitAsLong - stepValueAsLong)
            }
            ProgressionDirection.INCREASING -> {
                val constLimitAsLong = when (progressionType) {
                    ProgressionType.INT_PROGRESSION -> Int.MAX_VALUE.toLong()
                    ProgressionType.CHAR_PROGRESSION -> Char.MAX_VALUE.toLong()
                    ProgressionType.LONG_PROGRESSION -> Long.MAX_VALUE
                }
                lastValueAsLong > (constLimitAsLong - stepValueAsLong)
            }
        }
    }

    override fun asReversed() = ProgressionHeaderInfo(
        progressionType = progressionType,
        first = last,
        last = first,
        step = step.negate(),
        isReversed = !isReversed,
        direction = direction.asReversed(),
        additionalNotEmptyCondition = additionalNotEmptyCondition,
        additionalVariables = additionalVariables
    )
}

/**
 * Information about a for-loop over an object with an indexed get method (such as arrays or character sequences).
 * The internal induction variable used is an Int.
 */
internal class IndexedGetHeaderInfo(
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    canCacheLast: Boolean = true,
    val objectVariable: IrVariable,
    val expressionHandler: IndexedGetIterationHandler
) : NumericHeaderInfo(
    ProgressionType.INT_PROGRESSION,
    first,
    last,
    step,
    canCacheLast = canCacheLast,
    isReversed = false,
    direction = ProgressionDirection.INCREASING,
    additionalNotEmptyCondition = null
) {
    // Technically one can easily iterate over an array in reverse by swapping first/last and
    // negating the step. However, Array.reversed() and Array.reversedArray() return a collection
    // with a copy of the array elements, which means that the original array can be modified with
    // no effect on the iteration over the reversed array. That is:
    //
    //   val arr = intArrayOf(1, 2, 3, 4)
    //   for (i in arr.reversed()) {
    //     arr[0] = 0  // Does not affect iteration over reversed array
    //     print(i)    // Should print "4321"
    //   }
    //
    // If we simply iterated over `arr` in reverse, then we would get "4320" which is not the right
    // output. Hence we return null to indicate that we cannot loop over arrays in reverse.
    override fun asReversed(): HeaderInfo? = null
}

/**
 * Information about a for-loop over an iterable returned by `withIndex()`.
 */
internal class WithIndexHeaderInfo(val nestedInfo: HeaderInfo) : HeaderInfo() {
    // We cannot easily reverse `withIndex()` so we do not attempt to handle it. We would have to start from the last value of the index,
    // easily calculable (or even impossible) in most cases.
    override fun asReversed(): HeaderInfo? = null
}

/** Matches an iterable expression and builds a [HeaderInfo] from the expression. */
internal interface HeaderInfoHandler<E : IrExpression, D> {
    /** Returns true if the handler can build a [HeaderInfo] from the iterable expression. */
    fun matchIterable(expression: E): Boolean

    /**
     * Matches the `iterator()` call that produced the iterable; if the call matches (or the matcher is null),
     * the handler can build a [HeaderInfo] from the iterable.
     */
    val iteratorCallMatcher: IrCallMatcher?
        get() = null

    /** Builds a [HeaderInfo] from the expression. */
    fun build(expression: E, data: D, scopeOwner: IrSymbol): HeaderInfo?

    fun handle(expression: E, iteratorCall: IrCall?, data: D, scopeOwner: IrSymbol) =
        if ((iteratorCall == null || iteratorCallMatcher == null || iteratorCallMatcher!!(iteratorCall)) && matchIterable(expression)) {
            build(expression, data, scopeOwner)
        } else {
            null
        }
}

internal interface ExpressionHandler : HeaderInfoHandler<IrExpression, Nothing?> {
    fun build(expression: IrExpression, scopeOwner: IrSymbol): HeaderInfo?
    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol) = build(expression, scopeOwner)
}

/** Matches a call to build an iterable and builds a [HeaderInfo] from the call's context. */
internal interface HeaderInfoFromCallHandler<D> : HeaderInfoHandler<IrCall, D> {
    val matcher: IrCallMatcher

    override fun matchIterable(expression: IrCall) = matcher(expression)
}

internal typealias ProgressionHandler = HeaderInfoFromCallHandler<ProgressionType>

internal class HeaderInfoBuilder(context: CommonBackendContext, private val scopeOwnerSymbol: () -> IrSymbol) :
    IrElementVisitor<HeaderInfo?, IrCall?> {

    private val symbols = context.ir.symbols

    // TODO: Include unsigned types
    private val progressionElementTypes = listOf(
        context.irBuiltIns.byteType,
        context.irBuiltIns.shortType,
        context.irBuiltIns.intType,
        context.irBuiltIns.longType,
        context.irBuiltIns.charType
    )

    private val progressionHandlers = listOf(
        CollectionIndicesHandler(context),
        CharSequenceIndicesHandler(context),
        UntilHandler(context, progressionElementTypes),
        DownToHandler(context, progressionElementTypes),
        RangeToHandler(context, progressionElementTypes),
        StepHandler(context, this)
    )

    private val callHandlers = listOf(
        ReversedHandler(context, this),
        WithIndexHandler(context, this)
    )

    // NOTE: StringIterationHandler MUST come before CharSequenceIterationHandler.
    // String is subtype of CharSequence and therefore its handler is more specialized.
    private val expressionHandlers = listOf(
        ArrayIterationHandler(context),
        DefaultProgressionHandler(context),
        StringIterationHandler(context),
        CharSequenceIterationHandler(context)
    )

    override fun visitElement(element: IrElement, data: IrCall?): HeaderInfo? = null

    /** Builds a [HeaderInfo] for iterable expressions that are calls (e.g., `.reversed()`, `.indices`. */
    override fun visitCall(iterable: IrCall, iteratorCall: IrCall?): HeaderInfo? {
        // Return the HeaderInfo from the first successful match.
        // First, try to match a `reversed()` or `withIndex()` call.
        val callHeaderInfo = callHandlers.firstNotNullResult { it.handle(iterable, iteratorCall, null, scopeOwnerSymbol()) }
        if (callHeaderInfo != null)
            return callHeaderInfo

        // Try to match a call to build a progression (e.g., `.indices`, `downTo`).
        val progressionType = ProgressionType.fromIrType(iterable.type, symbols)
        val progressionHeaderInfo =
            progressionType?.run { progressionHandlers.firstNotNullResult { it.handle(iterable, iteratorCall, this, scopeOwnerSymbol()) } }

        return progressionHeaderInfo ?: super.visitCall(iterable, iteratorCall)
    }

    /** Builds a [HeaderInfo] for iterable expressions not handled in [visitCall]. */
    override fun visitExpression(iterable: IrExpression, iteratorCall: IrCall?): HeaderInfo? {
        return expressionHandlers.firstNotNullResult { it.handle(iterable, iteratorCall, null, scopeOwnerSymbol()) }
            ?: super.visitExpression(iterable, iteratorCall)
    }
}