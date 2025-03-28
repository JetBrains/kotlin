/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.util

import org.jetbrains.kotlin.utils.addToStdlib.ChainedIterator

/**
 * `Multimap` is a flexible, type-safe way to represent a key-to-collection mapping.
 * It:
 * * Generalizes the concept of a `Map` by allowing multiple values per key.
 * * Supports different collection types (e.g., [SetMultimap], [ListMultimap]).
 *
 * ### Example usage
 *
 * ```kotlin
 * val setMultimap = setMultimapOf<String, Int>()
 * setMultimap.put("even", 2)
 * setMultimap.put("even", 4)
 * setMultimap.put("even", 2) // Duplicate ignored
 * println(setMultimap["even"]) // {2, 4}
 *
 * val listMultimap = listMultimapOf<String, Int>()
 * listMultimap.put("odds", 1)
 * listMultimap.put("odds", 3)
 * listMultimap.put("odds", 1) // Duplicate allowed
 * println(listMultimap["odds"]) // [1, 3, 1]
 * ```
 */
interface Multimap<K, out V, out C : Collection<V>> : Iterable<Map.Entry<K, C>> {
    operator fun get(key: K): C
    operator fun contains(key: K): Boolean
    val keys: Set<K>
    val values: Collection<V>

    override operator fun iterator(): Iterator<Map.Entry<K, C>>
}

interface MutableMultimap<K, V, C : Collection<V>> : Multimap<K, V, C> {
    fun put(key: K, value: V)
    fun putAll(key: K, values: Collection<V>) {
        values.forEach { put(key, it) }
    }

    fun remove(key: K, value: V)
    fun removeKey(key: K): C

    fun clear()
}

abstract class BaseMultimap<K, V, C : Collection<V>, MC : MutableCollection<V>> : MutableMultimap<K, V, C> {
    private val map: MutableMap<K, MC> = mutableMapOf()
    protected abstract fun createContainer(): MC
    protected abstract fun createEmptyContainer(): C

    override fun get(key: K): C {
        @Suppress("UNCHECKED_CAST")
        return map[key] as C? ?: createEmptyContainer()
    }

    override operator fun contains(key: K): Boolean {
        return key in map
    }

    override val keys: Set<K>
        get() = map.keys

    override val values: Collection<V>
        get() = object : AbstractCollection<V>() {
            override val size: Int
                get() = map.values.sumOf { it.size }

            override fun iterator(): Iterator<V> {
                return ChainedIterator(map.values.map { it.iterator() })
            }
        }

    override fun put(key: K, value: V) {
        val container = map.getOrPut(key) { createContainer() }
        container.add(value)
    }

    override fun remove(key: K, value: V) {
        val collection = map[key] ?: return
        collection.remove(value)
        if (collection.isEmpty()) {
            map.remove(key)
        }
    }

    override fun removeKey(key: K): C {
        @Suppress("UNCHECKED_CAST")
        return map.remove(key) as C? ?: createEmptyContainer()
    }

    override fun clear() {
        map.clear()
    }

    override fun iterator(): Iterator<Map.Entry<K, C>> {
        @Suppress("UNCHECKED_CAST")
        return map.iterator() as Iterator<Map.Entry<K, C>>
    }
}

class SetMultimap<K, V> : BaseMultimap<K, V, Set<V>, MutableSet<V>>() {
    override fun createContainer(): MutableSet<V> {
        return mutableSetOf()
    }

    override fun createEmptyContainer(): Set<V> {
        return emptySet()
    }
}

class ListMultimap<K, V> : BaseMultimap<K, V, List<V>, MutableList<V>>() {
    override fun createContainer(): MutableList<V> {
        return mutableListOf()
    }

    override fun createEmptyContainer(): List<V> {
        return emptyList()
    }
}

fun <K, V> setMultimapOf(): SetMultimap<K, V> = SetMultimap()
fun <K, V> listMultimapOf(): ListMultimap<K, V> = ListMultimap()

operator fun <K, V> MutableMultimap<K, V, *>.plusAssign(map: Map<K, Collection<V>>) {
    for ((key, values) in map) {
        this.putAll(key, values)
    }
}
