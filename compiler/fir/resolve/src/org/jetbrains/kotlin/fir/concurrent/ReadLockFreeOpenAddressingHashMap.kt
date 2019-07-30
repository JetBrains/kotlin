/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.pow
import kotlin.math.sqrt

class ReadLockFreeOpenAddressingHashMap<K : Any, V> {
    @Volatile
    private var core = Core(128)

    operator fun contains(key: K) = core.getKey(key) === NOT_FOUND

    @Suppress("UNCHECKED_CAST")
    operator fun get(key: K): V? {
        val value = core.getKey(key)
        return if (value === NOT_FOUND) null else value as V
    }

    /**
     * Should be externally synchronized
     */
    // @Synchronized
    operator fun set(key: K, value: V) {
        while (core.isOverpopulated() || !core.putKey(key, value))
            core = core.rehash(core.length())
    }

    /**
     * Should be externally synchronized
     */
    // @Synchronized
    fun putAll(map: Map<K, V>) {
        for ((k, v) in map) {
            set(k, v)
        }
    }

    private class Core(
        capacity: Int
    ) : AtomicReferenceArray<Any?>(2 * capacity) {
        init {
            require(capacity and (capacity - 1) == 0)
        }


        private val shift = Integer.numberOfLeadingZeros(capacity) + 1
        private val limit: Int = capacity / 8
        var size = 0

        fun isOverpopulated(): Boolean {
            return size > (length() / 2 * DEFAULT_LOAD_FACTOR)
        }

        fun getKey(key: Any): Any? {
            var index = index(key)
            repeat(limit) {
                when (get(index)) {
                    null -> return NOT_FOUND
                    key -> return get(index + 1)
                }
                index -= 2
                if (index < 0) index = length() - 2
            }
            return NOT_FOUND
        }

        fun putKey(key: Any, value: Any?): Boolean {
            var index = index(key)
            repeat(limit) {
                when (get(index)) {
                    null -> {
                        lazySet(index + 1, value) // must be first!!!
                        set(index, key)
                        size++
                        return true
                    }
                    key -> {
                        set(index + 1, value)
                        return true
                    }
                }
                index -= 2
                if (index < 0) index = length() - 2
            }
            return false
        }


        fun index(key: Any): Int = (key.hashCode() * MAGIC) ushr shift shl 1

    }

    private tailrec fun Core.rehash(capacity: Int): Core {
        val new = Core(capacity)
        for (i in 0 until length() step 2) {
            val key = get(i)
            if (key != null) {
                if (!new.putKey(key, get(i + 1)))
                    return rehash(capacity * 2)
            }
        }
        return new
    }

}

private val MAGIC: Int = ((sqrt(5.0) - 1) * 2.0.pow(31)).toLong().toInt() // golden
private val NOT_FOUND = Any()
private const val DEFAULT_LOAD_FACTOR = 0.5





