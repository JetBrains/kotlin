/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.dumpCollection
import java.io.File

class SourceToJsOutputMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : BasicStringMap<Collection<String>>(storageFile, StringCollectionExternalizer, icContext) {
    override fun dumpValue(value: Collection<String>): String = value.dumpCollection()

    @Synchronized
    fun add(key: File, value: File) {
        storage.append(pathConverter.toPath(key), listOf(pathConverter.toPath(value)))
    }

    operator fun get(sourceFile: File): Collection<File> =
        storage[pathConverter.toPath(sourceFile)]?.map { pathConverter.toFile(it) } ?: setOf()


    @Synchronized
    operator fun set(key: File, values: Collection<File>) {
        if (values.isEmpty()) {
            remove(key)
            return
        }

        storage[pathConverter.toPath(key)] = values.map { pathConverter.toPath(it) }
    }

    @Synchronized
    fun remove(key: File) {
        storage.remove(pathConverter.toPath(key))
    }

    @Synchronized
    fun removeValues(key: File, removed: Set<File>) {
        val notRemoved = this[key].filter { it !in removed }
        this[key] = notRemoved
    }
}