/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

/** Maps each source file to the .class files that it is compiled into. */
class SourceToOutputFilesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : LazyStorageWrapper<File, Collection<File>, String, Collection<String>>(
    storage = createLazyStorage(storageFile, FilePathDescriptor, FilePathDescriptors, icContext),
    publicToInternalKey = icContext.pathConverterForSourceFiles::toPath,
    internalToPublicKey = icContext.pathConverterForSourceFiles::toFile,
    publicToInternalValue = icContext.pathConverterForClassFiles::toPaths,
    internalToPublicValue = icContext.pathConverterForClassFiles::toFiles,
), BasicMap<File, Collection<File>> {

    @Synchronized
    fun getAndRemove(sourceFile: File): Collection<File>? {
        return get(sourceFile)?.also {
            remove(sourceFile)
        }
    }
}
