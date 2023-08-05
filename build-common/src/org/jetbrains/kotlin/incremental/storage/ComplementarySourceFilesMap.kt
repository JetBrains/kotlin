/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

class ComplementarySourceFilesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : LazyStorageWrapper<File, Collection<File>, String, Collection<String>>(
    storage = createLazyStorage(storageFile, FilePathDescriptor, FilePathDescriptors, icContext),
    publicToInternalKey = icContext.pathConverterForSourceFiles::toPath,
    internalToPublicKey = icContext.pathConverterForSourceFiles::toFile,
    publicToInternalValue = icContext.pathConverterForSourceFiles::toPaths,
    internalToPublicValue = icContext.pathConverterForSourceFiles::toFiles,
), BasicMap<File, Collection<File>>