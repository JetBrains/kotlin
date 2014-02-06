/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package jet

public class ByteProgression(
        public override val start: Byte,
        public override val end: Byte,
        public override val increment: Int
) : Progression<Byte> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ByteIterator = ByteProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is ByteProgression && start == other.start && end == other.end && increment == other.increment

    override fun hashCode(): Int = 31 * (31 * start.toInt() + end) + increment

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class CharProgression(
        public override val start: Char,
        public override val end: Char,
        public override val increment: Int
) : Progression<Char> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): CharIterator = CharProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is CharProgression && start == other.start && end == other.end && increment == other.increment

    override fun hashCode(): Int = 31 * (31 * start.toInt() + end) + increment

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class ShortProgression(
        public override val start: Short,
        public override val end: Short,
        public override val increment: Int
) : Progression<Short> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): ShortIterator = ShortProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is ShortProgression && start == other.start && end == other.end && increment == other.increment

    override fun hashCode(): Int = 31 * (31 * start.toInt() + end) + increment

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class IntProgression(
        public override val start: Int,
        public override val end: Int,
        public override val increment: Int
) : Progression<Int> {
    {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): IntIterator = IntProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is IntProgression && start == other.start && end == other.end && increment == other.increment

    override fun hashCode(): Int = 31 * (31 * start + end) + increment

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class LongProgression(
        public override val start: Long,
        public override val end: Long,
        public override val increment: Long
) : Progression<Long> {
    {
        if (increment == 0L) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): LongIterator = LongProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is LongProgression && start == other.start && end == other.end && increment == other.increment

    override fun hashCode(): Int = (31 * (31 * (start xor (start ushr 32)) + (end xor (end ushr 32))) + (increment xor (increment ushr 32))).toInt()

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class FloatProgression(
        public override val start: Float,
        public override val end: Float,
        public override val increment: Float
) : Progression<Float> {
    {
        if (java.lang.Float.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0f) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): FloatIterator = FloatProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is FloatProgression && java.lang.Float.compare(start, other.start) == 0 && java.lang.Float.compare(end, other.end) == 0 && java.lang.Float.compare(increment, other.increment) == 0

    override fun hashCode(): Int = 31 * (31 * java.lang.Float.floatToIntBits(start) + java.lang.Float.floatToIntBits(end)) + java.lang.Float.floatToIntBits(increment)

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

public class DoubleProgression(
        public override val start: Double,
        public override val end: Double,
        public override val increment: Double
) : Progression<Double> {
    {
        if (java.lang.Double.isNaN(increment)) throw IllegalArgumentException("Increment must be not NaN")
        if (increment == 0.0) throw IllegalArgumentException("Increment must be non-zero")
    }

    override fun iterator(): DoubleIterator = DoubleProgressionIterator(start, end, increment)

    override fun equals(other: Any?): Boolean =
        other is DoubleProgression && java.lang.Double.compare(start, other.start) == 0 && java.lang.Double.compare(end, other.end) == 0 && java.lang.Double.compare(increment, other.increment) == 0

    override fun hashCode(): Int {
        var temp = java.lang.Double.doubleToLongBits(start)
        var result = (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(end)
        result = 31 * result + (temp xor (temp ushr 32))
        temp = java.lang.Double.doubleToLongBits(increment)
        return (31 * result + (temp xor (temp ushr 32))).toInt()
    }

    override fun toString(): String = if (increment > 0) "$start..$end step $increment" else "$start downTo $end step ${-increment}"
}

