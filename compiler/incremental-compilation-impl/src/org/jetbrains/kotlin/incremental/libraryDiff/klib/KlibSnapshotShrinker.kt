/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.CLASSPATH_ENTRY_COUNT
import org.jetbrains.kotlin.build.report.metrics.CLASSPATH_SNAPSHOT_SIZE
import org.jetbrains.kotlin.build.report.metrics.SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT
import org.jetbrains.kotlin.build.report.metrics.SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
import org.jetbrains.kotlin.build.report.metrics.SHRUNK_CLASSPATH_SNAPSHOT_SIZE
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.LookupSymbolSet
import org.jetbrains.kotlin.incremental.classpathDiff.toLookupSymbol
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.filterToSetOrEmpty

internal object KlibSnapshotShrinker {

    /** Retains only the snapshots referenced, directly or transitively, by the lookup symbols stored in [lookupStorage]. */
    fun shrink(allSnapshots: List<KlibFileSnapshot>, lookupStorage: LookupStorage): List<KlibFileSnapshot> =
        shrink(allSnapshots, lookupStorage.lookupSymbols)

    /**
     * Retains only the snapshots referenced by [lookupSymbols], directly or transitively (e.g. via supertypes), so that the impact
     * of changes can still be computed (see [KlibChangesComputer.computeChangedAndImpactedSet]).
     */
    fun shrink(allSnapshots: List<KlibFileSnapshot>, lookupSymbols: Collection<LookupSymbolKey>): List<KlibFileSnapshot> {
        val lookupSymbolSet =
            LookupSymbolSet(lookupSymbols.asSequence().map { LookupSymbol(name = it.name, scope = it.scope) }.asIterable())

        val transitivelyReferencedClassIds = findTransitivelyReferencedClassIds(
            allSnapshots,
            findReferencedClassIds(allSnapshots, lookupSymbolSet)
        )

        return allSnapshots.filter { snapshot ->
            val hasReferencedClass = snapshot.classInfos().any { it.classId in transitivelyReferencedClassIds }
            if (hasReferencedClass) return@filter true

            when (snapshot) {
                is PackageFragmentSnapshot -> {
                    val lookupNamesInPackage = lookupSymbolSet.getLookupNamesInScope(snapshot.packageName)
                    snapshot.packageMemberNames.any { it in lookupNamesInPackage }
                }
            }
        }
    }

    private fun findReferencedClassIds(allSnapshots: List<KlibFileSnapshot>, lookupSymbols: LookupSymbolSet): Set<ClassId> =
        allSnapshots
            .flatMap { it.classInfos() }
            .map { it.classId }
            .filterToSetOrEmpty { classId ->
                ClassSymbol(classId).toLookupSymbol() in lookupSymbols
                        || lookupSymbols.getLookupNamesInScope(classId.asSingleFqName()).isNotEmpty()
            }

    /** The returned set is *inclusive* (it contains [referencedClassIds] + their supertypes, directly or transitively). */
    private fun findTransitivelyReferencedClassIds(allSnapshots: List<KlibFileSnapshot>, referencedClassIds: Set<ClassId>): Set<ClassId> {
        if (referencedClassIds.isEmpty()) return emptySet()

        val impactingClassesResolver = KlibAllImpacts.getReverseResolver(allSnapshots)
        return findReachableNodes(referencedClassIds, impactingClassesResolver::getImpactingClasses)
    }
}

/**
 * Shrinks the current klib snapshot against the lookups of this build and saves it for the next build.
 *
 * Unlike `shrinkAndSaveClasspathSnapshot`, it always shrinks non-incrementally (see KT-90559 progress doc).
 */
internal fun shrinkAndSaveKlibSnapshot(
    classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
    lookupStorage: LookupStorage,
    lazyKlibSnapshot: LazyKlibSnapshot,
    reporter: ClasspathSnapshotBuildReporter,
) {
    val metrics = LazySnapshotLoadingMetrics.OnNonIncrementalShrunkClasspathUpdate
    val currentSnapshot = lazyKlibSnapshot.getCurrentKlibSnapshot(metrics)
    val shrunkCurrentSnapshot = reporter.measure(metrics.calculateShrunkClasspathTag) {
        KlibSnapshotShrinker.shrink(currentSnapshot, lookupStorage)
    }

    val snapshotFiles = classpathChanges.classpathSnapshotFiles
    reporter.measure(SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT) {
        ListExternalizer(KlibSnapshotExternalizer).saveToFile(snapshotFiles.shrunkPreviousClasspathSnapshotFile, shrunkCurrentSnapshot)
    }

    reporter.debug {
        "Shrunk current klib snapshot after compilation, retained ${shrunkCurrentSnapshot.size} / ${currentSnapshot.size} snapshots"
    }

    if (!reporter.isEnabled) return

    reporter.addMetric(SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, 1)
    reporter.addMetric(CLASSPATH_ENTRY_COUNT, snapshotFiles.currentClasspathEntrySnapshotFiles.size.toLong())
    reporter.addMetric(CLASSPATH_SNAPSHOT_SIZE, snapshotFiles.currentClasspathEntrySnapshotFiles.sumOf { it.length() })
    reporter.addMetric(SHRUNK_CLASSPATH_SNAPSHOT_SIZE, snapshotFiles.shrunkPreviousClasspathSnapshotFile.length())
}
