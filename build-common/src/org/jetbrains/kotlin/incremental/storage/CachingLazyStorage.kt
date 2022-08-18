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
    L, //CLOSE
    M, //CREATE_MAP,
    O, //FORCE
    D,  //DROP_MEMORY_CACHES
    C, //CLEAN
    A, //APPEND
    R, //REMOVE
    S, //SET
    G, //GET
    T, //CONTAINS
    K, //KEYS
    N, //GET_STORAGE_OR_CREATE_NEW
    E, //GET_STORAGE_IF_EXISTS
    F //FLUSH
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
        carefulAdd(E,events)

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
        carefulAdd(N,events)
        if (storage == null) {
            storage = createMap()
        }
        return storage!!
    }

    override val keys: Collection<K>
        @Synchronized
        get() {
            carefulAdd(K,events)
            return getStorageIfExists()?.allKeysWithExistingMapping ?: listOf()
        }

    @Synchronized
    override operator fun contains(key: K): Boolean {
        carefulAdd(T,events)
        return getStorageIfExists()?.containsMapping(key) ?: false
    }

    @Synchronized
    override operator fun get(key: K): V? {
        carefulAdd(G,events)
        return getStorageIfExists()?.get(key)
    }

    @Synchronized
    override operator fun set(key: K, value: V) {
        carefulAdd(S,events)
        getStorageOrCreateNew().put(key, value)
    }

    @Synchronized
    override fun remove(key: K) {
        carefulAdd(R,events)
        getStorageIfExists()?.remove(key)
    }

    @Synchronized
    override fun append(key: K, value: V) {
        carefulAdd(A,events)
        getStorageOrCreateNew().appendData(key, { valueExternalizer.save(it, value) })
    }

    @Synchronized
    override fun clean() {
        carefulAdd(C,events)
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
        carefulAdd(F,events)
        val existingStorage = storage ?: return

        if (memoryCachesOnly) {
            if (existingStorage.isDirty) {
                carefulAdd(D,events)
                existingStorage.dropMemoryCaches()
            }
        } else {
            carefulAdd(O,events)
            existingStorage.force()
        }
    }

    @Synchronized
    override fun close() {
        carefulAdd(L,events)
        dropEventsToLog()
        try {
            storage?.close()
        } finally {
            storage = null
        }
    }

    private fun createMap(): PersistentHashMap<K, V> {
        carefulAdd(M, events)
        return PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
    }

    @Synchronized
    fun carefulAdd(event:StorageEvents, eventsList: MutableList<StorageEvents>) {

        if(eventsList.size > 1000) {
            dropEventsToLog()
        }
        eventsList.add(event)
    }

    fun dropEventsToLog() {
        val fileKey = storageFile.absolutePath.substringAfter("/targets/")
        val eventsValue = events.toString().replace(", ", "")
        LOG.info(">>>$fileKey:$eventsValue")
        events.clear()
    }
}
