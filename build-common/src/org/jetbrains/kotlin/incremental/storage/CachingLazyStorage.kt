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

import com.intellij.util.CommonProcessors
import com.intellij.util.io.AppendablePersistentMap
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File
import java.io.IOException


/**
 * It's lazy in a sense that PersistentHashMap is created only on write
 */
class CachingLazyStorage<K, V>(
    private val storageFile: File,
    private val keyDescriptor: KeyDescriptor<K>,
    private val valueExternalizer: DataExternalizer<V>
) : AppendableLazyStorage<K, V> {
    private var storage: PersistentHashMap<K, V>? = null
    private var isStorageFileExist = true

    private fun getStorageIfExists(): PersistentHashMap<K, V>? {
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
        if (storage == null) {
            storage = createMap()
        }
        return storage!!
    }

    override val keys: Collection<K>
        @Synchronized
        get() = buildList {
            getStorageIfExists()?.processKeysWithExistingMapping(CommonProcessors.CollectProcessor(this))
        }

    @Synchronized
    override operator fun contains(key: K): Boolean =
        getStorageIfExists()?.containsMapping(key) ?: false

    @Synchronized
    override operator fun get(key: K): V? =
        getStorageIfExists()?.get(key)

    @Synchronized
    override operator fun set(key: K, value: V) {
        getStorageOrCreateNew().put(key, value)
    }

    @Synchronized
    override fun remove(key: K) {
        getStorageIfExists()?.remove(key)
    }

    @Synchronized
    override fun append(key: K, value: V) {
        check(valueExternalizer is AppendableDataExternalizer<*>) {
            "`valueExternalizer` should implement the `AppendableDataExternalizer` interface to be able to call `append`"
        }
        getStorageOrCreateNew().appendData(key, AppendablePersistentMap.ValueDataAppender { valueExternalizer.save(it, value) })
    }

    @Synchronized
    override fun clean() {
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
        try {
            storage?.close()
        } finally {
            storage = null
        }
    }

    private fun createMap(): PersistentHashMap<K, V> = PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)
}

private fun <K, V> createLazyStorageImpl(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: DataExternalizer<V>,
    icContext: IncrementalCompilationContext,
) = CachingLazyStorage(storageFile, keyDescriptor, valueExternalizer).let {
    if (icContext.keepIncrementalCompilationCachesInMemory) {
        DefaultInMemoryStorageWrapper(it, valueExternalizer).also { wrapper ->
            icContext.transaction.registerInMemoryStorageWrapper(wrapper)
        }
    } else {
        it
    }
}

fun <K, V> createLazyStorage(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: DataExternalizer<V>,
    icContext: IncrementalCompilationContext,
): LazyStorage<K, V> = createLazyStorageImpl(storageFile, keyDescriptor, valueExternalizer, icContext)

fun <K, V> createLazyStorage(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: AppendableDataExternalizer<V>,
    icContext: IncrementalCompilationContext,
): AppendableLazyStorage<K, V> = createLazyStorageImpl(storageFile, keyDescriptor, valueExternalizer, icContext)