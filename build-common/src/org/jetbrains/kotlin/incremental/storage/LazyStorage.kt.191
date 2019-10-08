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
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import java.io.DataOutput
import java.io.File
import java.io.IOException


/**
 * It's lazy in a sense that PersistentHashMap is created only on write
 */
class LazyStorage<K, V>(
        private val storageFile: File,
        private val keyDescriptor: KeyDescriptor<K>,
        private val valueExternalizer: DataExternalizer<V>
) {
    @Volatile
    private var storage: PersistentHashMap<K, V>? = null

    @Synchronized
    private fun getStorageIfExists(): PersistentHashMap<K, V>? {
        if (storage != null) return storage

        if (storageFile.exists()) {
            storage = createMap()
            return storage
        }

        return null
    }

    @Synchronized
    private fun getStorageOrCreateNew(): PersistentHashMap<K, V> {
        if (storage == null) {
            storage = createMap()
        }

        return storage!!
    }

    val keys: Collection<K>
        get() = getStorageIfExists()?.allKeysWithExistingMapping ?: listOf()

    operator fun contains(key: K): Boolean =
            getStorageIfExists()?.containsMapping(key) ?: false

    operator fun get(key: K): V? =
            getStorageIfExists()?.get(key)

    operator fun set(key: K, value: V) {
        getStorageOrCreateNew().put(key, value)
    }

    fun remove(key: K) {
        getStorageIfExists()?.remove(key)
    }

    fun append(key: K, value: String) {
        append(key) { out -> IOUtil.writeUTF(out, value) }
    }

    fun append(key: K, value: Int) {
        append(key) { out -> out.writeInt(value) }
    }

    @Synchronized
    fun clean() {
        try {
            storage?.close()
        }
        catch (ignored: Throwable) {
        }

        PersistentHashMap.deleteFilesStartingWith(storageFile)
        storage = null
    }

    @Synchronized
    fun flush(memoryCachesOnly: Boolean) {
        val existingStorage = storage ?: return

        if (memoryCachesOnly) {
            if (existingStorage.isDirty) {
                existingStorage.dropMemoryCaches()
            }
        }
        else {
            existingStorage.force()
        }
    }

    @Synchronized
    fun close() {
        storage?.close()
    }

    private fun createMap(): PersistentHashMap<K, V> =
            PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)

    private fun append(key: K, append: (DataOutput)->Unit) {
        getStorageOrCreateNew().appendData(key, append)
    }
}
