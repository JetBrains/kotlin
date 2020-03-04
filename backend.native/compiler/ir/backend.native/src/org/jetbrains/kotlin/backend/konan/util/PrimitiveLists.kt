/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.util

class IntArrayList : Iterable<Int> {
    private var array = IntArray(3)
    private var length = 0

    val size get() = length

    fun isEmpty() = size == 0

    fun add(x: Int) {
        length++
        ensureCapacity(length)
        set(length - 1, x)
    }

    fun reserve(minSize: Int) {
        if (length >= minSize) return
        length = minSize
        ensureCapacity(minSize)
    }

    operator fun get(index: Int): Int = when {
        index < 0 || index >= length -> throw IndexOutOfBoundsException("Index out of range: $index")

        else -> array[index]
    }

    operator fun set(index: Int, x: Int) = when {
        index < 0 || index >= length -> throw IndexOutOfBoundsException("Index out of range: $index")

        else -> array[index] = x
    }

    override operator fun iterator(): Iterator<Int> = Itr()

    private fun ensureCapacity(minCapacity: Int) {
        val oldArray = array
        if (minCapacity > oldArray.size) {
            var newSize = oldArray.size * 3 / 2
            if (minCapacity > newSize)
                newSize = minCapacity
            array = oldArray.copyOf(newSize)
        }
    }

    private inner class Itr : Iterator<Int> {
        private var index = 0

        override fun hasNext() = index < size

        override fun next() = get(index++)
    }
}

class LongArrayList : Iterable<Long> {
    private var array = LongArray(3)
    private var length = 0

    val size get() = length

    fun isEmpty() = size == 0

    fun add(x: Long) {
        length++
        ensureCapacity(length)
        set(length - 1, x)
    }

    fun get(index: Int): Long = when {
        index < 0 || index >= length -> throw IndexOutOfBoundsException("Index out of range: $index")

        else -> array[index]
    }

    fun set(index: Int, x: Long) = when {
        index < 0 || index >= length -> throw IndexOutOfBoundsException("Index out of range: $index")

        else -> array[index] = x
    }

    override operator fun iterator(): Iterator<Long> = Itr()

    private fun ensureCapacity(minCapacity: Int) {
        val oldArray = array
        if (minCapacity > oldArray.size) {
            var newSize = oldArray.size * 3 / 2
            if (minCapacity > newSize)
                newSize = minCapacity
            array = oldArray.copyOf(newSize)
        }
    }

    private inner class Itr : Iterator<Long> {
        private var index = 0

        override fun hasNext() = index < size

        override fun next() = get(index++)
    }
}