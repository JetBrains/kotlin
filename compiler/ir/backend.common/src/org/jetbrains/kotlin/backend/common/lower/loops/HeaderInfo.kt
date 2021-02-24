/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.loops.handlers.*
import org.jetbrains.kotlin.backend.common.lower.matchers.IrCallMatcher
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

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

// TODO: Update comments and member names in this file.
internal class FloatingPointRangeHeaderInfo(
    val start: IrExpression,
    val endInclusive: IrExpression
) : HeaderInfo() {
    // No reverse() in ClosedFloatingPointRange.
    override fun asReversed(): HeaderInfo? = null
}

internal class ComparableRangeInfo(
    val start: IrExpression,
    val endInclusive: IrExpression
) : HeaderInfo() {
    override fun asReversed(): HeaderInfo? = null
}

internal sealed class NumericHeaderInfo(
    val progressionType: ProgressionType,
    val first: IrExpression,
    val last: IrExpression,
    val step: IrExpression,
    val isLastInclusive: Boolean,
    val canCacheLast: Boolean,
    val isReversed: Boolean,
    val direction: ProgressionDirection
) : HeaderInfo()

/** Information about a for-loop over a progression. */
internal class ProgressionHeaderInfo(
    progressionType: ProgressionType,
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    isLastInclusive: Boolean = true,
    isReversed: Boolean = false,
    canOverflow: Boolean? = null,
    direction: ProgressionDirection,
    val additionalStatements: List<IrStatement> = listOf()
) : NumericHeaderInfo(
    progressionType, first, last, step, isLastInclusive,
    canCacheLast = true,
    isReversed = isReversed,
    direction = direction
) {

    val canOverflow: Boolean by lazy {
        if (canOverflow != null) return@lazy canOverflow

        // We can't determine the safe limit at compile-time if "step" is not const.
        val stepValueAsLong = step.constLongValue ?: return@lazy true

        if (direction == ProgressionDirection.UNKNOWN) {
            // If we don't know the direction, we can't be sure which limit to use.
            return@lazy true
        }

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

        if (progressionType is UnsignedProgressionType) {
            // "step" is still signed for unsigned progressions.
            val lastValueAsULong = last.constLongValue?.toULong() ?: return@lazy true  // If "last" is not a const Number or Char.
            when (direction) {
                ProgressionDirection.DECREASING -> {
                    val constLimitAsULong = progressionType.minValueAsLong.toULong()
                    lastValueAsULong < (constLimitAsULong - stepValueAsLong.toULong())
                }
                ProgressionDirection.INCREASING -> {
                    val constLimitAsULong = progressionType.maxValueAsLong.toULong()
                    lastValueAsULong > (constLimitAsULong - stepValueAsLong.toULong())
                }
                else -> error("Unexpected progression direction")
            }
        } else {
            val lastValueAsLong = last.constLongValue ?: return@lazy true  // If "last" is not a const Number or Char.
            when (direction) {
                ProgressionDirection.DECREASING -> {
                    val constLimitAsLong = progressionType.minValueAsLong
                    lastValueAsLong < (constLimitAsLong - stepValueAsLong)
                }
                ProgressionDirection.INCREASING -> {
                    val constLimitAsLong = progressionType.maxValueAsLong
                    lastValueAsLong > (constLimitAsLong - stepValueAsLong)
                }
                else -> error("Unexpected progression direction")
            }
        }
    }

    override fun asReversed() = if (isLastInclusive) {
        ProgressionHeaderInfo(
            progressionType = progressionType,
            first = last,
            last = first,
            step = step.negate(),
            isReversed = !isReversed,
            direction = direction.asReversed(),
            additionalStatements = additionalStatements
        )
    } else {
        // If reversed, we would have a "first-exclusive" loop. We are currently not supporting this since it would add more complexity
        // due to possible overflow when pre-incrementing the loop variable (see KT-42533).
        null
    }
}

/**
 * Information about a for-loop over an object with an indexed get method (such as arrays or character sequences).
 * The internal induction variable used is an Int.
 */
internal class IndexedGetHeaderInfo(
    symbols: Symbols<CommonBackendContext>,
    first: IrExpression,
    last: IrExpression,
    step: IrExpression,
    canCacheLast: Boolean = true,
    val objectVariable: IrVariable,
    val expressionHandler: IndexedGetIterationHandler
) : NumericHeaderInfo(
    IntProgressionType(symbols), first, last, step,
    isLastInclusive = false,
    canCacheLast = canCacheLast,
    isReversed = false,
    direction = ProgressionDirection.INCREASING
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
    // which is not easily calculable (or even impossible) in most cases.
    override fun asReversed(): HeaderInfo? = null
}

/**
 * Information about a for-loop over an Iterable or Sequence.
 */
