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
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentHashMap
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.utils.ThreadSafe
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File

/**
 * [PersistentStorage] which delegates operations to a [PersistentHashMap]. Note that the [PersistentHashMap] is created lazily (only when
 * required).
 */
@ThreadSafe
open class LazyStorage<KEY, VALUE>(
    override val storageFile: File,
    private val keyDescriptor: KeyDescriptor<KEY>,
    private val valueExternalizer: DataExternalizer<VALUE>,
) : PersistentStorage<KEY, VALUE> {

    private var storage: PersistentHashMap<KEY, VALUE>? = null

    // Use this property to minimize I/O
    private val isStorageFileExist: Boolean by lazy {
        storageFile.exists()
    }

    private fun getStorageIfExists(): PersistentHashMap<KEY, VALUE>? {
        return storage ?: when {
            isStorageFileExist -> createMap().also { storage = it }
            else -> null
        }
    }

    protected fun getStorageOrCreateNew(): PersistentHashMap<KEY, VALUE> {
        return storage ?: createMap().also { storage = it }
    }

    private fun createMap() = PersistentHashMap(storageFile, keyDescriptor, valueExternalizer)

    @get:Synchronized
    override val keys: Set<KEY>
        get() = buildSet {
            getStorageIfExists()?.processKeysWithExistingMapping(CommonProcessors.CollectProcessor(this))
        }

    @Synchronized
    override fun contains(key: KEY): Boolean =
        getStorageIfExists()?.containsMapping(key) ?: false

    @Synchronized
    override fun get(key: KEY): VALUE? =
        getStorageIfExists()?.get(key)

    @Synchronized
    override fun set(key: KEY, value: VALUE) {
        getStorageOrCreateNew().put(key, value)
    }

    @Synchronized
    override fun remove(key: KEY) {
        getStorageIfExists()?.remove(key)
    }

    @Synchronized
    override fun flush() {
        storage?.force()
    }

    @Synchronized
    override fun close() {
        storage?.close()
    }

}

/** [LazyStorage] where a map entry's value is a [Collection] of elements of type [E]. */
@ThreadSafe
class AppendableLazyStorage<KEY, E>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    elementExternalizer: DataExternalizer<E>,
) : LazyStorage<KEY, Collection<E>>(storageFile, keyDescriptor, AppendableCollectionExternalizer(elementExternalizer)),
    AppendablePersistentStorage<KEY, E> {

    private val appendableCollectionExternalizer = AppendableCollectionExternalizer(elementExternalizer)

    @Synchronized
    override fun append(key: KEY, elements: Collection<E>) {
        getStorageOrCreateNew().appendData(
            key,
            AppendablePersistentMap.ValueDataAppender { appendableCollectionExternalizer.append(it, elements) }
        )
    }
}

/**
 * [DataExternalizer] for a [Collection] of elements of type [E].
 *
 * IMPORTANT: It is a *private* class because it is meant to be used only with a [PersistentHashMap] (e.g., the [read] method reads until
 * the stream ends, [append] can be called multiple times and its implementation is identical to [save] -- these only work with a
 * [PersistentHashMap]).
 */
private class AppendableCollectionExternalizer<E>(
    private val elementExternalizer: DataExternalizer<E>,
) : DataExternalizer<Collection<E>> {

    fun append(output: DataOutput, elements: Collection<E>) {
        save(output, elements)
    }

    override fun save(output: DataOutput, value: Collection<E>) {
        value.forEach { elementExternalizer.save(output, it) }
    }

    override fun read(input: DataInput): Collection<E> {
        val result = ArrayList<E>()
        val stream = input as DataInputStream

        while (stream.available() > 0) {
            result.add(elementExternalizer.read(stream))
        }

        return result
    }
}

fun <KEY, VALUE> createPersistentStorage(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    valueExternalizer: DataExternalizer<VALUE>,
    icContext: IncrementalCompilationContext,
): PersistentStorage<KEY, VALUE> {
    return LazyStorage(storageFile, keyDescriptor, valueExternalizer).let { storage ->
        if (icContext.icFeatures.keepIncrementalCompilationCachesInMemory) {
            InMemoryStorage(storage).also {
                icContext.transaction.registerInMemoryStorageWrapper(it)
            }
        } else {
            storage
        }
    }
}

fun <KEY, E> createAppendablePersistentStorage(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    elementExternalizer: DataExternalizer<E>,
    icContext: IncrementalCompilationContext,
): AppendablePersistentStorage<KEY, E> {
    return AppendableLazyStorage(storageFile, keyDescriptor, elementExternalizer).let { storage ->
        if (icContext.icFeatures.keepIncrementalCompilationCachesInMemory) {
            AppendableInMemoryStorage(storage).also {
                icContext.transaction.registerInMemoryStorageWrapper(it)
            }
        } else {
            storage
        }
    }
}
