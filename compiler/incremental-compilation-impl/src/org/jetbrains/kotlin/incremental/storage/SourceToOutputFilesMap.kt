/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.dumpCollection
import java.io.File

class SourceToOutputFilesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer, icContext) {
    operator fun set(sourceFile: File, outputFiles: Collection<File>) {
        storage[pathConverter.toPath(sourceFile)] = outputFiles.map(pathConverter::toPath)
    }

    operator fun get(sourceFile: File): Collection<File> =
        storage[pathConverter.toPath(sourceFile)].orEmpty().map(pathConverter::toFile)

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun remove(file: File): Collection<File> =
        get(file).also { storage.remove(pathConverter.toPath(file)) }
}