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
    private val inMemoryStorage = LinkedHashMap<K, ValueWrapper>()
    private val removedKeys = hashSetOf<K>()
    private var isCleanRequested = false

    @get:Synchronized
    override val keys: Collection<K>
        get() = if (isCleanRequested) inMemoryStorage.keys else (origin.keys - removedKeys) + inMemoryStorage.keys

    @Synchronized
    override fun resetInMemoryChanges() {
        isCleanRequested = false
        inMemoryStorage.clear()
        removedKeys.clear()
    }

    @Synchronized
    override fun clean() {
        inMemoryStorage.clear()
        removedKeys.clear()
        isCleanRequested = true
    }

    @Synchronized
    override fun flush(memoryCachesOnly: Boolean) {
        if (isCleanRequested) {
            origin.clean()
        } else {
            for (key in removedKeys) {
                origin.remove(key)
            }
        }
        for ((key, valueWrapper) in inMemoryStorage) {
            when (valueWrapper) {
                is ValueWrapper.Value<*> -> origin[key] = valueWrapper.value.cast()
                // if we were appending the value and didn't access it,
                // then we have it as an append chain, so merge it and append to the origin as a single value
                is ValueWrapper.AppendChain<*> -> origin.append(key, getMergedValue(key, valueWrapper, false)).also {
                    origin[key] // trigger chunks compaction
                }
            }
        }

        resetInMemoryChanges()

        origin.flush(memoryCachesOnly)
    }

    @Synchronized
    override fun close() {
        origin.close()
    }

    @Synchronized
    override fun append(key: K, value: V) {
        /*
         * Plain English explanation:
         * 1. The key's value is present only in origin => appendToOrigin = true
         * 2. The key's value was set in this wrapper => appendToOrigin = false
         * 3. The key's value was appended but not set in this wrapper => appendToOrigin = true
         */
        check(valueExternalizer is AppendableDataExternalizer<V>) {
            "`valueExternalizer` should implement the `AppendableDataExternalizer` interface to be able to call `append`"
        }
        val currentWrapper = inMemoryStorage[key]
        if (currentWrapper is ValueWrapper.AppendChain<*>) {
            (currentWrapper.parts.cast<MutableList<V>>()).add(value)
            return
        }

        val newWrapper = when (currentWrapper) {
            is ValueWrapper.Value<*> -> ValueWrapper.AppendChain(mutableListOf(currentWrapper.value.cast(), value), false)
            // if `append` is called for the first time, assume it will be called more, so don't store it as `ValueWrapper.Value`
            else -> ValueWrapper.AppendChain(mutableListOf(value), true)
        }

        inMemoryStorage[key] = newWrapper
    }

    @Synchronized
    override fun remove(key: K) {
        removedKeys.add(key)
        inMemoryStorage.remove(key)
    }

    @Synchronized
    override fun set(key: K, value: V) {
        inMemoryStorage[key] = ValueWrapper.Value(value)
    }

    @Synchronized
    override fun get(key: K): V? {
        val wrapper = inMemoryStorage[key]
        return when {
            wrapper is ValueWrapper.Value<*> -> wrapper.value.cast<V>()
            wrapper is ValueWrapper.AppendChain<*> -> getMergedValue(key, wrapper).also { mergedValue ->
                inMemoryStorage[key] = ValueWrapper.Value(mergedValue)
            }
            key !in removedKeys -> origin[key]
            else -> null
        }
    }

    @Synchronized
    override fun contains(key: K) = key in inMemoryStorage || (key !in removedKeys && key in origin)

    /**
     * Merges a value for a [key] from [origin] if it isn't in [removedKeys] and [useOriginValue] != false with [ValueWrapper.AppendChain] and returns the merged value
     */
    private fun getMergedValue(key: K, wrapper: ValueWrapper, useOriginValue: Boolean = true): V {
        check(wrapper !is ValueWrapper.Value<*>) {
            "There's no need to merge values for $key"
        }
        check(valueExternalizer is AppendableDataExternalizer<V>) {
            "`valueExternalizer` should implement the `AppendableDataExternalizer` interface to be able to handle `append`"
        }
        return when (wrapper) {
            is ValueWrapper.AppendChain<*> -> {
                fun merge(acc: V, append: V) = valueExternalizer.append(acc, append)

                val initial = if (useOriginValue && wrapper.appendToOrigin) {
                    listOfNotNull(getOriginValue(key)).fold(valueExternalizer.createNil(), ::merge)
                } else {
                    valueExternalizer.createNil()
                }
                (wrapper.parts.cast<MutableList<V>>()).fold(initial, ::merge)
            }
            else -> error("In-memory storage contains no value for $key")
        }
    }

    private fun getOriginValue(key: K): V? = if (key !in removedKeys) {
        origin[key]
    } else {
        null
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any?.cast() = this as T

    private sealed interface ValueWrapper {
        class Value<V>(val value: V) : ValueWrapper

        class AppendChain<V>(val parts: MutableList<V>, val appendToOrigin: Boolean) : ValueWrapper
    }
}