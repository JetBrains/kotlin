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
 * Represents a 8-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `byte`.
 */
public class Byte private () : Number, Comparable<Byte> {
    default object {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public fun plus(other: Char): Int
    /** Adds the other value to this value. */
    public fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public fun div(other: Char): Int
    /** Divides this value by the other value. */
    public fun div(other: Short): Int
    /** Divides this value by the other value. */
    public fun div(other: Int): Int
    /** Divides this value by the other value. */
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Byte
    /** Decrements this value. */
    public fun dec(): Byte
    /** Returns this value. */
    public fun plus(): Int
    /** Returns the negative of this value. */
    public fun minus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): ByteRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): CharRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): ShortRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public class Char private () : Comparable<Char> {
    default object {}

/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Char): Int
/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares the character code of this character with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public fun div(other: Short): Int
    /** Divides this value by the other value. */
    public fun div(other: Int): Int
    /** Divides this value by the other value. */
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Char
    /** Decrements this value. */
    public fun dec(): Char
    /** Returns this value. */
    public fun plus(): Int
    /** Returns the negative of this value. */
    public fun minus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): CharRange

    /** Returns the value of this character as a `Byte`. */
    public override fun toByte(): Byte
    /** Returns the value of this character as a `Char`. */
    public override fun toChar(): Char
    /** Returns the value of this character as a `Short`. */
    public override fun toShort(): Short
    /** Returns the value of this character as a `Int`. */
    public override fun toInt(): Int
    /** Returns the value of this character as a `Long`. */
    public override fun toLong(): Long
    /** Returns the value of this character as a `Float`. */
    public override fun toFloat(): Float
    /** Returns the value of this character as a `Double`. */
    public override fun toDouble(): Double
}

/**
 * Represents a 16-bit signed integer.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `short`.
 */
public class Short private () : Number, Comparable<Short> {
    default object {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public fun plus(other: Char): Int
    /** Adds the other value to this value. */
    public fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public fun div(other: Char): Int
    /** Divides this value by the other value. */
    public fun div(other: Short): Int
    /** Divides this value by the other value. */
    public fun div(other: Int): Int
    /** Divides this value by the other value. */
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Short
    /** Decrements this value. */
    public fun dec(): Short
    /** Returns this value. */
    public fun plus(): Int
    /** Returns the negative of this value. */
    public fun minus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): ShortRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): ShortRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): ShortRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

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
public class Int private () : Number, Comparable<Int> {
    default object {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    public fun plus(other: Char): Int
    /** Adds the other value to this value. */
    public fun plus(other: Short): Int
    /** Adds the other value to this value. */
    public fun plus(other: Int): Int
    /** Adds the other value to this value. */
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Int
    /** Divides this value by the other value. */
    public fun div(other: Char): Int
    /** Divides this value by the other value. */
    public fun div(other: Short): Int
    /** Divides this value by the other value. */
    public fun div(other: Int): Int
    /** Divides this value by the other value. */
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Int
    /** Decrements this value. */
    public fun dec(): Int
    /** Returns this value. */
    public fun plus(): Int
    /** Returns the negative of this value. */
    public fun minus(): Int

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): IntRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

