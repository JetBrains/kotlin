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
        storage[icContext.pathConverterForSourceFiles.toPath(sourceFile)] = outputFiles.map(icContext.pathConverterForOutputFiles::toPath)
    }

    operator fun get(sourceFile: File): Collection<File>? =
        storage[icContext.pathConverterForSourceFiles.toPath(sourceFile)]?.map(icContext.pathConverterForOutputFiles::toFile)

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun getAndRemove(file: File): Collection<File>? =
        get(file).also { storage.remove(icContext.pathConverterForSourceFiles.toPath(file)) }
}