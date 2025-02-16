/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.CachedClasspathSnapshotSerializer
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasspath
import org.jetbrains.kotlin.incremental.classpathDiff.removeDuplicateAndInaccessibleClasses
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile

/**
 * There are 2 steps where we need to load the current classpath snapshot and shrink it:
 * - Before classpath diffing when `classpathChanges` is ToBeComputedByIncrementalCompiler (see `calculateSourcesToCompileImpl`)
 * - After compilation (see `performWorkAfterSuccessfulCompilation`)
 * To avoid duplicated work, we store the snapshots after the first step for reuse (if the first step is executed).
 *
 * To decouple this "lazy loading" logic from IC Runner state, LazyClasspathSnapshot is introduced
 */
internal class LazyClasspathSnapshot(
    private val classpathChanges: ClasspathChanges,
    private val reporter: ClasspathSnapshotBuildReporter
) {
    private var currentClasspathSnapshot: List<AccessibleClassSnapshot>? = null
    private var computedShrunkClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>? = null
    private var savedShrunkClasspathSnapshot: List<AccessibleClassSnapshot>? = null

    fun getCurrentClasspathSnapshot(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<AccessibleClassSnapshot> {
        val stored = currentClasspathSnapshot
        if (stored != null) {
            return stored
        }

        val computed = when (classpathChanges) {
            is ClasspathChanges.ClasspathSnapshotEnabled -> reporter.measure(metricToReportIfComputing.loadClasspathSnapshotTag) {
                val classpathSnapshotFiles = classpathChanges.classpathSnapshotFiles
                val classpathSnapshot =
                    CachedClasspathSnapshotSerializer.load(classpathSnapshotFiles.currentClasspathEntrySnapshotFiles, reporter)
                reporter.measure(metricToReportIfComputing.removeDuplicateClassesTag) {
                    classpathSnapshot.removeDuplicateAndInaccessibleClasses()
                }
            }
            else -> emptyList()
        }
        currentClasspathSnapshot = computed
        return computed
    }

    fun getComputedShrunkClasspathAgainstPreviousLookups(lookupStorage: LookupStorage, metricToReportIfComputing: LazySnapshotLoadingMetrics): List<AccessibleClassSnapshot> {
        val stored = computedShrunkClasspathAgainstPreviousLookups
        if (stored != null) {
            return stored
        }

        val computed = reporter.measure(metricToReportIfComputing.calculateShrunkClasspathTag) {
            val metricsReporter = if (metricToReportIfComputing is LazySnapshotLoadingMetrics.OnClasspathDiffComputation) {
                ClasspathSnapshotShrinker.MetricsReporter(
                    reporter,
                    GradleBuildTime.GET_LOOKUP_SYMBOLS,
                    GradleBuildTime.FIND_REFERENCED_CLASSES,
                    GradleBuildTime.FIND_TRANSITIVELY_REFERENCED_CLASSES
                )
            } else ClasspathSnapshotShrinker.MetricsReporter()
            shrinkClasspath(
                currentClasspathSnapshot!!, lookupStorage, metricsReporter
            )
        }
        computedShrunkClasspathAgainstPreviousLookups = computed
        return computed
    }

    fun getSavedShrunkClasspathAgainstPreviousLookups(metricToReportIfComputing: LazySnapshotLoadingMetrics): List<AccessibleClassSnapshot> {
        val stored = savedShrunkClasspathSnapshot
        if (stored != null) {
            return stored
        }

        val loaded = when (classpathChanges) {
            is ClasspathChanges.ClasspathSnapshotEnabled -> reporter.measure(metricToReportIfComputing.loadShrunkClasspathTag) {
                ListExternalizer(AccessibleClassSnapshotExternalizer)
                    .loadFromFile(classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile)
            }
            else -> emptyList()
        }
        savedShrunkClasspathSnapshot = loaded
        return loaded
    }
}

internal sealed interface LazySnapshotLoadingMetrics {
    val loadClasspathSnapshotTag: GradleBuildTime
    val removeDuplicateClassesTag: GradleBuildTime
    val calculateShrunkClasspathTag: GradleBuildTime
    val loadShrunkClasspathTag: GradleBuildTime

    object OnClasspathDiffComputation : LazySnapshotLoadingMetrics {
        override val loadClasspathSnapshotTag = GradleBuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT
        override val removeDuplicateClassesTag = GradleBuildTime.REMOVE_DUPLICATE_CLASSES
        override val calculateShrunkClasspathTag = GradleBuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT
        override val loadShrunkClasspathTag = GradleBuildTime.LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT
    }

    object OnIncrementalShrunkClasspathUpdate : LazySnapshotLoadingMetrics {
        override val loadClasspathSnapshotTag = GradleBuildTime.INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT
        override val removeDuplicateClassesTag = GradleBuildTime.INCREMENTAL_REMOVE_DUPLICATE_CLASSES
        override val calculateShrunkClasspathTag = GradleBuildTime.INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT
        override val loadShrunkClasspathTag = GradleBuildTime.INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS
    }

    object OnNonIncrementalShrunkClasspathUpdate : LazySnapshotLoadingMetrics {
        override val loadClasspathSnapshotTag = GradleBuildTime.NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT
        override val removeDuplicateClassesTag = GradleBuildTime.NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES
        override val calculateShrunkClasspathTag = GradleBuildTime.NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT
        override val loadShrunkClasspathTag
            get() = error("Non-incremental classpath shrinker should not need previous shrunk classpath.")
    }

    object AssertThatDataIsAlreadyComputed : LazySnapshotLoadingMetrics {
        override val loadClasspathSnapshotTag
            get() = error("Expected that snapshot is loaded, but it was null.")
        override val removeDuplicateClassesTag
            get() = error("Expected that snapshot is loaded, but it was null.")
        override val calculateShrunkClasspathTag
            get() = error("Expected that shrunk classpath is computed, but it was null.")
        override val loadShrunkClasspathTag
            get() = error("Expected that previous shrunk classpath was read, but it was null.")
    }
}
