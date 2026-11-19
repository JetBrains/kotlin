/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import java.io.File

/**
 * Loads and caches the klib snapshots needed before and after compilation. The klib counterpart of `LazyClasspathSnapshot`.
 *
 * @param currentSnapshotFiles files with the current [KlibEntrySnapshot]s, one per klib
 * @param shrunkPreviousSnapshotFile file with the shrunk snapshot saved by the previous build
 */
@Suppress("unused")
internal class LazyKlibSnapshot(
    private val currentSnapshotFiles: List<File>,
    private val shrunkPreviousSnapshotFile: File,
    private val reporter: ClasspathSnapshotBuildReporter,
) {
    fun getCurrentKlibSnapshot(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<KlibSnapshot> =
        TODO("KT-90559: load current klib snapshots")

    fun getComputedShrunkKlibSnapshotAgainstPreviousLookups(
        lookupStorage: LookupStorage,
        metricToReportIfComputing: LazySnapshotLoadingMetrics,
    ): List<KlibSnapshot> = TODO("KT-90559: shrink current klib snapshots")

    fun getSavedShrunkKlibSnapshotAgainstPreviousLookups(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<KlibSnapshot> =
        TODO("KT-90559: load shrunk previous klib snapshots")
}
