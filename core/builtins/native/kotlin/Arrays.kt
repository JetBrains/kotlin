/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
 * An array of bytes. When targeting the JVM, instances of this class are represented as `byte[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class ByteArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Byte)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Byte
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Byte): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator
}

/**
 * An array of chars. When targeting the JVM, instances of this class are represented as `char[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class CharArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Char)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Char
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Char): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): CharIterator
}

/**
 * An array of shorts. When targeting the JVM, instances of this class are represented as `short[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class ShortArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Short)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Short
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Short): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ShortIterator
}

/**
 * An array of ints. When targeting the JVM, instances of this class are represented as `int[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class IntArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Int)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Int
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Int): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): IntIterator
}

/**
 * An array of longs. When targeting the JVM, instances of this class are represented as `long[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class LongArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Long)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Long
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Long): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): LongIterator
}

/**
 * An array of floats. When targeting the JVM, instances of this class are represented as `float[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class FloatArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Float)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Float
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Float): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): FloatIterator
}

/**
 * An array of doubles. When targeting the JVM, instances of this class are represented as `double[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to zero.
 */
public class DoubleArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Double)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Double
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Double): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): DoubleIterator
}

/**
 * An array of booleans. When targeting the JVM, instances of this class are represented as `boolean[]`.
 * @constructor Creates a new array of the specified [size], with all elements initialized to false.
 */
public class BooleanArray(size: Int) {
    /**
     * Creates a new array of the specified [size], where each element is calculated by calling the specified
     * [init] function. The [init] function returns an array element given its index.
     */
    public inline constructor(size: Int, init: (Int) -> Boolean)

    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): Boolean
    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: Boolean): Unit

    /** Returns the number of elements in the array. */
    public val size: Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): BooleanIterator
}

