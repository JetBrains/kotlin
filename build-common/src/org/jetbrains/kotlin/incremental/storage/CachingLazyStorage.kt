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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import org.jetbrains.kotlin.incremental.storage.StorageEvents.*
import java.io.File
import java.io.IOException

enum class StorageEvents {
    CLOSE, CREATE_MAP, FORCE, DROP_MEMORY_CACHES, CLEAN, APPEND, REMOVE, SET, GET, CONTAINS, KEYS, GET_STORAGE_OR_CREATE_NEW, GET_STORAGE_IF_EXISTS, FLUSH
}

/**
 * It's lazy in a sense that PersistentHashMap is created only on write
 */
class CachingLazyStorage<K, V>(
    private val storageFile: File,
    private val keyDescriptor: KeyDescriptor<K>,
    private val valueExternalizer: DataExternalizer<V>
) : LazyStorage<K, V> {
    val LOG = Logger.getInstance("#org.jetbrains.kotlin.jps.build.KotlinBuilder")
    private var storage: PersistentHashMap<K, V>? = null
    private var isStorageFileExist = true
    var events = java.util.Collections.synchronizedList(mutableListOf<StorageEvents>())

    private fun getStorageIfExists(): PersistentHashMap<K, V>? {
        events.add(GET_STORAGE_IF_EXISTS)

        if (storage != null) return storage

        if (!isStorageFileExist) return null

        if (storageFile.exists()) {
            storage = createMap()
            return storage
        }

        isStorageFileExist = false
        return null
    }

    private fun getStorageOrCreateNew(): PersistentHashMap<K, V> {
        events.add(GET_STORAGE_OR_CREATE_NEW)
        if (storage == null) {
            storage = createMap()
        }
        return storage!!
    }

    override val keys: Collection<K>
        @Synchronized
        get() {
            events.add(KEYS)
            return getStorageIfExists()?.allKeysWithExistingMapping ?: listOf()
        }

    @Synchronized
    override operator fun contains(key: K): Boolean {
        events.add(CONTAINS)
        return getStorageIfExists()?.containsMapping(key) ?: false
    }

    @Synchronized
    override operator fun get(key: K): V? {
        events.add(GET)
        return getStorageIfExists()?.get(key)
    }

    @Synchronized
    override operator fun set(key: K, value: V) {
        events.add(SET)
        getStorageOrCreateNew().put(key, value)
    }

    @Synchronized
    override fun remove(key: K) {
        events.add(REMOVE)
        getStorageIfExists()?.remove(key)
    }

    @Synchronized
    override fun append(key: K, value: V) {
        events.add(APPEND)
        getStorageOrCreateNew().appendData(key, { valueExternalizer.save(it, value) })
    }

    @Synchronized
    override fun clean() {
        events.add(CLEAN)
        try {
            storage?.close()
        } finally {
            storage = null
            if (!IOUtil.deleteAllFilesStartingWith(storageFile)) {
                throw IOException("Could not delete internal storage: ${storageFile.absolutePath}")
            }
        }
    }

    @Synchronized
    override fun flush(memoryCachesOnly: Boolean) {
        events.add(FLUSH)
        val existingStorage = storage ?: return

        if (memoryCachesOnly) {
            if (existingStorage.isDirty) {
                events.add(DROP_MEMORY_CACHES)
                existingStorage.dropMemoryCaches()
            }
        } else {
            events.add(FORCE)
            existingStorage.force()
        }
    }

    @Synchronized
    override fun close() {
        events.add(CLOSE)
        LOG.info(">>>$storageFile:$events")
        try {
            storage?.close()
        } finally {
            storage = null
            events.clear()
        }
    }

    private fun createMap(): PersistentHashMap<K, V> {
        events.add(CREATE_MAP)
        return PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
    }
}
