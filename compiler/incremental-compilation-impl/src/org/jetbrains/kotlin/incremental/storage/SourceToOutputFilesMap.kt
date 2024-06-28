/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.File

class SourceToOutputFilesMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AppendableSetBasicMap<File, File>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    icContext.fileDescriptorForOutputFiles,
    icContext
) {
    @Synchronized
    fun getAndRemove(sourceFile: File): Set<File>? {
        return get(sourceFile).also {
            remove(sourceFile)
        }
    }
}
