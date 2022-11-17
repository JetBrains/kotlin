/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

class InMemoryStorageWrapper<K, V>(val origin: LazyStorage<K, V>) : LazyStorage<K, V> {
    private val inMemoryStorage = LinkedHashMap<K, V>()
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
        for ((key, value) in inMemoryStorage) {
            origin[key] = value
        }

        clean()
        isCleanRequested = false

        origin.flush(memoryCachesOnly)
    }

    override fun close() {
        origin.close()
    }

    override fun append(key: K, value: V) {
        // TODO
        @Suppress("UNCHECKED_CAST")
        when (value) {
            is Collection<*> -> ((inMemoryStorage[key] ?: origin[key]?.let { ArrayList(it as Collection<*>) } ?: mutableListOf<Any?>()) as MutableCollection<Any?>).addAll(value)
        }
    }

    override fun remove(key: K) {
        removedKeys.add(key)
        inMemoryStorage.remove(key)
    }

    override fun set(key: K, value: V) {
        inMemoryStorage[key] = value
    }

    override fun get(key: K): V? = when (key) {
        in inMemoryStorage -> inMemoryStorage[key]
        !in removedKeys -> origin[key]
        else -> null
    }

    override fun contains(key: K) = key in inMemoryStorage || (key !in removedKeys && key in origin)
}