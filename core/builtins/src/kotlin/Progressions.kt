/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    ) : Iterable<Char> {
    init {
        if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Char = start

    /**
     * The last element in the progression.
     */
    public val last: Char = getProgressionLastElement(start.toInt(), endInclusive.toInt(), step).toChar()

    /**
     * The step of the progression.
     */
    public val step: Int = step

    override fun iterator(): CharIterator = CharProgressionIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is CharProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    companion object {
        /**
         * Creates CharProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
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
    ) : Iterable<Int> {
    init {
        if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Int = start

    /**
     * The last element in the progression.
     */
    public val last: Int = getProgressionLastElement(start.toInt(), endInclusive.toInt(), step).toInt()

    /**
     * The step of the progression.
     */
    public val step: Int = step

    override fun iterator(): IntIterator = IntProgressionIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is IntProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first + last) + step)

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    companion object {
        /**
         * Creates IntProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
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
    ) : Iterable<Long> {
    init {
        if (step == 0L) throw kotlin.IllegalArgumentException("Step must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Long = start

    /**
     * The last element in the progression.
     */
    public val last: Long = getProgressionLastElement(start.toLong(), endInclusive.toLong(), step).toLong()

    /**
     * The step of the progression.
     */
    public val step: Long = step

    override fun iterator(): LongIterator = LongProgressionIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is LongProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * (first xor (first ushr 32)) + (last xor (last ushr 32))) + (step xor (step ushr 32))).toInt()

    override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

    companion object {
        /**
         * Creates LongProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Long, rangeEnd: Long, step: Long): LongProgression = LongProgression(rangeStart, rangeEnd, step)
    }
}

