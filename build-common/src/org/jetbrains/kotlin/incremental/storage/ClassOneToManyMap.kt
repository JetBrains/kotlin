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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal open class ClassOneToManyMap(storageFile: File, private val cacheable: Boolean = false) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer) {
    val countGet = AtomicInteger(0)
    val countReadKeys = AtomicInteger(0)
    val countOptimizationWorked = AtomicInteger(0)
    val totalTime = AtomicLong(0)
    val totalReadKeysTime = AtomicLong(0)



    var cacheRead = false
    val keys = HashSet<String>()

    override fun dumpValue(value: Collection<String>): String = value.dumpCollection()

    @Synchronized
    fun add(key: FqName, value: FqName) {
        storage.append(key.asString(), listOf(value.asString()))
    }

    @Synchronized
    operator fun get(key: FqName): Collection<FqName> {
        val start = System.currentTimeMillis()
        try {
            if (cacheable) {
                if (!cacheRead) {
                    val startRead = System.currentTimeMillis()
                    keys.addAll(storage.keysUnsafe)
                    totalReadKeysTime.addAndGet(System.currentTimeMillis() - startRead)
                    cacheRead = true
                    countReadKeys.incrementAndGet()
                }
                if (!keys.contains(key.asString())) {
                    countOptimizationWorked.incrementAndGet()
                    return setOf()
                }
            }
            countGet.incrementAndGet()
            return storage[key.asString()]?.map(::FqName) ?: setOf()
        } finally {
            totalTime.addAndGet(System.currentTimeMillis() - start)
        }
    }

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

    @Synchronized
    override fun close() {
        File("C:\\JB\\Cooperative\\LOGS_IC_.txt").appendText("${System.currentTimeMillis()}\t${super.storageFile}\tReadKeys=${countReadKeys.get()}\tOptimizationWorked=${countOptimizationWorked.get()}\tTotalGetCount=${countGet.get()}\tTotalTime=${totalTime.get()}\tReadKeysTime=${totalReadKeysTime.get()}\r\n")
        super.close()
        keys.clear()
        cacheRead = false
    }
}

internal class SubtypesMap(storageFile: File) : ClassOneToManyMap(storageFile, true)
internal class SupertypesMap(storageFile: File) : ClassOneToManyMap(storageFile, true)
