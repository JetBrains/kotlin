/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * An in-memory wrapper for [origin] that keeps all the write operations in-memory.
 * Flushes all the changes to the [origin] on [flush] invocation.
 * [resetInMemoryChanges] should be called to reset in-memory changes of this wrapper.
 */
class InMemoryStorageWrapper<K, V>(val origin: LazyStorage<K, V>) : LazyStorage<K, V> {
    private val inMemoryStorage = LinkedHashMap<K, ValueWrapper<V>>()
    private val removedKeys = hashSetOf<K>()
    private var isCleanRequested = false

    override val keys: Collection<K>
        get() = if (isCleanRequested) inMemoryStorage.keys else (origin.keys - removedKeys) + inMemoryStorage.keys

    fun resetInMemoryChanges() {
        isCleanRequested = false
        inMemoryStorage.clear()
        removedKeys.clear()
    }

    override fun clean() {
        inMemoryStorage.clear()
        removedKeys.clear()
        isCleanRequested = true
    }

    override fun flush(memoryCachesOnly: Boolean) {
        if (isCleanRequested) {
            origin.clean()
        }
        for (key in removedKeys) {
            origin.remove(key)
        }
        for ((key, valueWrapper) in inMemoryStorage) {
            if (valueWrapper.isAppend) {
                origin.append(key, valueWrapper.value)
                origin[key] // trigger chunks compaction
            } else {
                origin[key] = valueWrapper.value
            }
        }

        clean()
        isCleanRequested = false

        origin.flush(memoryCachesOnly)
    }

    override fun close() {
        origin.close()
    }

    override fun append(key: K, value: V) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
            is Collection<*> -> {
                when {
                    key in inMemoryStorage -> {
                        // the key's value was set in-memory, so we keep changing it without additional marking as append
                        (inMemoryStorage.getValue(key).value as MutableCollection<Any?>).addAll(value)
                    }
                    key !in removedKeys && key in origin -> {
                        val collection = copyCollection(origin[key] as Collection<*>)
                        collection.addAll(value)
                        inMemoryStorage[key] = ValueWrapper(collection as V, isAppend = true)
                    }
                    else -> {
                        set(key, value)
                    }
                }
            }
            else -> error("The value is expected to implement the Collection interface")
        }
    }

    override fun remove(key: K) {
        removedKeys.add(key)
        inMemoryStorage.remove(key)
    }

    override fun set(key: K, value: V) {
        @Suppress("UNCHECKED_CAST")
        inMemoryStorage[key] = ValueWrapper(if (value is Collection<*>) copyCollection(value) as V else value)
    }

    private fun copyCollection(collection: Collection<*>): MutableCollection<Any?> {
        val newCollection = when (collection) {
            is Set<*> -> TreeSet<Any?>()
            else -> ArrayList<Any?>(collection.size)
        }
        newCollection.addAll(collection)
        return newCollection
    }

    override fun get(key: K): V? = when (key) {
        in inMemoryStorage -> inMemoryStorage.getValue(key).value
        !in removedKeys -> origin[key]
        else -> null
    }

    override fun contains(key: K) = key in inMemoryStorage || (key !in removedKeys && key in origin)

    private fun <K, V> Map<K, V>.getValue(key: K) =
        this[key] ?: error("$key was unexpectedly removed from in-memory storage. Seems to be a multithreading issue")

    class ValueWrapper<V>(val value: V, val isAppend: Boolean = false)
}