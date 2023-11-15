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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.utils.Printer
import java.io.File

/** [PersistentStorage] that provides a few extra utility methods. */
interface BasicMap<KEY, VALUE> : PersistentStorage<KEY, VALUE> {

    /** Removes all entries. */
    fun clear() {
        synchronized(this) {
            keys.forEach { remove(it) }
        }
    }

    /**
     * Deletes [storageFile] or a group of files associated with [storageFile] (e.g., an implementation of [PersistentStorage] may use a
     * [com.intellij.util.io.PersistentHashMap], which creates files such as "storageFile.tab", "storageFile.tab.len", etc.).
     *
     * Make sure the storage has been closed first before calling this method.
     */
    fun deleteStorageFiles() {
        synchronized(this) {
            check(IOUtil.deleteAllFilesStartingWith(storageFile)) {
                "Unable to delete storage file(s) with name prefix: ${storageFile.path}"
            }
        }
    }

    @TestOnly
    fun dump(): String {
        val map: Map<KEY, VALUE> = synchronized(this) {
            keys.associateWith { this[it]!! }
        }
        val printableMap: Map<String, String> = map.map {
            dumpKey(it.key) to dumpValue(it.value)
        }.sortedBy { it.first }.toMap()

        return StringBuilder().apply {
            Printer(this).apply {
                println("${storageFile.name.substringBefore(".tab")} (${this@BasicMap::class.java.simpleName})")
                pushIndent()
                printableMap.forEach {
                    println("${it.key} -> ${it.value}")
                }
                popIndent()
            }
        }.toString()
    }

    @TestOnly
    fun dumpKey(key: KEY): String = key.toString()

    @TestOnly
    fun dumpValue(value: VALUE): String {
        return if (value is Collection<*>) {
            // Sort the elements so that we can reliably compare `Collection`s in tests (in case the order of the elements is not stable).
            value.sortedBy { it.toString() }.toString()
        } else {
            value.toString()
        }
    }
}

abstract class BasicMapBase<KEY, VALUE>(
    // TODO(KT-63456): Remove `protected val` (currently this property is still used by BasicStringMap)
    protected val persistentStorage: PersistentStorage<KEY, VALUE>,
) : PersistentStorageWrapper<KEY, VALUE>(persistentStorage), BasicMap<KEY, VALUE>

abstract class AppendableBasicMapBase<KEY, E>(
    storage: AppendablePersistentStorage<KEY, E>,
) : AppendablePersistentStorageWrapper<KEY, E>(storage), BasicMap<KEY, Collection<E>>

abstract class AbstractBasicMap<KEY, VALUE>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    valueExternalizer: DataExternalizer<VALUE>,
    icContext: IncrementalCompilationContext,
) : BasicMapBase<KEY, VALUE>(
    createPersistentStorage(storageFile, keyDescriptor, valueExternalizer, icContext)
)

abstract class AppendableBasicMap<KEY, E>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    elementExternalizer: DataExternalizer<E>,
    icContext: IncrementalCompilationContext,
) : AppendableBasicMapBase<KEY, E>(
    createAppendablePersistentStorage(storageFile, keyDescriptor, elementExternalizer, icContext)
)

abstract class AppendableSetBasicMap<KEY, E>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    elementExternalizer: DataExternalizer<E>,
    icContext: IncrementalCompilationContext,
) : PersistentStorage<KEY, Set<E>>, BasicMap<KEY, Set<E>> {

    private val storage = createAppendablePersistentStorage(storageFile, keyDescriptor, elementExternalizer, icContext)

    override val storageFile: File = storage.storageFile

    @get:Synchronized
    override val keys: Set<KEY>
        get() = storage.keys

    @Synchronized
    override fun contains(key: KEY): Boolean =
        storage.contains(key)

    @Synchronized
    override fun get(key: KEY): Set<E>? {
        // Note: To optimize this getter, consider changing the type of `storage` from PersistentStorage<KEY, Collection<E>> to
        // PersistentStorage<KEY, Set<E>> so that we don't have to call `toSet()`. The downside is that it will make the code more complex,
        // so we'll do it only if it's necessary.
        return storage[key]?.toSet()
    }

    @Synchronized
    override fun set(key: KEY, value: Set<E>) {
        storage[key] = value
    }

    @Synchronized
    override fun remove(key: KEY) {
        storage.remove(key)
    }

    @Synchronized
    override fun flush() {
        storage.flush()
    }

    @Synchronized
    override fun close() {
        storage.close()
    }

    @Synchronized
    fun append(key: KEY, element: E) {
        storage.append(key, setOf(element))
    }

    @Synchronized
    fun append(key: KEY, elements: Set<E>) {
        storage.append(key, elements)
    }
}

abstract class BasicStringMap<VALUE>(
    storageFile: File,
    valueExternalizer: DataExternalizer<VALUE>,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<String, VALUE>(
    storageFile,
    StringExternalizer.toDescriptor(),
    valueExternalizer,
    icContext
) {
    // TODO(KT-63456): Remove this property
    // (To do this, we need to refactor all subclasses of BasicStringMap such that they don't use this property. For examples of what the
    // outcome of the refactoring should look like, see other subclasses of AbstractBasicMap.)
    protected val storage: PersistentStorage<String, VALUE> = persistentStorage
}
