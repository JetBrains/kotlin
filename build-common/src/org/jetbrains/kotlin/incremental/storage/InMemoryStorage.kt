/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.utils.ThreadSafe

/**
 * [PersistentStorage] which reads from another underlying [PersistentStorage], keeps all changes to it in memory, and writes the changes
 * back to the underlying storage on [applyChanges], [flush], or [close].
 */
interface InMemoryStorageInterface<KEY, VALUE> : PersistentStorage<KEY, VALUE> {

    /**
     * Applies in-memory changes to the underlying [PersistentStorage], then calls [clearChanges].
     *
     * Note that the changes are only propagated to the underlying [PersistentStorage], they may not be written to [storageFile] yet. If
     * you want to propagate the changes to [storageFile], call [flush] instead.
     */
    fun applyChanges()

    /** Removes all in-memory changes. */
    fun clearChanges()
}

/** See [InMemoryStorageInterface]. */
@ThreadSafe
open class InMemoryStorage<KEY, VALUE>(
    private val storage: PersistentStorage<KEY, VALUE>,
) : InMemoryStorageInterface<KEY, VALUE> {

    override val storageFile = storage.storageFile

    // The following properties store changes to `storage`.
    // Note that:
    //   - The keys across these groups must be mutually exclusive.
    //   - `addedEntries.keys` must be outside `storage.keys`.
    //   - `modifiedEntries.keys`, `appendedEntries.keys`, and `removedKeys` must be subsets of `storage.keys`.
    //   - `appendedEntries` is meant to be used only by the subclass `AppendableInMemoryStorage`, but it is in this class because we want
    //     to share code as much as possible.
    protected val addedEntries = LinkedHashMap<KEY, VALUE>()
    protected val modifiedEntries = LinkedHashMap<KEY, VALUE>()
    protected val appendedEntries = LinkedHashMap<KEY, VALUE>()
    protected val removedKeys = LinkedHashSet<KEY>()

    @get:Synchronized
    override val keys: Set<KEY>
        get() = storage.keys + addedEntries.keys - removedKeys

    @Synchronized
    override fun contains(key: KEY): Boolean = when (key) {
        in addedEntries -> true
        in modifiedEntries -> true
        in appendedEntries -> true
        in removedKeys -> false
        else -> key in storage
    }

    @Synchronized
    override fun get(key: KEY): VALUE? =
        addedEntries[key]
            ?: modifiedEntries[key]
            // appendedEntries is handled by AppendableInMemoryStorage#get
            ?: when (key) {
                in removedKeys -> null
                else -> storage[key]
            }

    @Synchronized
    override fun set(key: KEY, value: VALUE) = when (key) {
        in addedEntries -> addedEntries[key] = value
        in modifiedEntries -> modifiedEntries[key] = value
        in appendedEntries -> {
            appendedEntries.remove(key)
            modifiedEntries[key] = value
        }
        in removedKeys -> {
            removedKeys.remove(key)
            modifiedEntries[key] = value
        }
        in storage -> modifiedEntries[key] = value
        else -> addedEntries[key] = value
    }

    @Synchronized
    override fun remove(key: KEY) {
        when (key) {
            in addedEntries -> addedEntries.remove(key)
            in modifiedEntries -> {
                modifiedEntries.remove(key)
                removedKeys.add(key)
            }
            in appendedEntries -> {
                appendedEntries.remove(key)
                removedKeys.add(key)
            }
            in removedKeys -> Unit
            in storage -> removedKeys.add(key)
            else -> Unit
        }
    }

    @Synchronized
    override fun applyChanges() {
        addedEntries.forEach {
            storage[it.key] = it.value
        }
        modifiedEntries.forEach {
            storage[it.key] = it.value
        }
        // appendedEntries is handled by AppendableInMemoryStorage#applyChanges
        removedKeys.forEach {
            storage.remove(it)
        }
        clearChanges()
    }

    @Synchronized
    override fun clearChanges() {
        addedEntries.clear()
        modifiedEntries.clear()
        appendedEntries.clear()
        removedKeys.clear()
    }

    @Synchronized
    override fun flush() {
        applyChanges()
        storage.flush()
    }

    @Synchronized
    override fun close() {
        applyChanges()
        storage.close()
    }

}

/** [InMemoryStorage] where a map entry's value is a [Collection] of elements of type [E]. */
@ThreadSafe
class AppendableInMemoryStorage<KEY, E>(
    private val storage: AppendablePersistentStorage<KEY, E>,
) : InMemoryStorage<KEY, Collection<E>>(storage), AppendablePersistentStorage<KEY, E> {

    @Synchronized
    override fun get(key: KEY): Collection<E>? = when (key) {
        in appendedEntries -> storage[key]!! + appendedEntries[key]!!
        else -> super.get(key)
    }

    @Synchronized
    override fun append(key: KEY, elements: Collection<E>) = when (key) {
        in addedEntries -> addedEntries[key] = addedEntries[key]!! + elements
        in modifiedEntries -> modifiedEntries[key] = modifiedEntries[key]!! + elements
        in appendedEntries -> appendedEntries[key] = appendedEntries[key]!! + elements
        in removedKeys -> {
            removedKeys.remove(key)
            // Note: We update `modifiedEntries` (not `appendedEntries`), because if the entry is removed and then appended, it is
            // equivalent to being modified (not appended)
            modifiedEntries[key] = elements
        }
        in storage -> appendedEntries[key] = elements
        else -> addedEntries[key] = elements
    }

    @Synchronized
    override fun applyChanges() {
        appendedEntries.forEach {
            storage.append(it.key, it.value)
        }
        appendedEntries.clear()
        super.applyChanges()
    }

}
