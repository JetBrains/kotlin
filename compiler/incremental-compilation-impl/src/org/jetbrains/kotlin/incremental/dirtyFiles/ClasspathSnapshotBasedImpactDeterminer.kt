/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotDisabled
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.ClasspathChanges.NotAvailableForJSCompiler
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClasspathChanges
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.snapshots.LazyClasspathSnapshot
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.incremental.storage.LinkedHashMapExternalizer
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LongExternalizer
import org.jetbrains.kotlin.incremental.storage.StringExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import java.io.File

internal class ClasspathSnapshotBasedImpactDeterminer (
    private val caches: IncrementalJvmCachesManager,
    private val classpathChanges: ClasspathChanges,

    private val lazyClasspathSnapshot: LazyClasspathSnapshot,
    private val reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
) : ImpactedFilesDeterminer {

    override fun determineChangedAndImpactedSymbols(): ChangesEither {
        reporter.debug { "Classpath changes info passed from Gradle task: ${classpathChanges::class.simpleName}" }

        return when (classpathChanges) {
            // Note: classpathChanges is deserialized, so they are no longer singleton objects and need to be compared using `is` (not `==`)
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(COMPUTE_CLASSPATH_CHANGES) {
                reporter.addMetric(COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT, 1)
                val currentModuleInfoHashes =
                    lazyClasspathSnapshot.getCurrentModuleInfoHashes(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
                val previousModuleInfoHashes =
                    loadPreviousModuleInfoHashes(classpathChanges.classpathSnapshotFiles.previousModuleInfoHashesFile)
                if (currentModuleInfoHashes != previousModuleInfoHashes) {
                    ChangesEither.Unknown(BuildAttribute.DEPENDENCY_MODULE_INFO_CHANGED)
                } else {
                    val classpathSymbolChanges = computeClasspathChanges(
                        caches.lookupCache,
                        lazyClasspathSnapshot,
                        ClasspathSnapshotBuildReporter(reporter)
                    )
                    val changedPackages = computeChangedPackages(
                        previous = loadPreviousPackageInfoHashes(classpathChanges.classpathSnapshotFiles.previousPackageInfoHashesFile),
                        current = lazyClasspathSnapshot.getCurrentPackageInfoHashes(LazySnapshotLoadingMetrics.OnClasspathDiffComputation),
                    )
                    // The classpath ABI changes plus the package-info-derived changes give the changed/impacted symbols on the classpath.
                    // We also need to compute symbols in the current module that are impacted by them.
                    val changes = classpathSymbolChanges.toChangeInfoList() +
                            changedPackageClassChangeInfos(changedPackages, lazyClasspathSnapshot)
                    changes.getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter).toChangesEither()
                }
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> error("Unexpected ClasspathSnapshotDisabled for this code path: ${classpathChanges.javaClass.name}.")
            is NotAvailableForJSCompiler -> error("Unexpected NotAvailableForJSCompiler for this code path: ${classpathChanges.javaClass.name}.")
        }
    }
}

private fun loadPreviousModuleInfoHashes(file: File): Set<Long> =
    if (file.exists()) ListExternalizer(LongExternalizer).loadFromFile(file).toSet() else emptySet()

private fun loadPreviousPackageInfoHashes(file: File): Map<String, Long> =
    if (file.exists()) LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).loadFromFile(file) else emptyMap()

private fun computeChangedPackages(previous: Map<String, Long>, current: Map<String, Long>): Set<String> =
    (previous.keys + current.keys).filterTo(mutableSetOf()) { previous[it] != current[it] }

private fun changedPackageClassChangeInfos(
    changedPackages: Set<String>,
    lazyClasspathSnapshot: LazyClasspathSnapshot,
): List<ChangeInfo> {
    if (changedPackages.isEmpty()) return emptyList()
    return lazyClasspathSnapshot.getCurrentClasspathSnapshot(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        .filter { it.classId.packageFqName.asString() in changedPackages }
        // `areSubclassesAffected = true`: a package-level annotation can affect subclasses declared in the consuming module too.
        .map { ChangeInfo.SignatureChanged(it.classId.asSingleFqName(), areSubclassesAffected = true) }
}

private fun DirtyData.toChangesEither(): ChangesEither.Known {
    return ChangesEither.Known(
        lookupSymbols = dirtyLookupSymbols,
        fqNames = dirtyClassesFqNames + dirtyClassesFqNamesForceRecompile
    )
}

private fun ProgramSymbolSet.toChangeInfoList(): List<ChangeInfo> {
    val changes = mutableListOf<ChangeInfo>()
    classes.forEach { classId ->
        // It's important to set `areSubclassesAffected = true` when we don't know
        changes.add(ChangeInfo.SignatureChanged(classId.asSingleFqName(), areSubclassesAffected = true))
    }
    classMembers.forEach { [classId, members] ->
        changes.add(ChangeInfo.MembersChanged(classId.asSingleFqName(), members))
    }
    packageMembers.forEach { [packageFqName, members] ->
        changes.add(ChangeInfo.MembersChanged(packageFqName, members))
    }
    return changes
}
