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

package kotlin

/**
 * Represents a 16-bit Unicode character.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.
 */
public class Char private () : Comparable<Char> {
    companion object {}

    /**
     * Compares the character code of this character with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int or other operand to Char.")
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
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int or other operand to Char.")
    public fun compareTo(other: Short): Int
    /**
     * Compares the character code of this character with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int or other operand to Char.")
    public fun compareTo(other: Int): Int
    /**
     * Compares the character code of this character with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int or other operand to Char.")
    public fun compareTo(other: Long): Int
    /**
     * Compares the character code of this character with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun compareTo(other: Float): Int
    /**
     * Compares the character code of this character with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if its less than other,
     * or a positive number if its greater than other.
     */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun compareTo(other: Double): Int

    /** Adds the other value to this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(other: Byte): Int
    /** Adds the other value to this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(other: Short): Int
    /** Adds the other value to this value. */
    deprecated("This operation will change return type to Char in M13. Either call toInt or toChar on return value or convert Char operand to Int.")
    public fun plus(other: Int): Int
    /** Adds the other value to this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(other: Long): Long
    /** Adds the other value to this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(other: Float): Float
    /** Adds the other value to this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(other: Double): Double

    /** Subtracts the other value from this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun minus(other: Byte): Int
    /** Subtracts the other value from this value. */
    public fun minus(other: Char): Int
    /** Subtracts the other value from this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun minus(other: Short): Int
    /** Subtracts the other value from this value. */
    deprecated("This operation will change return type to Char in M13. Either call toInt or toChar on return value or convert Char operand to Int.")
    public fun minus(other: Int): Int
    /** Subtracts the other value from this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun minus(other: Long): Long
    /** Subtracts the other value from this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun minus(other: Float): Float
    /** Subtracts the other value from this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun minus(other: Double): Double

    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Byte): Int
    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Short): Int
    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Int): Int
    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Long): Long
    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Float): Float
    /** Multiplies this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun times(other: Double): Double

    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Byte): Int
    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Short): Int
    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Int): Int
    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Long): Long
    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Float): Float
    /** Divides this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun div(other: Double): Double

    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Byte): Int
    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Short): Int
    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Int): Int
    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Long): Long
    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Float): Float
    /** Calculates the remainder of dividing this value by the other value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun mod(other: Double): Double

    /** Increments this value. */
    public fun inc(): Char
    /** Decrements this value. */
    public fun dec(): Char
    /** Returns this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
    public fun plus(): Int
    /** Returns the negative of this value. */
    deprecated("This operation doesn't make sense and shall be removed in M13. Consider converting Char operand to Int.")
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

