/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_CHANGED_AND_IMPACTED_SET
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_CLASS_CHANGES
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_IMPACTED_SET
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics

/** Computes changes between two klib snapshots. The klib counterpart of [ClasspathChangesComputer]. */
internal object KlibChangesComputer {

    /**
     * Computes changes between the current and previous klib snapshots, plus unchanged elements that are impacted by the changes.
     *
     * Like on the JVM, the current snapshot is first shrunk against the lookups of the previous build, then compared with the shrunk
     * snapshot saved by the previous build.
     */
    fun computeKlibChanges(
        lookupStorage: LookupStorage,
        lazyKlibSnapshot: LazyKlibSnapshot,
        reporter: ClasspathSnapshotBuildReporter,
    ): ProgramSymbolSet {
        val currentSnapshots = lazyKlibSnapshot.getCurrentKlibSnapshot(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        val shrunkCurrentSnapshots = lazyKlibSnapshot
            .getComputedShrunkKlibSnapshotAgainstPreviousLookups(lookupStorage, LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        reporter.debug {
            "Shrunk current klib snapshot for diffing, retained ${shrunkCurrentSnapshots.size} / ${currentSnapshots.size} snapshots"
        }

        val shrunkPreviousSnapshots = lazyKlibSnapshot
            .getSavedShrunkKlibSnapshotAgainstPreviousLookups(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        reporter.debug { "Loaded shrunk previous klib snapshot for diffing, found ${shrunkPreviousSnapshots.size} snapshots" }

        return reporter.measure(COMPUTE_CHANGED_AND_IMPACTED_SET) {
            computeChangedAndImpactedSet(shrunkCurrentSnapshots, shrunkPreviousSnapshots, reporter)
        }
    }

    /**
     * Computes changes between the current and previous lists of snapshots, plus unchanged elements that are impacted by the changes.
     *
     * NOTE: Duplicate ids are not handled yet (see KT-90559 progress doc).
     */
    fun computeChangedAndImpactedSet(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
        reporter: ClasspathSnapshotBuildReporter,
    ): ProgramSymbolSet {
        val current: Map<String, KlibSnapshot> = currentSnapshots.associateBy { it.id }
        val previous: Map<String, KlibSnapshot> = previousSnapshots.associateBy { it.id }

        val changedCurrentSnapshots = current.values.filter { it.contentHash != previous[it.id]?.contentHash }
        val changedPreviousSnapshots = previous.values.filter { it.contentHash != current[it.id]?.contentHash }

        val changedSet = reporter.measure(COMPUTE_CLASS_CHANGES) {
            computePackageChanges(changedCurrentSnapshots, changedPreviousSnapshots)
        }
        reporter.reportVerboseWithLimit { "Changed set = ${changedSet.toDebugString()}" }

        if (changedSet.isEmpty()) {
            return changedSet
        }

        val changedAndImpactedSet = reporter.measure(COMPUTE_IMPACTED_SET) {
            // Over-approximate like on the JVM: compute the impact over the previous snapshots plus the changed current ones
            // (see `ClasspathChangesComputer.computeChangedAndImpactedSet`).
            KlibImpactedSymbolsComputer.computeImpactedSymbols(
                changes = changedSet,
                allSnapshots = (previousSnapshots.asSequence() + changedCurrentSnapshots.asSequence()).asIterable(),
            )
        }
        reporter.reportVerboseWithLimit {
            "Impacted classes = " +
                    (changedAndImpactedSet.run { classes + classMembers.keys } - changedSet.run { classes + classMembers.keys })
        }

        return changedAndImpactedSet
    }

    /**
     * Computes changes between the current and previous lists of changed snapshots. The result does not need to include elements that
     * are impacted by the changes.
     *
     * Snapshots without member-level data, or whose granularity changed between builds, are compared at the coarse level.
     */
    private fun computePackageChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet {
        val granularityChangedIds = findSnapshotsWithGranularityChange(currentSnapshots, previousSnapshots)

        val [coarseGrainedCurrent, fineGrainedCurrent] = currentSnapshots.partition {
            !it.hasMemberLevelData() || it.id in granularityChangedIds
        }
        val [coarseGrainedPrevious, fineGrainedPrevious] = previousSnapshots.partition {
            !it.hasMemberLevelData() || it.id in granularityChangedIds
        }

        return computeCoarseGrainedChanges(coarseGrainedCurrent, coarseGrainedPrevious) +
                computeFineGrainedChanges(fineGrainedCurrent, fineGrainedPrevious)
    }

    private fun findSnapshotsWithGranularityChange(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): Set<String> {
        val current = currentSnapshots.associateBy { it.id }
        val previous = previousSnapshots.associateBy { it.id }

        return (current.keys intersect previous.keys).filterTo(mutableSetOf()) { id ->
            current.getValue(id).hasMemberLevelData() != previous.getValue(id).hasMemberLevelData()
        }
    }

    private fun KlibSnapshot.hasMemberLevelData(): Boolean = when (this) {
        is PackageFragmentSnapshot -> packageFragmentProto != null
    }

    @Suppress("unused")
    private fun computeCoarseGrainedChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet = TODO("KT-90559: coarse-grained klib changes")

    @Suppress("unused")
    private fun computeFineGrainedChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet = TODO("KT-90559: fine-grained klib changes")
}

private object KlibImpactedSymbolsComputer {

    /**
     * Computes the set of symbols that are *transitively* impacted by [changes], e.g. subclasses of a changed class.
     *
     * The returned set is *inclusive* (it contains [changes] + the impacted symbols).
     */
    @Suppress("unused")
    fun computeImpactedSymbols(changes: ProgramSymbolSet, allSnapshots: Iterable<KlibSnapshot>): ProgramSymbolSet =
        TODO("KT-90559: impacted symbols for klibs")
}
