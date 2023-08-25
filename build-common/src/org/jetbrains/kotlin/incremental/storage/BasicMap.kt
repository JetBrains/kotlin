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
import com.intellij.util.io.EnumeratorStringDescriptor
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
        keys.forEach { remove(it) }
    }

    /**
     * DEPRECATED: This method should be removed because
     *   - The `memoryCachesOnly` parameter is not used (it is always `false`).
     *   - There is currently no good use case for flushing. In fact, the current implementation of this method does nothing.
     */
    fun flush(memoryCachesOnly: Boolean) {
        check(!memoryCachesOnly) { "Expected memoryCachesOnly = false but it is `true`" }
    }

    /**
     * Deletes [storageFile] or a group of files associated with [storageFile] (e.g., an implementation of [PersistentStorage] may use a
     * [com.intellij.util.io.PersistentHashMap], which creates files such as "storageFile.tab", "storageFile.tab.len", etc.).
     *
     * Make sure the storage has been closed first before calling this method.
     */
    fun deleteStorageFiles() {
        check(IOUtil.deleteAllFilesStartingWith(storageFile)) {
            "Unable to delete storage file(s) with name prefix: ${storageFile.path}"
        }
    }

    /**
     * DEPRECATED: This method should be removed because:
     *   - The method name is ambiguous (it may be confused with [clear], and it does not exactly describe the current implementation).
     *   - The method currently calls close(). However, close() is often already called separately and automatically, so calling this method
     *     means that close() will likely be called twice.
     * Instead, just inline this method or call either close() or deleteStorageFiles() directly.
     */
    fun clean() {
        close()
        deleteStorageFiles()
    }

    @TestOnly
    fun dump(): String {
        return StringBuilder().apply {
            Printer(this).apply {
                println("${storageFile.name.substringBefore(".tab")} (${this@BasicMap::class.java.simpleName})")
                pushIndent()

                for (key in keys.sortedBy { dumpKey(it) }) {
                    println("${dumpKey(key)} -> ${dumpValue(this@BasicMap[key]!!)}")
                }

                popIndent()
            }
        }.toString()
    }

    @TestOnly
    fun dumpKey(key: KEY): String = key.toString()

    @TestOnly
    fun dumpValue(value: VALUE): String = value.toString()
}

abstract class BasicMapBase<KEY, VALUE>(
    protected val storage: PersistentStorage<KEY, VALUE>,
) : PersistentStorageWrapper<KEY, VALUE>(storage), BasicMap<KEY, VALUE>

abstract class AppendableBasicMapBase<KEY, E, VALUE : Collection<E>>(
    protected val storage: AppendablePersistentStorage<KEY, E, VALUE>,
) : AppendablePersistentStorageWrapper<KEY, E, VALUE>(storage), BasicMap<KEY, VALUE>

abstract class AbstractBasicMap<KEY, VALUE>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    valueExternalizer: DataExternalizer<VALUE>,
    protected val icContext: IncrementalCompilationContext,
) : BasicMapBase<KEY, VALUE>(
    createPersistentStorage(storageFile, keyDescriptor, valueExternalizer, icContext)
) {
    protected val pathConverter
        get() = icContext.pathConverter
}

abstract class AppendableAbstractBasicMap<KEY, E, VALUE : Collection<E>>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<KEY>,
    elementExternalizer: DataExternalizer<E>,
    protected val icContext: IncrementalCompilationContext,
) : AppendableBasicMapBase<KEY, E, VALUE>(
    createAppendablePersistentStorage(storageFile, keyDescriptor, elementExternalizer, icContext)
) {
    protected val pathConverter
        get() = icContext.pathConverter
}

abstract class BasicStringMap<VALUE>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<String>,
    valueExternalizer: DataExternalizer<VALUE>,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<String, VALUE>(storageFile, keyDescriptor, valueExternalizer, icContext) {

    constructor(storageFile: File, valueExternalizer: DataExternalizer<VALUE>, icContext: IncrementalCompilationContext) :
            this(storageFile, EnumeratorStringDescriptor.INSTANCE, valueExternalizer, icContext)
}

abstract class AppendableBasicStringMap<E, VALUE : Collection<E>>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<String>,
    elementExternalizer: DataExternalizer<E>,
    icContext: IncrementalCompilationContext,
) : AppendableAbstractBasicMap<String, E, VALUE>(storageFile, keyDescriptor, elementExternalizer, icContext) {

    constructor(storageFile: File, elementExternalizer: DataExternalizer<E>, icContext: IncrementalCompilationContext) :
            this(storageFile, EnumeratorStringDescriptor.INSTANCE, elementExternalizer, icContext)
}
