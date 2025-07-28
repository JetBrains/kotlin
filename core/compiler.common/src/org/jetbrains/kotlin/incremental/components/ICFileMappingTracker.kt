/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import java.io.File

interface ICFileMappingTracker {
    fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File)
    fun recordSourceReferencedByCompilerPlugin(sourceFile: File)
    fun recordOutputFileGeneratedForPlugin(outputFile: File)

    object DoNothing : ICFileMappingTracker {
        override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {}
        override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) {}
        override fun recordOutputFileGeneratedForPlugin(outputFile: File) {}
    }
}
