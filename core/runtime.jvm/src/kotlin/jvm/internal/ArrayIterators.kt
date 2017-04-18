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

package kotlin.jvm.internal

private class ArrayByteIterator(private val array: ByteArray) : ByteIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextByte() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayCharIterator(private val array: CharArray) : CharIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextChar() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayShortIterator(private val array: ShortArray) : ShortIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextShort() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayIntIterator(private val array: IntArray) : IntIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextInt() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayLongIterator(private val array: LongArray) : LongIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextLong() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayFloatIterator(private val array: FloatArray) : FloatIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextFloat() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayDoubleIterator(private val array: DoubleArray) : DoubleIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextDouble() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayBooleanIterator(private val array: BooleanArray) : BooleanIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextBoolean() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

public fun iterator(array: ByteArray): ByteIterator = ArrayByteIterator(array)
public fun iterator(array: CharArray): CharIterator = ArrayCharIterator(array)
public fun iterator(array: ShortArray): ShortIterator = ArrayShortIterator(array)
public fun iterator(array: IntArray): IntIterator = ArrayIntIterator(array)
public fun iterator(array: LongArray): LongIterator = ArrayLongIterator(array)
public fun iterator(array: FloatArray): FloatIterator = ArrayFloatIterator(array)
public fun iterator(array: DoubleArray): DoubleIterator = ArrayDoubleIterator(array)
public fun iterator(array: BooleanArray): BooleanIterator = ArrayBooleanIterator(array)
