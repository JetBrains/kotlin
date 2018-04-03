/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.dumpCollection
import java.io.File

class FilesMap(storageFile: File)
    : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {

    operator fun set(sourceFile: File, outputFiles: Collection<File>) {
        storage[sourceFile.absolutePath] = outputFiles.map { it.absolutePath }
    }

    operator fun get(sourceFile: File): Collection<File> =
        storage[sourceFile.absolutePath].orEmpty().map(::File)

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun remove(file: File): Collection<File> =
        get(file).also { storage.remove(file.absolutePath) }
}