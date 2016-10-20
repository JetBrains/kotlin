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

package kotlin

/**
 * Represents a 8-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `byte`.
 */
public class Byte private constructor() : Number(), Comparable<Byte> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Byte can have.
         */
        public const val MIN_VALUE: Byte = -128

        /**
         * A constant holding the maximum value an instance of Byte can have.
         */
        public const val MAX_VALUE: Byte = 127
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Byte
    /** Decrements this value. */
    public operator fun dec(): Byte
    /** Returns this value. */
    public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a 16-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `short`.
 */
public class Short private constructor() : Number(), Comparable<Short> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Short can have.
         */
        public const val MIN_VALUE: Short = -32768

        /**
         * A constant holding the maximum value an instance of Short can have.
         */
        public const val MAX_VALUE: Short = 32767
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Short
    /** Decrements this value. */
    public operator fun dec(): Short
    /** Returns this value. */
    public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a 32-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `int`.
 */
public class Int private constructor() : Number(), Comparable<Int> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Int can have.
         */
        public const val MIN_VALUE: Int = -2147483648

        /**
         * A constant holding the maximum value an instance of Int can have.
         */
        public const val MAX_VALUE: Int = 2147483647
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Int
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Int
    /** Decrements this value. */
    public operator fun dec(): Int
    /** Returns this value. */
    public operator fun unaryPlus(): Int
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange

    /** Shifts this value left by [bits]. */
    public infix fun shl(bitCount: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    public infix fun shr(bitCount: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    public infix fun ushr(bitCount: Int): Int
    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: Int): Int
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: Int): Int
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: Int): Int
    /** Inverts the bits in this value/ */
    public fun inv(): Int

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a 64-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `long`.
 */
public class Long private constructor() : Number(), Comparable<Long> {
    companion object {
        /**
         * A constant holding the minimum value an instance of Long can have.
         */
        public const val MIN_VALUE: Long = -9223372036854775807L - 1L

        /**
         * A constant holding the maximum value an instance of Long can have.
         */
        public const val MAX_VALUE: Long = 9223372036854775807L
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Long
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Long
    /** Decrements this value. */
    public operator fun dec(): Long
    /** Returns this value. */
    public operator fun unaryPlus(): Long
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Long

     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Byte): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Short): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Int): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public operator fun rangeTo(other: Long): LongRange

    /** Shifts this value left by [bits]. */
    public infix fun shl(bitCount: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    public infix fun shr(bitCount: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    public infix fun ushr(bitCount: Int): Long
    /** Performs a bitwise AND operation between the two values. */
    public infix fun and(other: Long): Long
    /** Performs a bitwise OR operation between the two values. */
    public infix fun or(other: Long): Long
    /** Performs a bitwise XOR operation between the two values. */
    public infix fun xor(other: Long): Long
    /** Inverts the bits in this value/ */
    public fun inv(): Long

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a single-precision 32-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `float`.
 */
public class Float private constructor() : Number(), Comparable<Float> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Float.
         */
        public val MIN_VALUE: Float

        /**
         * A constant holding the largest positive finite value of Float.
         */
        public val MAX_VALUE: Float

        /**
         * A constant holding the positive infinity value of Float.
         */
        public val POSITIVE_INFINITY: Float

        /**
         * A constant holding the negative infinity value of Float.
         */
        public val NEGATIVE_INFINITY: Float

        /**
         * A constant holding the "not a number" value of Float.
         */
        public val NaN: Float
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Float
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Float
    /** Decrements this value. */
    public operator fun dec(): Float
    /** Returns this value. */
    public operator fun unaryPlus(): Float
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Float


    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a double-precision 64-bit IEEE 754 floating point number.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `double`.
 */
public class Double private constructor() : Number(), Comparable<Double> {
    companion object {
        /**
         * A constant holding the smallest *positive* nonzero value of Double.
         */
        public val MIN_VALUE: Double

        /**
         * A constant holding the largest positive finite value of Double.
         */
        public val MAX_VALUE: Double

        /**
         * A constant holding the positive infinity value of Double.
         */
        public val POSITIVE_INFINITY: Double

        /**
         * A constant holding the negative infinity value of Double.
         */
        public val NEGATIVE_INFINITY: Double

        /**
         * A constant holding the "not a number" value of Double.
         */
        public val NaN: Double
    }

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Byte): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Short): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Int): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Long): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public operator fun compareTo(other: Float): Int

    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    public override operator fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public operator fun plus(other: Byte): Double
    /** Adds the other value to this value. */
    public operator fun plus(other: Short): Double
    /** Adds the other value to this value. */
    public operator fun plus(other: Int): Double
    /** Adds the other value to this value. */
    public operator fun plus(other: Long): Double
    /** Adds the other value to this value. */
    public operator fun plus(other: Float): Double
    /** Adds the other value to this value. */
    public operator fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public operator fun minus(other: Byte): Double
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Short): Double
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Int): Double
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Long): Double
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Float): Double
    /** Subtracts the other value from this value. */
    public operator fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public operator fun times(other: Byte): Double
    /** Multiplies this value by the other value. */
    public operator fun times(other: Short): Double
    /** Multiplies this value by the other value. */
    public operator fun times(other: Int): Double
    /** Multiplies this value by the other value. */
    public operator fun times(other: Long): Double
    /** Multiplies this value by the other value. */
    public operator fun times(other: Float): Double
    /** Multiplies this value by the other value. */
    public operator fun times(other: Double): Double

    /** Divides this value by the other value. */
    public operator fun div(other: Byte): Double
    /** Divides this value by the other value. */
    public operator fun div(other: Short): Double
    /** Divides this value by the other value. */
    public operator fun div(other: Int): Double
    /** Divides this value by the other value. */
    public operator fun div(other: Long): Double
    /** Divides this value by the other value. */
    public operator fun div(other: Float): Double
    /** Divides this value by the other value. */
    public operator fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Byte): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Short): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Int): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Long): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Float): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public operator fun mod(other: Double): Double

    /** Increments this value. */
    public operator fun inc(): Double
    /** Decrements this value. */
    public operator fun dec(): Double
    /** Returns this value. */
    public operator fun unaryPlus(): Double
    /** Returns the negative of this value. */
    public operator fun unaryMinus(): Double


    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

