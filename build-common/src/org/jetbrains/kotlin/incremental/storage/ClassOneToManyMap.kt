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

import org.jetbrains.kotlin.incremental.dumpCollection
import org.jetbrains.kotlin.name.FqName
import java.io.File

internal open class ClassOneToManyMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
    override fun dumpValue(value: Collection<String>): String = value.dumpCollection()

    @Synchronized
    fun add(key: FqName, value: FqName) {
        storage.append(key.asString(), listOf(value.asString()))
    }

    operator fun get(key: FqName): Collection<FqName> =
        storage[key.asString()]?.map(::FqName) ?: setOf()

    @Synchronized
    operator fun set(key: FqName, values: Collection<FqName>) {
        if (values.isEmpty()) {
            remove(key)
            return
        }

        storage[key.asString()] = values.map(FqName::asString)
    }

    @Synchronized
    fun remove(key: FqName) {
        storage.remove(key.asString())
    }

    // Access to caches could be done from multiple threads (e.g. JPS worker and RMI). The underlying collection is already synchronized,
    // thus we need synchronization of this method and all modification methods.
    @Synchronized
    fun removeValues(key: FqName, removed: Set<FqName>) {
        val notRemoved = this[key].filter { it !in removed }
        this[key] = notRemoved
    }
}

internal class SubtypesMap(storageFile: File) : ClassOneToManyMap(storageFile)
internal class SupertypesMap(storageFile: File) : ClassOneToManyMap(storageFile)
