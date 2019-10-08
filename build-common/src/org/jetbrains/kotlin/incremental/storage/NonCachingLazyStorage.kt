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

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.JpsPersistentHashMap
import java.io.File


class NonCachingLazyStorage<K, V>(
    private val storageFile: File,
    private val keyDescriptor: KeyDescriptor<K>,
    private val valueExternalizer: DataExternalizer<V>
) : LazyStorage<K, V> {
    @Volatile
    private var storage: JpsPersistentHashMap<K, V>? = null

    @Synchronized
    private fun getStorageIfExists(): JpsPersistentHashMap<K, V>? {
        if (storage != null) return storage

        if (storageFile.exists()) {
            storage = createMap()
            return storage
        }

        return null
    }

    @Synchronized
    private fun getStorageOrCreateNew(): JpsPersistentHashMap<K, V> {
        if (storage == null) {
            storage = createMap()
        }

        return storage!!
    }

    override val keys: Collection<K>
        get() = getStorageIfExists()?.allKeysWithExistingMapping ?: listOf()

    override operator fun contains(key: K): Boolean =
        getStorageIfExists()?.containsMapping(key) ?: false

    override operator fun get(key: K): V? =
        getStorageIfExists()?.get(key)

    override operator fun set(key: K, value: V) {
        getStorageOrCreateNew().put(key, value)
    }

    override fun remove(key: K) {
        getStorageIfExists()?.remove(key)
    }

    override fun append(key: K, value: V) {
        getStorageOrCreateNew().appendDataWithoutCache(key, value)
    }

    @Synchronized
    override fun clean() {
        try {
            storage?.close()
        } catch (ignored: Throwable) {
        }

        JpsPersistentHashMap.deleteFilesStartingWith(storageFile)
        storage = null
    }

    @Synchronized
    override fun flush(memoryCachesOnly: Boolean) {
        val existingStorage = storage ?: return

        if (memoryCachesOnly) {
            if (existingStorage.isDirty) {
                existingStorage.dropMemoryCaches()
            }
        } else {
            existingStorage.force()
        }
    }

    @Synchronized
    override fun close() {
        storage?.close()
    }

    private fun createMap(): JpsPersistentHashMap<K, V> =
        JpsPersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
}
