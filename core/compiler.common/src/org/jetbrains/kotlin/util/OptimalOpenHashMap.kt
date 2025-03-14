/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.io.Serializable
import java.util.Objects

/**
 * OptimalOpenHashMap implements Map using open addressing with funnel probing.
 *
 * Based on:
 * "Optimal Bounds for Open Addressing Without Reordering"
 * by Martín Farach-Colton∗, Andrew Krapivin†, William Kuszmaul‡ (2025)
 *
 * This version uses lazy deletion via tombstones, cleaning up only when tombstones exceed a threshold (currently 25%)
 */
class OptimalOpenHashMap<K : Any, V : Any> : MutableMap<K, V>, java.io.Serializable {
    // Unique tombstone marker.
    //private val TOMBSTONE = Entry<K?, V?>(null, null, -1)

    private class Entry<K, V>(
        override val key: K, override var value: V, // cached hash value
        val hash: Int
    ) : MutableMap.MutableEntry<K, V>, Serializable {
        override fun setValue(value: V): V {
            val old = this.value
            this.value = value
            return old
        }

        override fun hashCode(): Int {
            return hash xor Objects.hashCode(value)
        }

        override fun equals(o: Any?): Boolean {
            if (o !is MutableMap.MutableEntry<*, *>) return false
            val e = o
            return hash == (e as Entry<*, *>).hash &&
                    key == e.key &&
                    value == e.value
        }

        override fun toString(): String {
            return "$key=$value"
        }
    }

    private var table = arrayOfNulls<Entry<*, *>>(DEFAULT_INITIAL_CAPACITY) as Array<Entry<K, V>?>
    private var _size = 0
    private var capacity: Int = DEFAULT_INITIAL_CAPACITY
    private val loadFactor: Float = DEFAULT_LOAD_FACTOR

    // Count of tombstones in the table.
    private var tombstones = 0

    /**
     * Table is a single array but we're filling it using the funnel method, in levels.
     *
     * @param key the key whose associated value is to be returned
     * @return
     */
    override fun get(key: K): V? {
//        if (key == null) return null
        val hash: Int = hash(key)

        var levelWidth = capacity ushr 1
        var offset = 0

        do {
            val levelIndex = hash and (levelWidth - 1)
            val tableIndex = offset + levelIndex

            val entry = table[tableIndex]
            if (entry == null) {
                return null
            }
            if (/*entry !== TOMBSTONE && */entry.hash == hash && entry.key == key) {
                return entry.value
            }
            // Move to next level
            offset = offset or levelWidth
            levelWidth = levelWidth ushr 1
        } while (levelWidth > 0)

        return null
    }

    override fun put(key: K, value: V): V? {
//        if (key == null) throw NullPointerException("key is null")
        if ((_size + tombstones + 1.0) / capacity > loadFactor) {
            check(capacity != MAXIMUM_CAPACITY) { "Cannot resize: maximum capacity reached ($MAXIMUM_CAPACITY)" }
            resize()
        }
        val hash: Int = hash(key)
        val tab = table

        var tombstoneIndex = -1 // fill gaps

        var levelWidth = capacity ushr 1
        var offset = 0

        do {
            val levelIndex = hash and (levelWidth - 1)
            var tableIndex = offset + levelIndex

            // Actual put() logic:
            val entry = table[tableIndex]
            if (entry == null || (/*entry !== TOMBSTONE && */entry.hash == hash && key == entry.key)) {
                val e = tab[tableIndex]
                if (e == null) {
//                    if (tombstoneIndex != -1) {
//                        // its a new entry, override the last tombstone:
//                        tableIndex = tombstoneIndex
//                        tombstones--
//                    }
                    tab[tableIndex] = Entry<K, V>(key, value, hash)
                    _size++
                    return null
                } else {
                    val oldVal = e.value
                    e.value = value
                    return oldVal
                }
            } //else if (entry === TOMBSTONE && tombstoneIndex == -1) {
              // tombstoneIndex = tableIndex
            //}

            // Move to next level
            offset = offset or levelWidth
            levelWidth = levelWidth ushr 1
        } while (levelWidth > 0)


        resize()
        return put(key, value)
    }

    override fun remove(key: K): V? = TODO()

    /**
     * Rehashes the entire table to remove tombstones.
     */
    private fun cleanup() {
        val oldTable = table
        val newTable = arrayOfNulls<Entry<*, *>>(capacity) as Array<Entry<K, V>?>
        var newSize = 0
        for (e in oldTable) {
            if (e != null/* && e !== TOMBSTONE*/) {
                val idx = funnelProbe(e.key, e.hash)
                if (newTable[idx] == null) {
                    newTable[idx] = e
                    newSize++
                    break
                }
            }
        }
        table = newTable
        _size = newSize
        tombstones = 0
    }

