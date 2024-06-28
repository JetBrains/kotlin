/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasses
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasspath
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.jetbrains.kotlin.name.ClassId

object ClasspathSnapshotShrinker {

    /**
     * Shrinks the given classes by retaining only classes that are referenced by the lookup symbols stored in the given [LookupStorage].
     */
    fun shrinkClasspath(
        allClasses: List<AccessibleClassSnapshot>,
        lookupStorage: LookupStorage,
        metrics: MetricsReporter = MetricsReporter()
    ): List<AccessibleClassSnapshot> {
        val lookupSymbols = metrics.getLookupSymbols {
            lookupStorage.lookupSymbols
        }
        return shrinkClasses(allClasses, lookupSymbols, metrics)
    }

    /**
     * Shrinks the given classes by retaining only classes that are referenced by the given lookup symbols.
     *
     * Note: We need to retain both directly and transitively referenced classes to compute the impact of classpath changes correctly (see
     * [ClasspathChangesComputer.computeChangedAndImpactedSet]).
     */
    fun shrinkClasses(
        allClasses: List<AccessibleClassSnapshot>,
        lookupSymbols: Collection<LookupSymbolKey>,
        metrics: MetricsReporter = MetricsReporter()
    ): List<AccessibleClassSnapshot> {
        val referencedClasses = metrics.findReferencedClasses {
            findReferencedClasses(allClasses, lookupSymbols)
        }
        return metrics.findTransitivelyReferencedClasses {
            findTransitivelyReferencedClasses(allClasses, referencedClasses)
        }
    }

    /**
     * Finds classes that are *directly* referenced by the given lookup symbols.
     *
     * Note: It's okay to over-approximate the result.
     */
    private fun findReferencedClasses(
        allClasses: List<AccessibleClassSnapshot>,
        lookupSymbolKeys: Collection<LookupSymbolKey>
    ): List<AccessibleClassSnapshot> {
        // Use LookupSymbolSet for efficiency
        val lookupSymbols =
            LookupSymbolSet(lookupSymbolKeys.asSequence().map { LookupSymbol(name = it.name, scope = it.scope) }.asIterable())

        val referencedClasses = allClasses.filter { clazz ->
            when (clazz) {
                is RegularKotlinClassSnapshot, is JavaClassSnapshot -> {
                    ClassSymbol(clazz.classId).toLookupSymbol() in lookupSymbols
                            || lookupSymbols.getLookupNamesInScope(clazz.classId.asSingleFqName()).isNotEmpty()
                }
                is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> {
                    val lookupNamesInScope = lookupSymbols.getLookupNamesInScope(clazz.classId.packageFqName)
                    if (lookupNamesInScope.isEmpty()) return@filter false
                    val packageMemberNames = when (clazz) {
                        is PackageFacadeKotlinClassSnapshot -> clazz.packageMemberNames
                        else -> (clazz as MultifileClassKotlinClassSnapshot).constantNames
                    }
                    packageMemberNames.any { it in lookupNamesInScope }
                }
            }
        }
        return referencedClasses
    }

    /**
     * Finds classes that are *transitively* referenced from the given classes. For example, if a subclass is referenced, its supertypes
     * will be transitively referenced.
     *
     * The returned list is *inclusive* (it contains the given list + the transitively referenced ones).
     */
    private fun findTransitivelyReferencedClasses(
        allClasses: List<AccessibleClassSnapshot>,
        referencedClasses: List<AccessibleClassSnapshot>
    ): List<AccessibleClassSnapshot> {
        val referencedClassIds = referencedClasses.map { it.classId }
        val impactingClassesResolver = AllImpacts.getReverseResolver(allClasses)
        val transitivelyReferencedClassIds: Set<ClassId> = /* Must be a Set for the presence check below */
            findReachableNodes(referencedClassIds, impactingClassesResolver::getImpactingClasses)

        return allClasses.filter { it.classId in transitivelyReferencedClassIds }
    }

    /**
     * Helper class to allow the caller of [ClasspathSnapshotShrinker] to provide a list of [BuildTime]s as different callers may want to
     * record different [BuildTime]s (because the [BuildTime.parent]s are different).
     */
    class MetricsReporter(
        private val metrics: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>? = null,
        private val getLookupSymbols: GradleBuildTime? = null,
        private val findReferencedClasses: GradleBuildTime? = null,
        private val findTransitivelyReferencedClasses: GradleBuildTime? = null
    ) {
        fun <T> getLookupSymbols(fn: () -> T) = metrics?.measure(getLookupSymbols!!, fn) ?: fn()
        fun <T> findReferencedClasses(fn: () -> T) = metrics?.measure(findReferencedClasses!!, fn) ?: fn()
        fun <T> findTransitivelyReferencedClasses(fn: () -> T) = metrics?.measure(findTransitivelyReferencedClasses!!, fn) ?: fn()
    }
}

