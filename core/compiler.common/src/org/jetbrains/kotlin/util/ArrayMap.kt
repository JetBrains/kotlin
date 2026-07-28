/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

sealed class ArrayMap<T : Any> : Iterable<T> {
    abstract val size: Int

    abstract operator fun set(index: Int, value: T)
    abstract operator fun get(index: Int): T?

    abstract fun copy(): ArrayMap<T>
}

internal object EmptyArrayMap : ArrayMap<Nothing>() {
    override val size: Int
        get() = 0

    override fun set(index: Int, value: Nothing) {
        throw IllegalStateException()
    }

    override fun get(index: Int): Nothing? {
        return null
    }

    override fun copy(): ArrayMap<Nothing> = this

    override fun iterator(): Iterator<Nothing> {
        return object : Iterator<Nothing> {
            override fun hasNext(): Boolean = false

            override fun next(): Nothing = throw NoSuchElementException()
        }
    }
}

internal class OneElementArrayMap<T : Any>(val value: T, val index: Int) : ArrayMap<T>() {
    override val size: Int
        get() = 1

    override fun set(index: Int, value: T) {
        throw IllegalStateException()
    }

    override fun get(index: Int): T? {
        return if (index == this.index) value else null
    }

    override fun copy(): ArrayMap<T> = OneElementArrayMap(value, index)

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            private var notVisited = true

            override fun hasNext(): Boolean {
                return notVisited
            }

            override fun next(): T {
                if (notVisited) {
                    notVisited = false
                    return value
                } else {
                    throw NoSuchElementException()
                }
            }
        }
    }
}

/**
 * Mutations ([set], [remove]) are guarded by this map's own monitor, and [data]/[size] are volatile, so that concurrent
 *   mutations cannot corrupt the map and lock-free readers ([get], [iterator], [entries]) cannot observe a partially
 *   updated state.
 *
 * Note that this only makes a single [ArrayMapImpl] instance safe to use concurrently. It does *not* make its owner
 *   thread-safe: [AttributeArrayOwner] replaces the whole [ArrayMap] implementation as it grows, and that transition
 *   still has to be synchronized by the owner (see [AttributeArrayOwner.registerComponent]).
 */
internal class ArrayMapImpl<T : Any> private constructor(
    data: Array<Any?>,
    initialSize: Int
) : ArrayMap<T>() {
    companion object {
        private const val DEFAULT_SIZE = 20
        private const val INCREASE_K = 2
    }

    constructor() : this(arrayOfNulls<Any>(DEFAULT_SIZE), 0)

    /**
     * `volatile`, because [set] replaces the array to grow it, and because a write to it publishes the preceding writes
     *   to its elements to lock-free readers.
     */
    @Volatile
    private var data: Array<Any?> = data

    @Volatile
    override var size: Int = initialSize
        private set

    /**
     * Returns an array which is large enough to hold [index], growing a copy of [data] if needed.
     *
     * Must be called while holding this map's monitor.
     */
    private fun ensureCapacity(index: Int): Array<Any?> {
        val array = data
        if (array.size > index) return array
        var newSize = array.size
        do {
            newSize *= INCREASE_K
        } while (newSize <= index)
        return array.copyOf(newSize)
    }

    override operator fun set(index: Int, value: T) {
        synchronized(this) {
            val array = ensureCapacity(index)
            if (array[index] == null) {
                size++
            }
            array[index] = value

            // Publishes the element write above (and a possibly grown array) to lock-free readers.
            data = array
        }
    }

    override operator fun get(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return data.getOrNull(index) as T?
    }

    override fun copy(): ArrayMap<T> = synchronized(this) { ArrayMapImpl(data.copyOf(), size) }

    override fun iterator(): Iterator<T> {
        val array = data
        return object : AbstractIterator<T>() {
            private var index = -1

            override fun computeNext() {
                do {
                    index++
                } while (index < array.size && array[index] == null)
                if (index >= array.size) {
                    done()
                } else {
                    @Suppress("UNCHECKED_CAST")
                    setNext(array[index] as T)
                }
            }
        }
    }

    fun remove(index: Int) {
        synchronized(this) {
            val array = data
            if (array[index] != null) {
                size--
            }
            array[index] = null

            // Publishes the element write above to lock-free readers.
            data = array
        }
    }

    fun entries(): List<Entry<T>> {
        @Suppress("UNCHECKED_CAST")
        return data.mapIndexedNotNull { index, value -> if (value != null) Entry(index, value as T) else null }
    }

    data class Entry<T>(override val key: Int, override val value: T) : Map.Entry<Int, T>
}