    /**
     * Divide the available space into blocks, for example a 128-capacity table in our case will have:
     * - 64 spots at level 1
     * - 32 spots at level 2
     * - 16 spots at level 3
     * - 8 spots at level 4
     * - 4 spots at level 5
     * - 2 spots at level 6
     * - 1 spot at level 7
     * - 1 spot at level 8
     *
     * This is probably not optimal, but fast to implement on the CPU.
     *
     * @param key   The key to probe for.
     * @param hash  The hashed value of the key.
     * @return      The computed index, or -1 if no available slot is found.
     */
    fun funnelProbe(key: Any, hash: Int): Int {
        var levelWidth = capacity ushr 1
        var offset = 0

        do {
            val levelIndex = hash and (levelWidth - 1)
            val tableIndex = offset + levelIndex

            val entry = table[tableIndex]
            if (entry == null || (/*entry !== TOMBSTONE && */entry.hash == hash && key == entry.key)) {
                // to avoid doing this twice this funnelProbe has been inlined into get(), put(), remove().
                return tableIndex
            }

            // Move to next level
            offset = offset or levelWidth
            levelWidth = levelWidth ushr 1
        } while (levelWidth > 0)

        // not found.
        return -1
    }

    private fun resize() {
        check(capacity < MAXIMUM_CAPACITY) { "Cannot resize: maximum capacity reached (" + MAXIMUM_CAPACITY + ")" }
        var newCapacity = capacity shl 1
        if (newCapacity > MAXIMUM_CAPACITY) newCapacity = MAXIMUM_CAPACITY
        val oldTable = table
        table = arrayOfNulls<Entry<*, *>>(newCapacity) as Array<Entry<K, V>?>
        capacity = newCapacity
        var newSize = 0
        tombstones = 0
        for (e in oldTable) {
            if (e != null/* && e !== TOMBSTONE*/) {
                val idx = funnelProbe(e.key, e.hash)
                table[idx] = e
                newSize++
            }
        }
        _size = newSize
    }

    override fun clear() {
        val newTable = arrayOfNulls<Entry<*, *>>(DEFAULT_INITIAL_CAPACITY) as Array<Entry<K, V>?>
        table = newTable
        _size = 0
        capacity = DEFAULT_INITIAL_CAPACITY
        tombstones = 0
    }

    override val size: Int
        get() = _size

    override fun isEmpty(): Boolean {
        return _size == 0
    }

    override fun containsKey(key: K): Boolean {
        return get(key) != null
    }

    override fun containsValue(value: V): Boolean {
        for (e in table) {
            if (e != null &&/* e !== TOMBSTONE && */e.value == value) return true
        }
        return false
    }

    override fun putAll(m: Map<out K, V>) {
        for (entry in m.entries) put(entry.key, entry.value)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            val set: MutableSet<MutableMap.MutableEntry<K, V>> = HashSet<MutableMap.MutableEntry<K, V>>()
            for (e in table) {
                if (e != null/* && e !== TOMBSTONE*/) {
                    set.add(e)
                }
            }
            return set
        }

    override val keys: MutableSet<K>
        get() {
            val set: MutableSet<K> = HashSet<K>()
            for (e in table) {
                if (e != null/* && e !== TOMBSTONE*/) {
                    set.add(e.key)
                }
            }
            return set
        }

    override val values: MutableCollection<V>
        get() {
            val values: MutableList<V> = ArrayList<V>()
            for (e in table) {
                if (e != null/* && e !== TOMBSTONE*/) {
                    values.add(e.value)
                }
            }
            return values
        }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("{")
        var first = true
        for (e in table) {
            if (e != null/* && e !== TOMBSTONE*/) {
                if (!first) {
                    sb.append(", ")
                }
                sb.append(e.key).append("=").append(e.value)
                first = false
            }
        }
        sb.append("}")
        return sb.toString()
    }

    companion object {
        const val DEFAULT_LOAD_FACTOR: Float = 0.9f // In theory this algorithm supports higher load factors
        const val DEFAULT_INITIAL_CAPACITY: Int = 1 shl 4 // 16
        const val MAXIMUM_CAPACITY: Int = 1 shl 30

        fun hash(key: Any): Int {
            val h: Int
            return (key.hashCode().also { h = it }) xor (h ushr 16)
        }
    }
}
