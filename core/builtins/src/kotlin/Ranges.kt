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
 * A range of values of type Byte.
 */
public class ByteRange(override val start: Byte, override val end: Byte) : Range<Byte>, Progression<Byte> {
    override val increment: Int
        get() = 1

    override fun contains(item: Byte): Boolean = start <= item && item <= end

    override fun iterator(): ByteIterator = ByteProgressionIterator(start, end, 1)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is ByteRange && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + end)

    default object {
        /** An empty range of values of type Byte. */
        public val EMPTY: ByteRange = ByteRange(1, 0)
    }
}

/**
 * A range of values of type Char.
 */
public class CharRange(override val start: Char, override val end: Char) : Range<Char>, Progression<Char> {
    override val increment: Int
        get() = 1

    override fun contains(item: Char): Boolean = start <= item && item <= end

    override fun iterator(): CharIterator = CharProgressionIterator(start, end, 1)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is CharRange && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + end)

    default object {
        /** An empty range of values of type Char. */
        public val EMPTY: CharRange = CharRange(1.toChar(), 0.toChar())
    }
}

/**
 * A range of values of type Short.
 */
public class ShortRange(override val start: Short, override val end: Short) : Range<Short>, Progression<Short> {
    override val increment: Int
        get() = 1

    override fun contains(item: Short): Boolean = start <= item && item <= end

    override fun iterator(): ShortIterator = ShortProgressionIterator(start, end, 1)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is ShortRange && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start.toInt() + end)

    default object {
        /** An empty range of values of type Short. */
        public val EMPTY: ShortRange = ShortRange(1, 0)
    }
}

/**
 * A range of values of type Int.
 */
public class IntRange(override val start: Int, override val end: Int) : Range<Int>, Progression<Int> {
    override val increment: Int
        get() = 1

    override fun contains(item: Int): Boolean = start <= item && item <= end

    override fun iterator(): IntIterator = IntProgressionIterator(start, end, 1)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is IntRange && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * start + end)

    default object {
        /** An empty range of values of type Int. */
        public val EMPTY: IntRange = IntRange(1, 0)
    }
}

/**
 * A range of values of type Long.
 */
public class LongRange(override val start: Long, override val end: Long) : Range<Long>, Progression<Long> {
    override val increment: Long
        get() = 1

    override fun contains(item: Long): Boolean = start <= item && item <= end

    override fun iterator(): LongIterator = LongProgressionIterator(start, end, 1)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is LongRange && (isEmpty() && other.isEmpty() ||
        start == other.start && end == other.end)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (start xor (start ushr 32)) + (end xor (end ushr 32))).toInt()

    default object {
        /** An empty range of values of type Long. */
        public val EMPTY: LongRange = LongRange(1, 0)
    }
}

/**
 * A range of values of type Float.
 */
public class FloatRange(override val start: Float, override val end: Float) : Range<Float>, Progression<Float> {
    override val increment: Float
        get() = 1.0f

    override fun contains(item: Float): Boolean = start <= item && item <= end

    override fun iterator(): FloatIterator = FloatProgressionIterator(start, end, 1.0f)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is FloatRange && (isEmpty() && other.isEmpty() ||
        java.lang.Float.compare(start, other.start) == 0 && java.lang.Float.compare(end, other.end) == 0)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * java.lang.Float.floatToIntBits(start) + java.lang.Float.floatToIntBits(end))

    default object {
        /** An empty range of values of type Float. */
        public val EMPTY: FloatRange = FloatRange(1.0f, 0.0f)
    }
}

/**
 * A range of values of type Double.
 */
public class DoubleRange(override val start: Double, override val end: Double) : Range<Double>, Progression<Double> {
    override val increment: Double
        get() = 1.0

    override fun contains(item: Double): Boolean = start <= item && item <= end

    override fun iterator(): DoubleIterator = DoubleProgressionIterator(start, end, 1.0)

    override fun isEmpty(): Boolean = start > end

    override fun equals(other: Any?): Boolean =
        other is DoubleRange && (isEmpty() && other.isEmpty() ||
        java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0)

    override fun hashCode(): Int {
        if (isEmpty()) return -1
        var temp = java.lang.Double.doubleToLongBits(start)
        val result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    default object {
        /** An empty range of values of type Double. */
        public val EMPTY: DoubleRange = DoubleRange(1.0, 0.0)
    }
}

