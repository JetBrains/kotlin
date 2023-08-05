/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental.storage

import java.io.File

interface LazyStorage<K, V> {
    val storageFile: File
    val keys: Collection<K>
    operator fun contains(key: K): Boolean
    operator fun get(key: K): V?
    operator fun set(key: K, value: V)
    fun remove(key: K)
    fun clean()
    fun flush(memoryCachesOnly: Boolean)
    fun close()
}

interface AppendableLazyStorage<K, V> : LazyStorage<K, V> {
    fun append(key: K, value: V)
}

/**
 * [LazyStorage] which maps [KEY] to [VALUE] as viewed by the users of this class, but internally maintains a private [LazyStorage] which
 * maps [INTERNAL_KEY] to [INTERNAL_VALUE].
 *
 * The users of this class need to provide the transformations from [KEY] to [INTERNAL_KEY], [VALUE] to [INTERNAL_VALUE], and vice versa.
 */
abstract class LazyStorageWrapper<KEY, VALUE, INTERNAL_KEY, INTERNAL_VALUE>(
    private val storage: LazyStorage<INTERNAL_KEY, INTERNAL_VALUE>,
    private val publicToInternalKey: (KEY) -> INTERNAL_KEY,
    private val internalToPublicKey: (INTERNAL_KEY) -> KEY,
    private val publicToInternalValue: (VALUE) -> INTERNAL_VALUE,
    private val internalToPublicValue: (INTERNAL_VALUE) -> VALUE,
) : LazyStorage<KEY, VALUE> {

    override val storageFile: File
        get() = storage.storageFile

    override val keys: Collection<KEY>
        get() = storage.keys.map { internalToPublicKey(it) }

    override fun contains(key: KEY): Boolean =
        storage.contains(publicToInternalKey(key))

    override fun get(key: KEY): VALUE? =
        storage[publicToInternalKey(key)]?.let { internalToPublicValue(it) }

    override fun set(key: KEY, value: VALUE) {
        storage[publicToInternalKey(key)] = publicToInternalValue(value)
    }

    override fun remove(key: KEY) {
        storage.remove(publicToInternalKey(key))
    }

    override fun clean() {
        storage.clean()
    }

    override fun flush(memoryCachesOnly: Boolean) {
        storage.flush(memoryCachesOnly)
    }

    override fun close() {
        storage.close()
    }
}

abstract class AppendableLazyStorageWrapper<KEY, VALUE, INTERNAL_KEY, INTERNAL_VALUE>(
    private val storage: AppendableLazyStorage<INTERNAL_KEY, INTERNAL_VALUE>,
    private val publicToInternalKey: (KEY) -> INTERNAL_KEY,
    internalToPublicKey: (INTERNAL_KEY) -> KEY,
    private val publicToInternalValue: (VALUE) -> INTERNAL_VALUE,
    internalToPublicValue: (INTERNAL_VALUE) -> VALUE,
) : LazyStorageWrapper<KEY, VALUE, INTERNAL_KEY, INTERNAL_VALUE>(
    storage,
    publicToInternalKey,
    internalToPublicKey,
    publicToInternalValue,
    internalToPublicValue
), AppendableLazyStorage<KEY, VALUE> {

    override fun append(key: KEY, value: VALUE) {
        storage.append(publicToInternalKey(key), publicToInternalValue(value))
    }
}
