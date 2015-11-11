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

@Deprecated("Use IntProgression instead.", ReplaceWith("IntProgression"), level = DeprecationLevel.WARNING)
/**
 * A progression of values of type `Byte`.
 */
public open class ByteProgression
    @Deprecated("This constructor will become private soon. Use ByteProgression.fromClosedRange() instead.", ReplaceWith("ByteProgression.fromClosedRange(start, end, increment)"))
    public constructor
    (
            start: Byte,
            endInclusive: Byte,
            override val increment: Int
    ) : Progression<Byte> /*, Iterable<Byte> */ {
    init {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Byte = start
    /**
     * The last element in the progression.
     */
    public val last: Byte = kotlin.internal.getProgressionFinalElement(start.toInt(), endInclusive.toInt(), increment).toByte()

    @Deprecated("Use first instead.", ReplaceWith("first"))
    public override val start: Byte get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: Byte = endInclusive

    override fun iterator(): ByteIterator = ByteProgressionIterator(first, last, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is ByteProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + increment)

    override fun toString(): String = if (increment > 0) "$first..$last step $increment" else "$first downTo $last step ${-increment}"

    companion object {
        /**
         * Creates ByteProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Byte, rangeEnd: Byte, step: Int): ByteProgression = ByteProgression(rangeStart, rangeEnd, step)
    }
}

/**
 * A progression of values of type `Char`.
 */
public open class CharProgression
    @Deprecated("This constructor will become private soon. Use CharProgression.fromClosedRange() instead.", ReplaceWith("CharProgression.fromClosedRange(start, end, increment)"))
    public constructor
    (
            start: Char,
            endInclusive: Char,
            override val increment: Int
    ) : Progression<Char> /*, Iterable<Char> */ {
    init {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Char = start
    /**
     * The last element in the progression.
     */
    public val last: Char = kotlin.internal.getProgressionFinalElement(start.toInt(), endInclusive.toInt(), increment).toChar()

    @Deprecated("Use first instead.", ReplaceWith("first"))
    public override val start: Char get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: Char = endInclusive

    override fun iterator(): CharIterator = CharProgressionIterator(first, last, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is CharProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + increment)

    override fun toString(): String = if (increment > 0) "$first..$last step $increment" else "$first downTo $last step ${-increment}"

    companion object {
        /**
         * Creates CharProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Char, rangeEnd: Char, step: Int): CharProgression = CharProgression(rangeStart, rangeEnd, step)
    }
}

@Deprecated("Use IntProgression instead.", ReplaceWith("IntProgression"), level = DeprecationLevel.WARNING)
/**
 * A progression of values of type `Short`.
 */
public open class ShortProgression
    @Deprecated("This constructor will become private soon. Use ShortProgression.fromClosedRange() instead.", ReplaceWith("ShortProgression.fromClosedRange(start, end, increment)"))
    public constructor
    (
            start: Short,
            endInclusive: Short,
            override val increment: Int
    ) : Progression<Short> /*, Iterable<Short> */ {
    init {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Short = start
    /**
     * The last element in the progression.
     */
    public val last: Short = kotlin.internal.getProgressionFinalElement(start.toInt(), endInclusive.toInt(), increment).toShort()

    @Deprecated("Use first instead.", ReplaceWith("first"))
    public override val start: Short get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: Short = endInclusive

    override fun iterator(): ShortIterator = ShortProgressionIterator(first, last, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is ShortProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + increment)

    override fun toString(): String = if (increment > 0) "$first..$last step $increment" else "$first downTo $last step ${-increment}"

    companion object {
        /**
         * Creates ShortProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Short, rangeEnd: Short, step: Int): ShortProgression = ShortProgression(rangeStart, rangeEnd, step)
    }
}

/**
 * A progression of values of type `Int`.
 */
public open class IntProgression
    @Deprecated("This constructor will become private soon. Use IntProgression.fromClosedRange() instead.", ReplaceWith("IntProgression.fromClosedRange(start, end, increment)"))
    public constructor
    (
            start: Int,
            endInclusive: Int,
            override val increment: Int
    ) : Progression<Int> /*, Iterable<Int> */ {
    init {
        if (increment == 0) throw IllegalArgumentException("Increment must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Int = start
    /**
     * The last element in the progression.
     */
    public val last: Int = kotlin.internal.getProgressionFinalElement(start.toInt(), endInclusive.toInt(), increment).toInt()

    @Deprecated("Use first instead.", ReplaceWith("first"))
    public override val start: Int get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: Int = endInclusive

    override fun iterator(): IntIterator = IntProgressionIterator(first, last, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is IntProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first + last) + increment)

    override fun toString(): String = if (increment > 0) "$first..$last step $increment" else "$first downTo $last step ${-increment}"

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
    @Deprecated("This constructor will become private soon. Use LongProgression.fromClosedRange() instead.", ReplaceWith("LongProgression.fromClosedRange(start, end, increment)"))
    public constructor
    (
            start: Long,
            endInclusive: Long,
            override val increment: Long
    ) : Progression<Long> /*, Iterable<Long> */ {
    init {
        if (increment == 0L) throw IllegalArgumentException("Increment must be non-zero")
    }

    /**
     * The first element in the progression.
     */
    public val first: Long = start
    /**
     * The last element in the progression.
     */
    public val last: Long = kotlin.internal.getProgressionFinalElement(start.toLong(), endInclusive.toLong(), increment).toLong()

    @Deprecated("Use first instead.", ReplaceWith("first"))
    public override val start: Long get() = first

    /**
     * The end value of the progression (inclusive).
     */
    @Deprecated("Use 'last' property instead.")
    public override val end: Long = endInclusive

    override fun iterator(): LongIterator = LongProgressionIterator(first, last, increment)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (increment > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is LongProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && increment == other.increment)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * (first xor (first ushr 32)) + (last xor (last ushr 32))) + (increment xor (increment ushr 32))).toInt()

    override fun toString(): String = if (increment > 0) "$first..$last step $increment" else "$first downTo $last step ${-increment}"

    companion object {
        /**
         * Creates LongProgression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: Long, rangeEnd: Long, step: Long): LongProgression = LongProgression(rangeStart, rangeEnd, step)
    }
}

