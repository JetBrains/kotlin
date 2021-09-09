/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // preserve parameter name of 'contains' override
package kotlin.ranges

import kotlin.internal.getProgressionLastElement
import kotlin.internal.unsignedIncrementAndClamp
import kotlin.internal.progressionUnsignedDivide

/**
 * A progression of values of type `Char`.
 */
public open class CharProgression
    internal constructor
    (
            start: Char,
            endInclusive: Char,
            step: Int
    ) : Collection<Char>, kotlin.internal.ProgressionCollection {
    init {
        if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == Int.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Int.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: Char = start

    /**
     * The last element in the progression.
     */
    public val last: Char = getProgressionLastElement(start.code, endInclusive.code, step).toChar()

    /**
     * The step of the progression.
     */
    public val step: Int = step

    override fun iterator(): CharIterator = CharProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.
     *
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public override fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is CharProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.code + last.code) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    @SinceKotlin("1.6")
    override val size: Int
        get() = if (isEmpty()) 0 else (last - first) / step + 1

    @SinceKotlin("1.6")
    override fun contains(value: Char): Boolean = when {
        @Suppress("USELESS_CAST") (value as Any? !is Char) -> false // TODO: Eliminate this check after KT-30016 gets fixed.
        step > 0 && value >= first && value <= last ||
        step < 0 && value <= first && value >= last -> value.code.mod(step) == first.code.mod(step)
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Char>): Boolean =
        if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in (this as Collection<Any?>) }

    companion object {
        /**
         * Creates CharProgression within the specified bounds of a closed range.
         *
         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Int.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: Char, rangeEnd: Char, step: Int): CharProgression = CharProgression(rangeStart, rangeEnd, step)
    }
}

/**
 * A progression of values of type `Int`.
 */
public open class IntProgression
    internal constructor
    (
            start: Int,
            endInclusive: Int,
            step: Int
    ) : Collection<Int>, kotlin.internal.ProgressionCollection {
    init {
        if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == Int.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Int.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: Int = start

    /**
     * The last element in the progression.
     */
    public val last: Int = getProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    public val step: Int = step

    override fun iterator(): IntIterator = IntProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.
     *
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public override fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is IntProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first + last) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    @SinceKotlin("1.6")
    override val size: Int
        get() = when {
            isEmpty() -> 0
            step > 0 -> unsignedIncrementAndClamp(progressionUnsignedDivide(last - first, step))
            step < 0 -> unsignedIncrementAndClamp(progressionUnsignedDivide(first - last, -step))
            else -> error("Progression invariant is broken: step == 0")
        }

    @SinceKotlin("1.6")
    override fun contains(value: Int): Boolean = when {
        @Suppress("USELESS_CAST") (value as Any? !is Int) -> false // TODO: Eliminate this check after KT-30016 gets fixed.
        step > 0 && value >= first && value <= last ||
        step < 0 && value <= first && value >= last -> value.mod(step) == first.mod(step)
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Int>): Boolean =
        if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in (this as Collection<Any?>) }

    companion object {
        /**
         * Creates IntProgression within the specified bounds of a closed range.
         *
         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Int.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: Int, rangeEnd: Int, step: Int): IntProgression = IntProgression(rangeStart, rangeEnd, step)
    }
}

/**
 * A progression of values of type `Long`.
 */
public open class LongProgression
    internal constructor
    (
            start: Long,
            endInclusive: Long,
            step: Long
    ) : Collection<Long>, kotlin.internal.ProgressionCollection {
    init {
        if (step == 0L) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == Long.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Long.MIN_VALUE to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: Long = start

    /**
     * The last element in the progression.
     */
    public val last: Long = getProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    public val step: Long = step

    override fun iterator(): LongIterator = LongProgressionIterator(first, last, step)

    /**
     * Checks if the progression is empty.
     *
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public override fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is LongProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * (first xor (first ushr 32)) + (last xor (last ushr 32))) + (step xor (step ushr 32))).toInt()

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    @SinceKotlin("1.6")
    override val size: Int
        get() = when {
            isEmpty() -> 0
            step > 0 -> unsignedIncrementAndClamp(progressionUnsignedDivide(last - first, step))
            step < 0 -> unsignedIncrementAndClamp(progressionUnsignedDivide(first - last, -step))
            else -> error("Progression invariant is broken: step == 0")
        }

    @SinceKotlin("1.6")
    override fun contains(value: Long): Boolean = when {
        @Suppress("USELESS_CAST") (value as Any? !is Long) -> false // TODO: Eliminate this check after KT-30016 gets fixed.
        step > 0L && value >= first && value <= last ||
        step < 0L && value <= first && value >= last -> value.mod(step) == first.mod(step)
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Long>): Boolean =
        if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in (this as Collection<Any?>) }

    companion object {
        /**
         * Creates LongProgression within the specified bounds of a closed range.
         *
         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `Long.MIN_VALUE` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: Long, rangeEnd: Long, step: Long): LongProgression = LongProgression(rangeStart, rangeEnd, step)
    }
}