/**
 * Removes duplicate classes and [InaccessibleClassSnapshot]s from the given [ClasspathSnapshot].
 *
 * To see why removing duplicate classes is important, consider this example:
 *   - Current classpath: (Unchanged) jar2!/com/example/A.class containing A.foo, (Added) jar3!/com/example/A.class containing A.bar
 *   - Previous classpath: (Removed) jar1!/com/example/A.class containing A.bar, (Unchanged) jar2!/com/example/A.class containing A.foo
 * Without removing duplicates, we might report that there are no changes (both the current classpath and previous classpath have A.foo and
 * A.bar). However, the correct report should be that A.bar is removed and A.foo is added because the second A class on each classpath does
 * not have any effect.
 *
 * It's also important to remove duplicate classes first before removing [InaccessibleClassSnapshot]s. For example, if
 * jar1!/com/example/A.class is inaccessible and jar2!/com/example/A.class is accessible, removing inaccessible classes first would mean
 * that jar2!/com/example/A.class would be kept whereas it shouldn't be since it is a duplicate class (keeping a duplicate class can
 * lead to incorrect change reports as shown in the previous example).
 *
 * That is also why we cannot remove inaccessible classes from each classpath entry in isolation (i.e., during classpath entry
 * snapshotting), even though it seems more efficient to do so. For correctness, we need to look at the entire classpath first, remove
 * duplicate classes, and then remove inaccessible classes.
 */
internal fun ClasspathSnapshot.removeDuplicateAndInaccessibleClasses(): List<AccessibleClassSnapshot> {
    return getNonDuplicateClassSnapshots().filterIsInstance<AccessibleClassSnapshot>()
}

/**
 * Returns all [ClassSnapshot]s in this [ClasspathSnapshot].
 *
 * If there are duplicate classes on the classpath, retain only the first one to match the compiler's behavior.
 */
private fun ClasspathSnapshot.getNonDuplicateClassSnapshots(): List<ClassSnapshot> {
    val classSnapshots = LinkedHashMap<String, ClassSnapshot>(classpathEntrySnapshots.sumOf { it.classSnapshots.size })
    for (classpathEntrySnapshot in classpathEntrySnapshots) {
        for ((unixStyleRelativePath, classSnapshot) in classpathEntrySnapshot.classSnapshots) {
            classSnapshots.putIfAbsent(unixStyleRelativePath, classSnapshot)
        }
    }
    return classSnapshots.values.toList()
}

/** Used by [shrinkAndSaveClasspathSnapshot]. */
private sealed class ShrinkMode {
    object UnchangedLookupsUnchangedClasspath : ShrinkMode()

    class UnchangedLookupsChangedClasspath(
        val currentClasspathSnapshot: List<AccessibleClassSnapshot>,
        val shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>
    ) : ShrinkMode()

    sealed class ChangedLookups : ShrinkMode() {
        abstract val addedLookupSymbols: Set<LookupSymbolKey>
    }

    class ChangedLookupsUnchangedClasspath(
        override val addedLookupSymbols: Set<LookupSymbolKey>
    ) : ChangedLookups()

    class ChangedLookupsChangedClasspath(
        override val addedLookupSymbols: Set<LookupSymbolKey>,
        val currentClasspathSnapshot: List<AccessibleClassSnapshot>,
        val shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>
    ) : ChangedLookups()

    object NonIncremental : ShrinkMode()
}

