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
internal sealed class HeaderInfo(
    val progressionType: ProgressionType,
    val first: IrExpression,
    val last: IrExpression,
    val step: IrExpression,
    val isReversed: Boolean,
    val direction: ProgressionDirection,
    val additionalNotEmptyCondition: IrExpression?
) {
    /**
     * Returns a copy of this [HeaderInfo] with the values reversed.
     * I.e., first and last are swapped, step is negated.
     * Returns null if the iterable cannot be iterated in reverse.
     */
    abstract fun asReversed(): HeaderInfo?
}

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
) : HeaderInfo(progressionType, first, last, step, isReversed, direction, additionalNotEmptyCondition) {

    val canOverflow: Boolean by lazy {
        if (canOverflow != null) return@lazy canOverflow

        // Induction variable can overflow if it is not a const, or is MAX/MIN_VALUE (depending on direction).
        val lastValueAsLong = last.constLongValue ?: return@lazy true  // If "last" is not a const Number or Char.
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
    val objectVariable: IrVariable
) : HeaderInfo(
    ProgressionType.INT_PROGRESSION,
    first,
    last,
    step,
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

/** Matches an iterable expression and builds a [HeaderInfo] from the expression. */
internal interface HeaderInfoHandler<E : IrExpression, D> {
    /** Returns true if the handler can build a [HeaderInfo] from the expression. */
    fun match(expression: E): Boolean

    /** Builds a [HeaderInfo] from the expression. */
    fun build(expression: E, data: D, scopeOwner: IrSymbol): HeaderInfo?

    fun handle(expression: E, data: D, scopeOwner: IrSymbol) = if (match(expression)) {
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

    override fun match(expression: IrCall) = matcher(expression)
}

internal typealias ProgressionHandler = HeaderInfoFromCallHandler<ProgressionType>

internal class HeaderInfoBuilder(context: CommonBackendContext, private val scopeOwnerSymbol: () -> IrSymbol) :
    IrElementVisitor<HeaderInfo?, Nothing?> {

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
        IndicesHandler(context),
        UntilHandler(context, progressionElementTypes),
        DownToHandler(context, progressionElementTypes),
        RangeToHandler(context, progressionElementTypes)
    )

    private val reversedHandler = ReversedHandler(context, this)

    private val expressionHandlers = listOf(
        ArrayIterationHandler(context),
        DefaultProgressionHandler(context),
        CharSequenceIterationHandler(context)
    )

    override fun visitElement(element: IrElement, data: Nothing?): HeaderInfo? = null

    /** Builds a [HeaderInfo] for iterable expressions that are calls (e.g., `.reversed()`, `.indices`. */
    override fun visitCall(expression: IrCall, data: Nothing?): HeaderInfo? {
        // Return the HeaderInfo from the first successful match. First, try to match a `reversed()` call.
        val reversedHeaderInfo = reversedHandler.handle(expression, null, scopeOwnerSymbol())
        if (reversedHeaderInfo != null)
            return reversedHeaderInfo

        // Try to match a call to build a progression (e.g., `.indices`, `downTo`).
        val progressionType = ProgressionType.fromIrType(expression.type, symbols)
        val progressionHeaderInfo =
            progressionType?.run { progressionHandlers.firstNotNullResult { it.handle(expression, this, scopeOwnerSymbol()) } }

        return progressionHeaderInfo ?: super.visitCall(expression, data)
    }

    /** Builds a [HeaderInfo] for iterable expressions not handled in [visitCall]. */
    override fun visitExpression(expression: IrExpression, data: Nothing?): HeaderInfo? {
        return expressionHandlers.firstNotNullResult { it.handle(expression, null, scopeOwnerSymbol()) }
            ?: super.visitExpression(expression, data)
    }
}