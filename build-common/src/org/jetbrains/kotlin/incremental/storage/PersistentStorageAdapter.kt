/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.utils.ThreadSafe
import java.io.File

/**
 * [PersistentStorage] which maps [KEY] to [VALUE] as viewed by the users of this class, but delegates operations to another [storage] which
 * maps [INTERNAL_KEY] to [INTERNAL_VALUE].
 *
 * The users of this class need to provide the transformations from [KEY] to [INTERNAL_KEY], [VALUE] to [INTERNAL_VALUE], and vice versa.
 */
@ThreadSafe
abstract class PersistentStorageAdapter<KEY, VALUE, INTERNAL_KEY, INTERNAL_VALUE>(
    private val storage: PersistentStorage<INTERNAL_KEY, INTERNAL_VALUE>,
    private val publicToInternalKey: (KEY) -> INTERNAL_KEY,
    private val internalToPublicKey: (INTERNAL_KEY) -> KEY,
    private val publicToInternalValue: (VALUE) -> INTERNAL_VALUE,
    private val internalToPublicValue: (INTERNAL_VALUE) -> VALUE,
) : PersistentStorage<KEY, VALUE>, BasicMap<KEY, VALUE> {

    override val storageFile: File = storage.storageFile

    @get:Synchronized
    override val keys: Set<KEY>
        get() = storage.keys.mapTo(LinkedHashSet()) { internalToPublicKey(it) }

    @Synchronized
    override fun contains(key: KEY): Boolean =
        storage.contains(publicToInternalKey(key))

    @Synchronized
    override fun get(key: KEY): VALUE? =
        storage[publicToInternalKey(key)]?.let { internalToPublicValue(it) }

    @Synchronized
    override fun set(key: KEY, value: VALUE) {
        storage[publicToInternalKey(key)] = publicToInternalValue(value)
    }

    @Synchronized
    override fun remove(key: KEY) {
        storage.remove(publicToInternalKey(key))
    }

    @Synchronized
    override fun flush() {
        storage.close()
    }

    @Synchronized
    override fun close() {
        storage.close()
    }
}

/** [PersistentStorageAdapter] where a map entry's value is a [Collection]. */
@ThreadSafe
abstract class AppendablePersistentStorageAdapter<KEY, E, VALUE : Collection<E>, INTERNAL_KEY, INTERNAL_E, INTERNAL_VALUE : Collection<INTERNAL_E>>(
    private val storage: AppendablePersistentStorage<INTERNAL_KEY, INTERNAL_E, INTERNAL_VALUE>,
    private val publicToInternalKey: (KEY) -> INTERNAL_KEY,
    internalToPublicKey: (INTERNAL_KEY) -> KEY,
    private val publicToInternalValue: (VALUE) -> INTERNAL_VALUE,
    internalToPublicValue: (INTERNAL_VALUE) -> VALUE,
) : PersistentStorageAdapter<KEY, VALUE, INTERNAL_KEY, INTERNAL_VALUE>(
    storage,
    publicToInternalKey,
    internalToPublicKey,
    publicToInternalValue,
    internalToPublicValue
), AppendablePersistentStorage<KEY, E, VALUE> {

    @Synchronized
    override fun append(key: KEY, elements: VALUE) {
        storage.append(publicToInternalKey(key), publicToInternalValue(elements))
    }
}