internal fun shrinkAndSaveClasspathSnapshot(
    compilationWasIncremental: Boolean,
    classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
    lookupStorage: LookupStorage,
    currentClasspathSnapshot: List<AccessibleClassSnapshot>?, // Not null iff classpathChanges is ToBeComputedByIncrementalCompiler
    shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>?, // Not null iff classpathChanges is ToBeComputedByIncrementalCompiler
    reporter: ClasspathSnapshotBuildReporter
) {
    // In the following, we'll try to shrink the classpath snapshot incrementally when possible.
    // For incremental shrinking, we currently use only lookupStorage.addedLookupSymbols, not lookupStorage.removedLookupSymbols. It is
    // because updating the shrunk classpath snapshot for removedLookupSymbols is expensive. Therefore, the shrunk classpath snapshot may be
    // larger than necessary (and non-deterministic), but it is okay for it to be an over-approximation.
    val shrinkMode = if (!compilationWasIncremental) {
        ShrinkMode.NonIncremental
    } else when (classpathChanges) {
        is NoChanges -> {
            val addedLookupSymbols = lookupStorage.addedLookupSymbols
            if (addedLookupSymbols.isEmpty()) {
                ShrinkMode.UnchangedLookupsUnchangedClasspath
            } else {
                ShrinkMode.ChangedLookupsUnchangedClasspath(addedLookupSymbols)
            }
        }
        is ToBeComputedByIncrementalCompiler -> {
            val addedLookupSymbols = lookupStorage.addedLookupSymbols
            if (addedLookupSymbols.isEmpty()) {
                ShrinkMode.UnchangedLookupsChangedClasspath(
                    currentClasspathSnapshot!!,
                    shrunkCurrentClasspathAgainstPreviousLookups!!
                )
            } else {
                ShrinkMode.ChangedLookupsChangedClasspath(
                    addedLookupSymbols,
                    currentClasspathSnapshot!!,
                    shrunkCurrentClasspathAgainstPreviousLookups!!
                )
            }
        }
        is NotAvailableDueToMissingClasspathSnapshot -> ShrinkMode.NonIncremental
        is NotAvailableForNonIncrementalRun -> error("NotAvailableForNonIncrementalRun is not expected as compilationWasIncremental==true")
    }

    // Shrink current classpath against current lookups
    val (currentClasspath: List<AccessibleClassSnapshot>?, shrunkCurrentClasspath: List<AccessibleClassSnapshot>?) = when (shrinkMode) {
        is ShrinkMode.UnchangedLookupsUnchangedClasspath -> {
            // There are no changes in the lookups and classpath, so there will be no changes in the shrunk classpath snapshot compared to
            // the previous run. Return null here as we don't need to compute this.
            null to null
        }
        is ShrinkMode.UnchangedLookupsChangedClasspath -> {
            // There are no changes in the lookups, so
            // shrunkCurrentClasspathAgainst[*Current*]Lookups == shrunkCurrentClasspathAgainst[*Previous*]Lookups
            shrinkMode.currentClasspathSnapshot to shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups
        }
        is ShrinkMode.ChangedLookups -> reporter.measure(GradleBuildTime.INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT) {
            // There are changes in the lookups, so we will shrink incrementally.
            val currentClasspath = reporter.measure(GradleBuildTime.INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT) {
                when (shrinkMode) {
                    is ShrinkMode.ChangedLookupsUnchangedClasspath ->
                        CachedClasspathSnapshotSerializer
                            .load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles, reporter)
                            .removeDuplicateAndInaccessibleClasses()
                    is ShrinkMode.ChangedLookupsChangedClasspath -> shrinkMode.currentClasspathSnapshot
                }
            }
            val shrunkCurrentClasspathAgainstPrevLookups =
                reporter.measure(GradleBuildTime.INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS) {
                    when (shrinkMode) {
                        is ShrinkMode.ChangedLookupsUnchangedClasspath -> {
                            // There are no changes in the classpath, so
                            // shrunk[*Current*]ClasspathAgainstPreviousLookups == shrunk[*Previous*]ClasspathAgainstPreviousLookups
                            ListExternalizer(AccessibleClassSnapshotExternalizer)
                                .loadFromFile(classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile)
                        }
                        is ShrinkMode.ChangedLookupsChangedClasspath -> shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups
                    }
                }

            val shrunkClasses = shrunkCurrentClasspathAgainstPrevLookups.mapTo(mutableSetOf()) { it.classId }
            val notYetShrunkClasses = currentClasspath.filter { it.classId !in shrunkClasses }
            val shrunkRemainingClassesAgainstNewLookups = shrinkClasses(notYetShrunkClasses, shrinkMode.addedLookupSymbols)

            val shrunkCurrentClasspath = shrunkCurrentClasspathAgainstPrevLookups + shrunkRemainingClassesAgainstNewLookups
            currentClasspath to shrunkCurrentClasspath
        }
        is ShrinkMode.NonIncremental -> {
            // Changes in the lookups and classpath are not available, so we will shrink non-incrementally.
            reporter.measure(GradleBuildTime.NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT) {
                val currentClasspath = reporter.measure(GradleBuildTime.NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT) {
                    CachedClasspathSnapshotSerializer
                        .load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles, reporter)
                        .removeDuplicateAndInaccessibleClasses()
                }
                val shrunkCurrentClasspath = shrinkClasspath(currentClasspath, lookupStorage)
                currentClasspath to shrunkCurrentClasspath
            }
        }
    }

    if (shrinkMode == ShrinkMode.UnchangedLookupsUnchangedClasspath) {
        // There are no changes in the lookups and classpath, so there will be no changes in the shrunk classpath snapshot compared to the
        // previous run. Just double-check that the file still exists.
        check(classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.isFile) {
            "File '${classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.path}' does not exist"
        }
    } else {
        reporter.measure(GradleBuildTime.SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT) {
            ListExternalizer(AccessibleClassSnapshotExternalizer).saveToFile(
                classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile,
                shrunkCurrentClasspath!!
            )
        }
    }

    reporter.debug {
        "Shrunk current classpath snapshot after compilation (shrink mode = ${shrinkMode::class.simpleName})" + when (shrinkMode) {
            is ShrinkMode.UnchangedLookupsUnchangedClasspath -> ", no updates since previous run"
            else -> ", retained ${shrunkCurrentClasspath!!.size} / ${currentClasspath!!.size} classes"
        }
    }

    reporter.addMetric(GradleBuildPerformanceMetric.SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, 1)
    reporter.addMetric(
        GradleBuildPerformanceMetric.CLASSPATH_ENTRY_COUNT,
        classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.size.toLong()
    )
    reporter.addMetric(
        GradleBuildPerformanceMetric.CLASSPATH_SNAPSHOT_SIZE,
        classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.sumOf { it.length() }
    )
    reporter.addMetric(
        GradleBuildPerformanceMetric.SHRUNK_CLASSPATH_SNAPSHOT_SIZE,
        classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.length()
    )
}
