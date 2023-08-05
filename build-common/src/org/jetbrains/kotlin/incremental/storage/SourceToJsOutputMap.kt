/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

class SourceToJsOutputMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AppendableLazyStorageWrapper<File, Collection<File>, String, Collection<String>>(
    storage = createAppendableLazyStorage(storageFile, FilePathDescriptor, FilePathDescriptors, icContext),
    publicToInternalKey = icContext.pathConverterForSourceFiles::toPath,
    internalToPublicKey = icContext.pathConverterForSourceFiles::toFile,
    publicToInternalValue = icContext.pathConverterForClassFiles::toPaths,
    internalToPublicValue = icContext.pathConverterForClassFiles::toFiles,
), BasicMap<File, Collection<File>> {

    @Synchronized
    fun add(key: File, value: File) {
        append(key, listOf(value))
    }
}