    /** Shifts this value left by [bits]. */
    public fun shl(bits: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    public fun shr(bits: Int): Int
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    public fun ushr(bits: Int): Int
    /** Performs a bitwise AND operation between the two values. */
    public fun and(other: Int): Int
    /** Performs a bitwise OR operation between the two values. */
    public fun or(other: Int): Int
    /** Performs a bitwise XOR operation between the two values. */
    public fun xor(other: Int): Int
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
public class Long private () : Number, Comparable<Long> {
    default object {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Long
    /** Adds the other value to this value. */
    public fun plus(other: Char): Long
    /** Adds the other value to this value. */
    public fun plus(other: Short): Long
    /** Adds the other value to this value. */
    public fun plus(other: Int): Long
    /** Adds the other value to this value. */
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Long
    /** Divides this value by the other value. */
    public fun div(other: Char): Long
    /** Divides this value by the other value. */
    public fun div(other: Short): Long
    /** Divides this value by the other value. */
    public fun div(other: Int): Long
    /** Divides this value by the other value. */
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Long
    /** Decrements this value. */
    public fun dec(): Long
    /** Returns this value. */
    public fun plus(): Long
    /** Returns the negative of this value. */
    public fun minus(): Long

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): LongRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

    /** Shifts this value left by [bits]. */
    public fun shl(bits: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit. */
    public fun shr(bits: Int): Long
    /** Shifts this value right by [bits], filling the leftmost bits with zeros. */
    public fun ushr(bits: Int): Long
    /** Performs a bitwise AND operation between the two values. */
    public fun and(other: Long): Long
    /** Performs a bitwise OR operation between the two values. */
    public fun or(other: Long): Long
    /** Performs a bitwise XOR operation between the two values. */
    public fun xor(other: Long): Long
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
public class Float private () : Number, Comparable<Float> {
    default object : FloatingPointConstants<Float> {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Float
    /** Adds the other value to this value. */
    public fun plus(other: Char): Float
    /** Adds the other value to this value. */
    public fun plus(other: Short): Float
    /** Adds the other value to this value. */
    public fun plus(other: Int): Float
    /** Adds the other value to this value. */
    public fun plus(other: Long): Float
    /** Adds the other value to this value. */
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Float
    /** Divides this value by the other value. */
    public fun div(other: Char): Float
    /** Divides this value by the other value. */
    public fun div(other: Short): Float
    /** Divides this value by the other value. */
    public fun div(other: Int): Float
    /** Divides this value by the other value. */
    public fun div(other: Long): Float
    /** Divides this value by the other value. */
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Float
    /** Decrements this value. */
    public fun dec(): Float
    /** Returns this value. */
    public fun plus(): Float
    /** Returns the negative of this value. */
    public fun minus(): Float

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): FloatRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

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
public class Double private () : Number, Comparable<Double> {
    default object : FloatingPointConstants<Double> {}

/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Byte): Int
/**
 * Compares this value with the character code of the specified character for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Char): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Short): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Int): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Long): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public fun compareTo(other: Float): Int
/**
 * Compares this value with the specified value for order.
 * Returns zero if this value is equal to the specified other value, a negative number if its less than other, 
 * or a positive number if its greater than other.
 */
    public override fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    public fun plus(other: Byte): Double
    /** Adds the other value to this value. */
    public fun plus(other: Char): Double
    /** Adds the other value to this value. */
    public fun plus(other: Short): Double
    /** Adds the other value to this value. */
    public fun plus(other: Int): Double
    /** Adds the other value to this value. */
    public fun plus(other: Long): Double
    /** Adds the other value to this value. */
    public fun plus(other: Float): Double
    /** Adds the other value to this value. */
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    public fun minus(other: Byte): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Short): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Int): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Long): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Float): Double
    /** Subtracts the other value from this value. */
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    public fun times(other: Byte): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Char): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Short): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Int): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Long): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Float): Double
    /** Multiplies this value by the other value. */
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    public fun div(other: Byte): Double
    /** Divides this value by the other value. */
    public fun div(other: Char): Double
    /** Divides this value by the other value. */
    public fun div(other: Short): Double
    /** Divides this value by the other value. */
    public fun div(other: Int): Double
    /** Divides this value by the other value. */
    public fun div(other: Long): Double
    /** Divides this value by the other value. */
    public fun div(other: Float): Double
    /** Divides this value by the other value. */
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Byte): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Char): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Short): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Int): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Long): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Float): Double
    /** Calculates the remainder of dividing this value by the other value. */
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Double
    /** Decrements this value. */
    public fun dec(): Double
    /** Returns this value. */
    public fun plus(): Double
    /** Returns the negative of this value. */
    public fun minus(): Double

     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Byte): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Char): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Short): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Int): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Long): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Float): DoubleRange
     /** Creates a range from this value to the specified [other] value. */
    public fun rangeTo(other: Double): DoubleRange

    public override fun toByte(): Byte
    public override fun toChar(): Char
    public override fun toShort(): Short
    public override fun toInt(): Int
    public override fun toLong(): Long
    public override fun toFloat(): Float
    public override fun toDouble(): Double
}

