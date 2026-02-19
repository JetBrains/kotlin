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
) : AppendableSetBasicMap<File, File>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    icContext.fileDescriptorForOutputFiles,
    icContext
)
