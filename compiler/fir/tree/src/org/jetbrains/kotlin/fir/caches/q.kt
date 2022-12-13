/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import java.util.*
import java.util.function.BiConsumer

// binary representation of fractional part of phi = (sqrt(5) - 1) / 2
private const val MAGIC: Int = 0x9E3779B9L.toInt() // ((sqrt(5.0) - 1) / 2 * pow(2.0, 32.0)).toLong().toString(16)
private const val MAX_SHIFT = 27
private const val THRESHOLD = ((1L shl 31) - 1).toInt() // 50% fill factor for speed
private val EMPTY_ARRAY = arrayOf<Any?>()


// For more details see for Knuth's multiplicative hash with golden ratio
// Shortly, we're trying to keep distribution of it uniform independently of input
// It's necessary because we use very simple linear probing
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.computeHash(shift: Int) = ((hashCode() * MAGIC) ushr shift) shl 1

/**
 * The main ideas that might lead to better locality:
 * - Storing values in the same array as keys
 * - Using linear probes to avoid jumping to to new random indices
 *
 * This Map implementation is not intended to follow some of the maps' contracts:
 * - `put` doesn't returns previous value
 * - `entries` is unsupported (use forEach instead)
 * - `remove` is unsupported
 */
internal class OpenAddressLinearProbingHashTableNullable<K : Any, V, CONTEXT>(
    private val createValue: (K, CONTEXT) -> V
) : FirCache<K, V, CONTEXT>() {
    // fields be initialized later in `clear()`

    // capacity = 1 << (32 - shift)
    private var shift = 0
    // keys are stored in even elements, values are in odd ones
    private var array = EMPTY_ARRAY
    private var size_ = 0

    init {
        clear()
    }

    private fun index(key: K): Int {
        var i = key.computeHash(shift)
        var k = array[i]

        while (true) {
            if (k === null) return -i
            if (k == key) return i + 1
            if (i == 0) {
                i = array.size
            }
            i -= 2
            k = array[i]
        }
    }

    override fun getValueIfComputed(key: K): V? {
        val index = index(key)
        if (index <= 0) return null
        @Suppress("UNCHECKED_CAST")
        return array[index] as V
    }

    override fun getValue(key: K, context: CONTEXT): V {
        val index = index(key)
        @Suppress("UNCHECKED_CAST")
        if (index > 0) return array[index] as V
        val nIndex = -index
        array[nIndex] = key
        val newValue = createValue(key, context)
        array[nIndex + 1] = newValue

        rehashIfNeeded()

        return newValue
    }

    private fun rehashIfNeeded() {
        if (++size_ >= (THRESHOLD ushr shift)) {
            rehash()
        }
    }

    private fun rehash() {
        val newShift = maxOf(shift - 3, 0)
        val newArraySize = 1 shl (33 - newShift)
        val newArray = arrayOfNulls<Any>(newArraySize)

        var i = 0
        val arraySize = array.size
        while (i < arraySize) {
            val key = array[i]
            if (key != null) {
                put(newArray, newShift, key, array[i + 1])
            }
            i += 2
        }

        shift = newShift
        array = newArray
    }

    fun clear() {
        shift = MAX_SHIFT
        array = arrayOfNulls(1 shl (33 - shift))

        size_ = 0
    }

    fun forEach(action: BiConsumer<in K, in V>) {
        var i = 0
        val arraySize = array.size
        while (i < arraySize) {
            val key = array[i]
            if (key != null) {
                @Suppress("UNCHECKED_CAST")
                action.accept(key as K, array[i + 1] as V)
            }
            i += 2
        }
    }

    val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (@Suppress("ConstantConditionIf") DEBUG) {
                return Collections.unmodifiableSet(mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
                    forEach { key, value -> add(Entry(key, value)) }
                })
            }

            throw IllegalStateException("OpenAddressLinearProbingHashTable::entries is not supported and hardly will be")
        }

    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("This Entry is not mutable.")
    }

    companion object {
        // Change to "true" to be able to see the contents of the map in debugger views
        private const val DEBUG = false
    }
}

private fun put(array: Array<Any?>, aShift: Int, key: Any, value: Any?): Boolean {
    var i = key.computeHash(aShift)

    while (true) {
        val k = array[i]
        if (k == null) {
            array[i] = key
            array[i + 1] = value
            return true
        }
        if (k == key) break
        if (i == 0) {
            i = array.size
        }
        i -= 2
    }

    array[i + 1] = value

    return false
}
