/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin.ranges

import kotlin.internal.getProgressionLastElement

/**
 * A progression of values of type `Char`.
 */
public open class CharProgression
    internal constructor
    (
            start: Char,
            endInclusive: Char,
            step: Int
    ) : Collection<Char> {
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

    private infix fun Char.mod(n: Int): Int {
        val positiveN = kotlin.math.abs(n)
        val r = (this - Char.MIN_VALUE) % positiveN
        return if (r < 0) r + positiveN else r
    }

    @SinceKotlin("1.6")
    override fun contains(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") /* for the backward compatibility with old names */ value: Char): Boolean = when {
        step > 0 && value >= first && value <= last -> value mod step == first mod step
        step < 0 && value <= first && value >= last -> value mod step == first mod step
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Char>): Boolean = if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in this }

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
    ) : Collection<Int> {
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
        get() = if (isEmpty()) 0 else when {
            step == 1 ->
                if (first >= 0 || last < 0 || last <= Int.MAX_VALUE + first)
                    (last - first).let { if (it < Int.MAX_VALUE) it.inc() else Int.MAX_VALUE }
                else Int.MAX_VALUE
            step == -1 ->
                if (last >= 0 || first < 0 || first <= Int.MAX_VALUE + last)
                    (first - last).let { if (it < Int.MAX_VALUE) it.inc() else Int.MAX_VALUE }
                else Int.MAX_VALUE
            step > Int.MIN_VALUE / 2 && step < Int.MAX_VALUE / 2 -> {
                //(last - first) / step =
                // = (last / step * step + last % step - first / step * step - first % step) / step =
                // = last / step - first / step + (last % step - first % step) / step

                //no overflow because |step| >= 2
                //Int.MIN_VALUE / 2 <= last / step <= Int.MAX_VALUE / 2
                //Int.MIN_VALUE / 2 <= first / step <= Int.MAX_VALUE / 2
                //Int.MIN_VALUE / 2 - Int.MAX_VALUE / 2 <= last / step - first / step <= Int.MAX_VALUE / 2 - Int.MIN_VALUE / 2
                //Int.MIN_VALUE + 1 <= last / step - first / step <= Int.MAX_VALUE
                val div = last / step - first / step // >= 0 because either step > 0 && last >= first or step < 0 && last <= first
                //no overflow because Int.MIN_VALUE / 2 < step < Int.MAX_VALUE / 2
                //min(Int.MIN_VALUE / 2, -(Int.MAX_VALUE / 2)) < first % step < max(Int.MAX_VALUE / 2, -(Int.MIN_VALUE / 2))
                //Int.MIN_VALUE / 2 < first % step <= Int.MAX_VALUE / 2
                //Int.MIN_VALUE / 2 <= first % step <= Int.MAX_VALUE / 2
                //Int.MIN_VALUE / 2 <= last % step <= Int.MAX_VALUE / 2
                //Int.MIN_VALUE <= last % step - first % step <= Int.MAX_VALUE
                val rem = (last % step - first % step) / step
                if (div <= Int.MAX_VALUE - rem)
                    (div + rem).let { if (it < Int.MAX_VALUE) it.inc() else Int.MAX_VALUE }
                else
                    Int.MAX_VALUE
            }
            else -> {
                //number of items is < 5 (the smallest (by its absolute value) step is Int.MAX_VALUE / 2, so if progression starts at Int.MIN_VALUE, it contains 4 elements
                var count = 0
                for (item in this) count++
                count
                //count() is not used as it may recursively use size property for Collections
            }
        }

    private infix fun Int.mod(n: Int): Int {
        val positiveN = kotlin.math.abs(n)
        val r = this % positiveN
        return if (r < 0) r + positiveN else r
    }

    @SinceKotlin("1.6")
    override fun contains(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") /* for the backward compatibility with old names */ value: Int): Boolean = when {
        step > 0 && value >= first && value <= last -> value mod step == first mod step
        step < 0 && value <= first && value >= last -> value mod step == first mod step
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Int>): Boolean = if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in this }

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
    ) : Collection<Long> {
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
        get() = if (isEmpty()) 0 else when {
            step == 1L ->
                if (first >= 0L || last < 0L || last <= Long.MAX_VALUE + first)
                    (last - first).let { if (it < Int.MAX_VALUE.toLong()) it.toInt().inc() else Int.MAX_VALUE }
                else Int.MAX_VALUE
            step == -1L ->
                if (last >= 0L || first < 0L || first <= Long.MAX_VALUE + last)
                    (first - last).let { if (it < Int.MAX_VALUE.toLong()) it.toInt().inc() else Int.MAX_VALUE }
                else Int.MAX_VALUE
            step > Long.MIN_VALUE / 2L && step < Long.MAX_VALUE / 2L -> {
                //(last - first) / step =
                // = (last / step * step + last % step - first / step * step - first % step) / step =
                // = last / step - first / step + (last % step - first % step) / step

                //no overflow because |step| >= 2
                //Long.MIN_VALUE / 2 <= last / step <= Long.MAX_VALUE / 2
                //Long.MIN_VALUE / 2 <= first / step <= Long.MAX_VALUE / 2
                //Long.MIN_VALUE / 2 - Long.MAX_VALUE / 2 <= last / step - first / step <= Long.MAX_VALUE / 2 - Long.MIN_VALUE / 2
                //Long.MIN_VALUE + 1L <= last / step - first / step <= Long.MAX_VALUE
                val div = last / step - first / step // >= 0 because either step > 0 && last >= first or step < 0 && last <= first
                //no overflow because Long.MIN_VALUE / 2 < step < Long.MAX_VALUE / 2
                //min(Long.MIN_VALUE / 2, -(Long.MAX_VALUE / 2)) < first % step < max(Long.MAX_VALUE / 2, -(Long.MIN_VALUE / 2))
                //Long.MIN_VALUE / 2 < first % step <= Long.MAX_VALUE / 2
                //Long.MIN_VALUE / 2 <= first % step <= Long.MAX_VALUE / 2
                //Long.MIN_VALUE / 2 <= last % step <= Long.MAX_VALUE / 2
                //Long.MIN_VALUE <= last % step - first % step <= Long.MAX_VALUE
                val rem = (last % step - first % step) / step
                if (div <= Long.MAX_VALUE - rem)
                    (div + rem).let { if (it < Int.MAX_VALUE.toLong()) it.toInt().inc() else Int.MAX_VALUE }
                else
                    Int.MAX_VALUE
            }
            else -> {
                //number of items is < 5 (the smallest (by its absolute value) step is Long.MAX_VALUE / 2, so if progression starts at Long.MIN_VALUE, it contains 4 elements
                var count = 0
                for (item in this) count++
                count
                //count() is not used as it may recursively use size property for Collections
            }
        }

    private infix fun Long.mod(n: Long): Long {
        val positiveN = kotlin.math.abs(n)
        val r = this % positiveN
        return if (r < 0L) r + positiveN else r
    }

    @SinceKotlin("1.6")
    override fun contains(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") /* for the backward compatibility with old names */ value: Long): Boolean = when {
        step > 0L && value >= first && value <= last -> value mod step == first mod step
        step < 0L && value <= first && value >= last -> value mod step == first mod step
        else -> false
    }

    @SinceKotlin("1.6")
    override fun containsAll(elements: Collection<Long>): Boolean = if (this.isEmpty()) elements.isEmpty() else (elements as Collection<*>).all { it in this }

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

