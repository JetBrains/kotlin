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
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.utils.Printer
import java.io.File

interface BasicMap<K, V> : LazyStorage<K, V> {

    @TestOnly
    fun dump(): String {
        return with(StringBuilder()) {
            with(Printer(this)) {
                println("${storageFile.name.substringBefore(".tab")} (${this@BasicMap::class.java.simpleName})")
                pushIndent()

                for (key in keys.sortedBy { dumpKey(it) }) {
                    println("${dumpKey(key)} -> ${dumpValue(this@BasicMap[key]!!)}")
                }

                popIndent()
            }

            this
        }.toString()
    }

    @TestOnly
    fun dumpKey(key: K): String = key.toString()

    @TestOnly
    fun dumpValue(value: V): String = value.toString()
}

abstract class AbstractBasicMap<K, V, StorageType : LazyStorage<K, V>>(
    protected val storage: StorageType,
) : BasicMap<K, V>, LazyStorage<K, V> by storage

abstract class AbstractAppendableBasicMap<K, V, StorageType : AppendableLazyStorage<K, V>>(
    storage: StorageType,
) : BasicMap<K, V>, AppendableLazyStorage<K, V> by storage

abstract class NonAppendableBasicMap<K, V>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: DataExternalizer<V>,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<K, V, LazyStorage<K, V>>(
    createLazyStorage(storageFile, keyDescriptor, valueExternalizer, icContext)
)

abstract class AppendableBasicMap<K, V>(
    storageFile: File,
    keyDescriptor: KeyDescriptor<K>,
    valueExternalizer: AppendableDataExternalizer<V>,
    icContext: IncrementalCompilationContext,
) : AbstractAppendableBasicMap<K, V, AppendableLazyStorage<K, V>>(
    createAppendableLazyStorage(storageFile, keyDescriptor, valueExternalizer, icContext)
)

abstract class BasicStringMap<V>(
    storageFile: File,
    valueExternalizer: DataExternalizer<V>,
    icContext: IncrementalCompilationContext,
) : NonAppendableBasicMap<String, V>(storageFile, EnumeratorStringDescriptor.INSTANCE, valueExternalizer, icContext)
