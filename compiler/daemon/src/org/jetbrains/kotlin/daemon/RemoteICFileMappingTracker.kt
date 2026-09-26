/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.ICFileMappingTracker
import java.io.File

class RemoteICFileMappingTracker(
    @Suppress("DEPRECATION") val facade: CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
) : ICFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) {
        profiler.withMeasure(this) {
            facade.icFileMappingTracker_recordSourceFilesToOutputFileMapping(sourceFiles.map { it.path }, outputFile.path)
        }
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) {
        profiler.withMeasure(this) {
            facade.icFileMappingTracker_recordSourceReferencedByCompilerPlugin(sourceFile.path)
        }
    }

    override fun recordOutputFileGeneratedForPlugin(outputFile: File) {
        profiler.withMeasure(this) {
            facade.icFileMappingTracker_recordOutputFileGeneratedForPlugin(outputFile.path)
        }
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFile: File) {
        profiler.withMeasure(this) {
            facade.icFileMappingTracker_recordSourceFileGeneratedForPlugin(sourceFile.path)
        }
    }
}
