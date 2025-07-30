/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import java.io.File

class ICFileMappingTrackerImpl(private val outputItemsCollector: OutputItemsCollector) : ICFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {
        outputItemsCollector.add(sourceFiles, outputFile)
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) {
        outputItemsCollector.addSourceReferencedByCompilerPlugin(sourceFile)
    }

    override fun recordOutputFileGeneratedForPlugin(outputFile: File) {
        outputItemsCollector.addOutputFileGeneratedForPlugin(outputFile)
    }
}
