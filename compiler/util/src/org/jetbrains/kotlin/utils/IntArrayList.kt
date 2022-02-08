/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

// Based on com.intellij.util.containers.IntArrayList.
class IntArrayList(initialCapacity: Int) {
    private var data = IntArray(initialCapacity)
    private var size = 0

    fun trimToSize() {
        if (size < data.size) {
            data = data.copyOf(size)
        }
    }

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > data.size) {
            data = data.copyOf(maxOf(data.size * 3 / 2 + 1, minCapacity))
        }
    }

    fun size(): Int = size

    val isEmpty: Boolean get() = size == 0

    operator fun get(index: Int): Int = data[index]

    fun add(o: Int) {
        ensureCapacity(size + 1)
        data[size++] = o
    }

    override fun toString(): String =
        (0 until size).joinToString(", ", "[", "]") { this[it].toString() }
}
