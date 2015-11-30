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

package kotlin.jvm.internal

private class ArrayByteIterator(private val array: ByteArray) : ByteIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextByte() = array[index++]
}

private class ArrayCharIterator(private val array: CharArray) : CharIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextChar() = array[index++]
}

private class ArrayShortIterator(private val array: ShortArray) : ShortIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextShort() = array[index++]
}

private class ArrayIntIterator(private val array: IntArray) : IntIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextInt() = array[index++]
}

private class ArrayLongIterator(private val array: LongArray) : LongIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextLong() = array[index++]
}

private class ArrayFloatIterator(private val array: FloatArray) : FloatIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextFloat() = array[index++]
}

private class ArrayDoubleIterator(private val array: DoubleArray) : DoubleIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextDouble() = array[index++]
}

private class ArrayBooleanIterator(private val array: BooleanArray) : BooleanIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextBoolean() = array[index++]
}

public fun iterator(array: ByteArray): ByteIterator = ArrayByteIterator(array)
public fun iterator(array: CharArray): CharIterator = ArrayCharIterator(array)
public fun iterator(array: ShortArray): ShortIterator = ArrayShortIterator(array)
public fun iterator(array: IntArray): IntIterator = ArrayIntIterator(array)
public fun iterator(array: LongArray): LongIterator = ArrayLongIterator(array)
public fun iterator(array: FloatArray): FloatIterator = ArrayFloatIterator(array)
public fun iterator(array: DoubleArray): DoubleIterator = ArrayDoubleIterator(array)
public fun iterator(array: BooleanArray): BooleanIterator = ArrayBooleanIterator(array)
