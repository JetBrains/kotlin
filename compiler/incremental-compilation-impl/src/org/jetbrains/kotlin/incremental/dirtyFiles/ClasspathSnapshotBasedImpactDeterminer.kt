/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.dirtyFiles

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
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

internal class ClasspathSnapshotBasedImpactDeterminer (
    private val caches: IncrementalJvmCachesManager,
    private val classpathChanges: ClasspathChanges,

    private val lazyClasspathSnapshot: LazyClasspathSnapshot,
    private val reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
) : ImpactedFilesDeterminer {

    override fun determineChangedAndImpactedSymbols(): ChangesEither {
        reporter.debug { "Classpath changes info passed from Gradle task: ${classpathChanges::class.simpleName}" }

        return when (classpathChanges) {
            // Note: classpathChanges is deserialized, so they are no longer singleton objects and need to be compared using `is` (not `==`)
            is NoChanges -> ChangesEither.Known(emptySet(), emptySet())
            is ToBeComputedByIncrementalCompiler -> reporter.measure(GradleBuildTime.COMPUTE_CLASSPATH_CHANGES) {
                reporter.addMetric(GradleBuildPerformanceMetric.COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT, 1)
                val classpathChanges = computeClasspathChanges(
                    caches.lookupCache,
                    lazyClasspathSnapshot,
                    ClasspathSnapshotBuildReporter(reporter)
                )
                // `classpathChanges` contains changed and impacted symbols on the classpath.
                // We also need to compute symbols in the current module that are impacted by `classpathChanges`.
                classpathChanges.toChangeInfoList().getChangedAndImpactedSymbols(listOf(caches.platformCache), reporter).toChangesEither()
            }
            is NotAvailableDueToMissingClasspathSnapshot -> ChangesEither.Unknown(BuildAttribute.CLASSPATH_SNAPSHOT_NOT_FOUND)
            is NotAvailableForNonIncrementalRun -> ChangesEither.Unknown(BuildAttribute.UNKNOWN_CHANGES_IN_GRADLE_INPUTS)
            is ClasspathSnapshotDisabled -> error("Unexpected ClasspathSnapshotDisabled for this code path: ${classpathChanges.javaClass.name}.")
            is NotAvailableForJSCompiler -> error("Unexpected NotAvailableForJSCompiler for this code path: ${classpathChanges.javaClass.name}.")
        }
    }
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
    classMembers.forEach { (classId, members) ->
        changes.add(ChangeInfo.MembersChanged(classId.asSingleFqName(), members))
    }
    packageMembers.forEach { (packageFqName, members) ->
        changes.add(ChangeInfo.MembersChanged(packageFqName, members))
    }
    return changes
}