internal class IterableHeaderInfo(val iteratorVariable: IrVariable) : HeaderInfo() {
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

internal abstract class HeaderInfoBuilder(
    context: CommonBackendContext,
    private val scopeOwnerSymbol: () -> IrSymbol,
    private val allowUnsignedBounds: Boolean = false
) :
    IrElementVisitor<HeaderInfo?, IrCall?> {

    private val symbols = context.ir.symbols

    protected open val progressionHandlers = listOf(
        CollectionIndicesHandler(context),
        ArrayIndicesHandler(context),
        CharSequenceIndicesHandler(context),
        UntilHandler(context),
        DownToHandler(context),
        RangeToHandler(context),
        StepHandler(context, this)
    )

    protected abstract val callHandlers: List<HeaderInfoFromCallHandler<Nothing?>>
    protected abstract val expressionHandlers: List<ExpressionHandler>

    override fun visitElement(element: IrElement, data: IrCall?): HeaderInfo? = null

    /** Builds a [HeaderInfo] for iterable expressions that are calls (e.g., `.reversed()`, `.indices`. */
    override fun visitCall(expression: IrCall, data: IrCall?): HeaderInfo? {
        // Return the HeaderInfo from the first successful match.
        // First, try to match a `reversed()` or `withIndex()` call.
        val callHeaderInfo = callHandlers.firstNotNullResult { it.handle(expression, data, null, scopeOwnerSymbol()) }
        if (callHeaderInfo != null)
            return callHeaderInfo

        // Try to match a call to build a progression (e.g., `.indices`, `downTo`).
        val progressionType = ProgressionType.fromIrType(expression.type, symbols, allowUnsignedBounds)
        val progressionHeaderInfo =
            progressionType?.run { progressionHandlers.firstNotNullResult { it.handle(expression, data, this, scopeOwnerSymbol()) } }

        return progressionHeaderInfo ?: super.visitCall(expression, data)
    }

    /** Builds a [HeaderInfo] for iterable expressions not handled in [visitCall]. */
    override fun visitExpression(expression: IrExpression, data: IrCall?): HeaderInfo? {
        return expressionHandlers.firstNotNullResult { it.handle(expression, data, null, scopeOwnerSymbol()) }
            ?: super.visitExpression(expression, data)
    }
}

internal class DefaultHeaderInfoBuilder(context: CommonBackendContext, scopeOwnerSymbol: () -> IrSymbol) :
    HeaderInfoBuilder(context, scopeOwnerSymbol) {
    override val callHandlers = listOf(
        ReversedHandler(context, this),
        WithIndexHandler(
            context,
            NestedHeaderInfoBuilderForWithIndex(
                context,
                scopeOwnerSymbol
            )
        )
    )

    // NOTE: StringIterationHandler MUST come before CharSequenceIterationHandler.
    // String is subtype of CharSequence and therefore its handler is more specialized.
    override val expressionHandlers = listOf(
        ArrayIterationHandler(context),
        DefaultProgressionHandler(context),
        StringIterationHandler(context),
        CharSequenceIterationHandler(context)
    )
}

// WithIndexHandler attempts to retrieve the HeaderInfo from the underlying index, using NestedHeaderInfoBuilderForWithIndex instead of
// DefaultHeaderInfoBuilder. The differences between the two are that NestedHeaderInfoBuilderForWithIndex:
//
//   - Has NO WithIndexHandler. We do not attempt to optimize `*.withIndex().withIndex()`.
//   - Has Default(Iterable|Sequence)Handler. This allows us to optimize `Iterable<*>.withIndex()` and `Sequence<*>.withIndex()`.
internal class NestedHeaderInfoBuilderForWithIndex(context: CommonBackendContext, scopeOwnerSymbol: () -> IrSymbol) :
    HeaderInfoBuilder(context, scopeOwnerSymbol) {
    // NOTE: No WithIndexHandler; we cannot lower `iterable.withIndex().withIndex()`.
    override val callHandlers = listOf(
        ReversedHandler(context, this)
    )

    // NOTE: StringIterationHandler MUST come before CharSequenceIterationHandler.
    // String is subtype of CharSequence and therefore its handler is more specialized.
    // Default(Iterable|Sequence)Handler must come last as they handle iterables not handled by more specialized handlers.
    override val expressionHandlers = listOf(
        ArrayIterationHandler(context),
        DefaultProgressionHandler(context),
        StringIterationHandler(context),
        CharSequenceIterationHandler(context),
        DefaultIterableHandler(context),
        DefaultSequenceHandler(context),
    )
}

internal val Symbols<*>.progressionElementTypes: Collection<IrType>
    get() = listOfNotNull(byte, short, int, long, char, uByte, uShort, uInt, uLong).map { it.defaultType }
