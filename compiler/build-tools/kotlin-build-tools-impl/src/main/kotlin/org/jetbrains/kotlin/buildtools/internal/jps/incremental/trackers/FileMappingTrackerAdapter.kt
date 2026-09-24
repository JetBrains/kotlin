/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerFileMappingTracker
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import java.io.File

@OptIn(InternalBuildToolsApi::class)
internal class FileMappingTrackerAdapter(private val tracker: CompilerFileMappingTracker) : ICFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {
        tracker.recordSourceFilesToOutputFileMapping(sourceFiles.map { it.path }, outputFile.path)
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) {
        tracker.recordSourceReferencedByCompilerPlugin(sourceFile.path)
    }

    override fun recordOutputFileGeneratedForPlugin(outputFile: File) {
        tracker.recordOutputFileGeneratedForPlugin(outputFile.path)
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFile: File) {
        tracker.recordSourceFileGeneratedForPlugin(sourceFile.path)
    }
}
