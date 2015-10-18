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
 * A range of values of type `Byte`.
 */
public class ByteRange(override val start: Byte, override val endInclusive: Byte) : InclusiveRange<Byte>, InclusiveRangeProgression<Byte> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Byte get() = endInclusive

    override val increment: Int
        get() = 1

    override fun contains(item: Byte): Boolean = start <= item && item <= endInclusive

    override fun iterator(): ByteIterator = ByteProgressionIterator(start, endInclusive, 1)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is ByteRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Byte. */
        public val EMPTY: ByteRange = ByteRange(1, 0)
    }
}

/**
 * A range of values of type `Char`.
 */
public class CharRange(override val start: Char, override val endInclusive: Char) : InclusiveRange<Char>, InclusiveRangeProgression<Char> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Char get() = endInclusive

    override val increment: Int
        get() = 1

    override fun contains(item: Char): Boolean = start <= item && item <= endInclusive

    override fun iterator(): CharIterator = CharProgressionIterator(start, endInclusive, 1)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is CharRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Char. */
        public val EMPTY: CharRange = CharRange(1.toChar(), 0.toChar())
    }
}

/**
 * A range of values of type `Short`.
 */
public class ShortRange(override val start: Short, override val endInclusive: Short) : InclusiveRange<Short>, InclusiveRangeProgression<Short> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Short get() = endInclusive

    override val increment: Int
        get() = 1

    override fun contains(item: Short): Boolean = start <= item && item <= endInclusive

    override fun iterator(): ShortIterator = ShortProgressionIterator(start, endInclusive, 1)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is ShortRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + endInclusive.toInt())

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Short. */
        public val EMPTY: ShortRange = ShortRange(1, 0)
    }
}

/**
 * A range of values of type `Int`.
 */
public class IntRange(override val start: Int, override val endInclusive: Int) : InclusiveRange<Int>, InclusiveRangeProgression<Int> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Int get() = endInclusive

    override val increment: Int
        get() = 1

    override fun contains(item: Int): Boolean = start <= item && item <= endInclusive

    override fun iterator(): IntIterator = IntProgressionIterator(start, endInclusive, 1)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is IntRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start + endInclusive)

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Int. */
        public val EMPTY: IntRange = IntRange(1, 0)
    }
}

/**
 * A range of values of type `Long`.
 */
public class LongRange(override val start: Long, override val endInclusive: Long) : InclusiveRange<Long>, InclusiveRangeProgression<Long> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Long get() = endInclusive

    override val increment: Long
        get() = 1

    override fun contains(item: Long): Boolean = start <= item && item <= endInclusive

    override fun iterator(): LongIterator = LongProgressionIterator(start, endInclusive, 1)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is LongRange && (isEmpty() && other.isEmpty() ||
        start == other.start && endInclusive == other.endInclusive)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (start xor (start ushr 32)) + (endInclusive xor (endInclusive ushr 32))).toInt()

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Long. */
        public val EMPTY: LongRange = LongRange(1, 0)
    }
}

@Deprecated("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION_ERROR")
/**
 * A range of values of type `Float`.
 */
public class FloatRange(override val start: Float, override val endInclusive: Float) : InclusiveRange<Float>, InclusiveRangeProgression<Float> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Float get() = endInclusive

    override val increment: Float
        get() = 1.0f

    override fun contains(item: Float): Boolean = start <= item && item <= endInclusive

    override fun iterator(): FloatIterator = FloatProgressionIterator(start, endInclusive, 1.0f)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is FloatRange && (isEmpty() && other.isEmpty() ||
        java.lang.Float.compare(start, other.start) == 0 && java.lang.Float.compare(endInclusive, other.endInclusive) == 0)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * java.lang.Float.floatToIntBits(start) + java.lang.Float.floatToIntBits(endInclusive))

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Float. */
        public val EMPTY: FloatRange = FloatRange(1.0f, 0.0f)
    }
}

@Deprecated("This range implementation has unclear semantics and will be removed soon.", level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION_ERROR")
/**
 * A range of values of type `Double`.
 */
public class DoubleRange(override val start: Double, override val endInclusive: Double) : InclusiveRange<Double>, InclusiveRangeProgression<Double> {
    @Deprecated("Use endInclusive instead.", ReplaceWith("endInclusive"))
    override val end: Double get() = endInclusive

    override val increment: Double
        get() = 1.0

    override fun contains(item: Double): Boolean = start <= item && item <= endInclusive

    override fun iterator(): DoubleIterator = DoubleProgressionIterator(start, endInclusive, 1.0)

    override fun isEmpty(): Boolean = start > endInclusive

    override fun equals(other: Any?): Boolean =
        other is DoubleRange && (isEmpty() && other.isEmpty() ||
        java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(endInclusive, other.endInclusive) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        val result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(endInclusive)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    override fun toString(): String = "$start..$endInclusive"

    companion object {
        /** An empty range of values of type Double. */
        public val EMPTY: DoubleRange = DoubleRange(1.0, 0.0)
    }
}

