/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import java.io.File

/** Computes a [KlibEntrySnapshot] of a klib, with one snapshot per package fragment (i.e. per source file). */
object KlibEntrySnapshotter {
    fun snapshot(
        klib: File,
        settings: ClasspathEntrySnapshotter.Settings,
        metrics: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric> = DoNothingBuildMetricsReporter,
    ): KlibEntrySnapshot {
        val result = KlibLoader { libraryPaths(klib.path) }.load()
        val library = result.librariesStdlibFirst.singleOrNull()
            ?: error("Failed to load klib $klib: ${result.problematicLibraries.joinToString { it.problemCase.toString() }}")

        val fragmentSnapshots = metrics.measure(SNAPSHOT_CLASSES) {
            PackageFragmentSnapshotter.snapshot(library.metadata, settings)
        }
        return KlibEntrySnapshot(klibFileSnapshots = fragmentSnapshots)
    }
}
