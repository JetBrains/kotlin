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
 * Represents an array (specifically, a Java array when targeting the JVM platform).
 * Array instances can be created using the [array] and [arrayOfNulls] standard
 * library functions.
 * See [Kotlin language documentation](http://kotlinlang.org/docs/reference/basic-types.html#arrays)
 * for more information on arrays.
 */
public class Array<reified T> private (): Cloneable {
    /**
     * Returns the array element at the specified [index]. This method can be called using the
     * index operator:
     * ```
     * value = arr[index]
     * ```
     */
    public fun get(index: Int): T

    /**
     * Sets the array element at the specified [index] to the specified [value]. This method can
     * be called using the index operator:
     * ```
     * arr[index] = value
     * ```
     */
    public fun set(index: Int, value: T): Unit

    /**
     * Returns the number of elements in the array.
     */
    public fun size(): Int

    /**
     * Creates an iterator for iterating over the elements of the array.
     */
    public fun iterator(): Iterator<T>

    /**
     * Creates a shallow copy of the array.
     */
    public override fun clone(): Array<T>
}
