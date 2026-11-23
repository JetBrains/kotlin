/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import java.io.File

/**
 * Loads and caches the klib snapshots needed before and after compilation, so that the current snapshot is loaded and shrunk only
 * once per build.
 *
 * @param currentSnapshotFiles files with the current [KlibEntrySnapshot]s, one per klib
 * @param shrunkPreviousSnapshotFile file with the shrunk snapshot saved by the previous build
 */
internal class LazyKlibSnapshot(
    private val currentSnapshotFiles: List<File>,
    private val shrunkPreviousSnapshotFile: File,
    private val reporter: ClasspathSnapshotBuildReporter,
) {
    private var currentSnapshot: List<KlibFileSnapshot>? = null
    private var computedShrunkSnapshotAgainstPreviousLookups: List<KlibFileSnapshot>? = null
    private var savedShrunkSnapshot: List<KlibFileSnapshot>? = null

    fun getCurrentKlibSnapshot(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<KlibFileSnapshot> {
        currentSnapshot?.let { return it }

        return reporter.measure(metricToReportIfComputing.loadClasspathSnapshotTag) {
            val entrySnapshots = currentSnapshotFiles.map { KlibEntrySnapshotExternalizer.loadFromFile(it) }
            reporter.measure(metricToReportIfComputing.removeDuplicateClassesTag) {
                entrySnapshots.toKlibSnapshots()
            }
        }.also { currentSnapshot = it }
    }

    fun getComputedShrunkKlibSnapshotAgainstPreviousLookups(
        lookupStorage: LookupStorage,
        metricToReportIfComputing: LazySnapshotLoadingMetrics,
    ): List<KlibFileSnapshot> {
        computedShrunkSnapshotAgainstPreviousLookups?.let { return it }

        val currentSnapshot = getCurrentKlibSnapshot(metricToReportIfComputing)
        return reporter.measure(metricToReportIfComputing.calculateShrunkClasspathTag) {
            KlibSnapshotShrinker.shrink(currentSnapshot, lookupStorage)
        }.also { computedShrunkSnapshotAgainstPreviousLookups = it }
    }

    fun getSavedShrunkKlibSnapshotAgainstPreviousLookups(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<KlibFileSnapshot> {
        savedShrunkSnapshot?.let { return it }

        return reporter.measure(metricToReportIfComputing.loadShrunkClasspathTag) {
            ListExternalizer(KlibSnapshotExternalizer).loadFromFile(shrunkPreviousSnapshotFile)
        }.also { savedShrunkSnapshot = it }
    }
}

// TODO(KT-90559): handle duplicate ids across klibs. Until then, fail instead of silently missing changes.
/** Flattens the snapshots of all klibs. */
internal fun List<KlibEntrySnapshot>.toKlibSnapshots(): List<KlibFileSnapshot> {
    val snapshots = flatMap { it.klibFileSnapshots }

    val duplicateIds = snapshots.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
    check(duplicateIds.isEmpty()) { "Klib snapshots with duplicate ids are not supported yet: ${duplicateIds.sorted()}" }

    return snapshots
}
