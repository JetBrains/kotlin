/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.DataExternalizer
import java.util.*
import kotlin.collections.LinkedHashMap

interface InMemoryStorageWrapper<K, V> : AppendableLazyStorage<K, V> {
    fun resetInMemoryChanges()
}

/**
 * An in-memory wrapper for [origin] that keeps all the write operations in-memory.
 * Flushes all the changes to the [origin] on [flush] invocation.
 * [resetInMemoryChanges] should be called to reset in-memory changes of this wrapper.
 */
class DefaultInMemoryStorageWrapper<K, V>(
    private val origin: CachingLazyStorage<K, V>,
    private val valueExternalizer: DataExternalizer<V>
) :
    InMemoryStorageWrapper<K, V> {
    // These state properties keep the current diff that will be applied to the [origin] on flush if [resetInMemoryChanges] is not called
    private val inMemoryStorage = LinkedHashMap<K, ValueWrapper<V>>()
    private val removedKeys = hashSetOf<K>()
    private var isCleanRequested = false

    override val keys: Collection<K>
        get() = if (isCleanRequested) inMemoryStorage.keys else (origin.keys - removedKeys) + inMemoryStorage.keys

    override fun resetInMemoryChanges() {
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
        } else {
            for (key in removedKeys) {
                origin.remove(key)
            }
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
        check(valueExternalizer is AppendableDataExternalizer<V>) {
            "`valueExternalizer` should implement the `AppendableDataExternalizer` interface to be able to call `append`"
        }
        when {
            key in inMemoryStorage -> {
                val currentEntry = inMemoryStorage.getValue(key)
                // should we avoid new allocations by performing appends in-place?
                inMemoryStorage[key] = ValueWrapper(valueExternalizer.append(currentEntry.value, value), isAppend = currentEntry.isAppend)
            }
            key !in removedKeys && key in origin -> {
                inMemoryStorage[key] = ValueWrapper(value, isAppend = true)
            }
            else -> {
                set(key, value)
            }
        }
    }

    override fun remove(key: K) {
        removedKeys.add(key)
        inMemoryStorage.remove(key)
    }

    override fun set(key: K, value: V) {
        inMemoryStorage[key] = ValueWrapper(value)
    }

    override fun get(key: K): V? {
        return when (key) {
            in inMemoryStorage -> {
                val entry = inMemoryStorage.getValue(key)
                val originValue = origin[key]
                if (entry.isAppend && originValue != null) {
                    check(valueExternalizer is AppendableDataExternalizer<V>) {
                        "`valueExternalizer` should implement the `AppendableDataExternalizer` interface to be able to handle `append`"
                    }
                    valueExternalizer.append(originValue, entry.value)
                } else {
                    entry.value
                }
            }
            !in removedKeys -> origin[key]
            else -> null
        }
    }

    override fun contains(key: K) = key in inMemoryStorage || (key !in removedKeys && key in origin)

    private fun <K, V> Map<K, V>.getValue(key: K) =
        this[key] ?: error("$key was unexpectedly removed from in-memory storage. Seems to be a multithreading issue")

    private class ValueWrapper<V>(val value: V, val isAppend: Boolean = false)
}