/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin

/**
 * A progression of values of type `Byte`.
 */
public class ByteProgression(
        override val start: Byte,
        override val end: Byte,
        override val increment: Int
) : Progression<Byte> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ByteIterator = ByteProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is ByteProgression && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * start.toInt() + end) + increment)

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Char`.
 */
public class CharProgression(
        override val start: Char,
        override val end: Char,
        override val increment: Int
) : Progression<Char> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): CharIterator = CharProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is CharProgression && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * start.toInt() + end) + increment)

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Short`.
 */
public class ShortProgression(
        override val start: Short,
        override val end: Short,
        override val increment: Int
) : Progression<Short> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ShortIterator = ShortProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is ShortProgression && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * start.toInt() + end) + increment)

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Int`.
 */
public class IntProgression(
        override val start: Int,
        override val end: Int,
        override val increment: Int
) : Progression<Int> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): IntIterator = IntProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is IntProgression && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * start + end) + increment)

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Long`.
 */
public class LongProgression(
        override val start: Long,
        override val end: Long,
        override val increment: Long
) : Progression<Long> {
    {
        if (increment == 0L) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): LongIterator = LongProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is LongProgression && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * (start xor (start ushr 32)) + (end xor (end ushr 32))) + (increment xor (increment ushr 32))).toInt()

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Float`.
 */
public class FloatProgression(
        override val start: Float,
        override val end: Float,
        override val increment: Float
) : Progression<Float> {
    {
        if (java.lang.Float.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0f) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): FloatIterator = FloatProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is FloatProgression && (isEmpty() && other.isEmpty() ||
        java.lang.Float.compare(start, other.start) == 0 && java.lang.Float.compare(end, other.end) == 0 && java.lang.Float.compare(increment, other.increment) == 0)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * java.lang.Float.floatToIntBits(start) + java.lang.Float.floatToIntBits(end)) + java.lang.Float.floatToIntBits(increment))

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

/**
 * A progression of values of type `Double`.
 */
public class DoubleProgression(
        override val start: Double,
        override val end: Double,
        override val increment: Double
) : Progression<Double> {
    {
        if (java.lang.Double.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): DoubleIterator = DoubleProgressionIterator(start, end, increment)

    /** Checks if the progression is empty. */
    public fun isEmpty(): Boolean = if (increment > 0) start > end else start < end

    override fun equals(other: Any?): Boolean =
        other is DoubleProgression && (isEmpty() && other.isEmpty() ||
        java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0 && java.lang.Double.compare(increment, other.increment) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        var result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        result = 31 * result + (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(increment)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

