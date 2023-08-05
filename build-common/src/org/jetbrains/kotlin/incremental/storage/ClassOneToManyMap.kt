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

import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal open class ClassOneToManyMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AppendableLazyStorageWrapper<FqName, Collection<FqName>, String, Collection<String>>(
    storage = createAppendableLazyStorage(
        storageFile,
        EnumeratorStringDescriptor.INSTANCE,
        AppendableCollectionExternalizer(StringExternalizer) { linkedSetOf() },
        icContext
    ),
    publicToInternalKey = FqName::asString,
    internalToPublicKey = ::FqName,
    publicToInternalValue = { it.mapTo(linkedSetOf(), FqName::asString) },
    internalToPublicValue = { it.mapTo(linkedSetOf(), ::FqName) },
), BasicMap<FqName, Collection<FqName>> {

    @Synchronized
    override operator fun set(key: FqName, value: Collection<FqName>) {
        if (value.isNotEmpty()) {
            super.set(key, value)
        } else {
            remove(key)
        }
    }

    @Synchronized
    fun add(key: FqName, value: FqName) {
        append(key, listOf(value))
    }

    // Access to caches could be done from multiple threads (e.g. JPS worker and RMI). The underlying collection is already synchronized,
    // but we still need synchronization of this method and all read/write methods.
    @Synchronized
    fun removeValues(key: FqName, removed: Set<FqName>) {
        this[key] = this[key].orEmpty() - removed
    }

    @TestOnly
    override fun dumpValue(value: Collection<FqName>): String = value.map(FqName::asString).dumpCollection()
}

internal class SubtypesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : ClassOneToManyMap(storageFile, icContext)

internal class SupertypesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : ClassOneToManyMap(storageFile, icContext)
