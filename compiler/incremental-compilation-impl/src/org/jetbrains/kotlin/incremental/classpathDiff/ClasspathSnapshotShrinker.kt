/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasses
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasspath
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

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
                .map { LookupSymbol(name = it.name, scope = it.scope) }
                .filterLookupSymbols(allClasses)
        }
        return shrinkClasses(allClasses, lookupSymbols, metrics)
    }

    /** Shrinks the given classes by retaining only classes that are referenced by the given lookup symbols. */
    fun shrinkClasses(
        allClasses: List<AccessibleClassSnapshot>,
        lookupSymbols: List<ProgramSymbol>,
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
     * Finds classes that are referenced by the given lookup symbols.
     *
     * Note: It's okay to over-approximate the result.
     */
    private fun findReferencedClasses(
        allClasses: List<AccessibleClassSnapshot>,
        lookupSymbols: List<ProgramSymbol>
    ): List<AccessibleClassSnapshot> {
        val lookedUpClassIds: Set<ClassId> = lookupSymbols.mapNotNullTo(mutableSetOf()) {
            when (it) {
                is ClassSymbol -> it.classId
                is ClassMember -> it.classId
                is PackageMember -> null
            }
        }
        val lookedUpPackageMembers: PackageMemberSet =
            lookupSymbols.filterIsInstanceTo<PackageMember, MutableSet<PackageMember>>(mutableSetOf()).compact()

        return allClasses.filter {
            when (it) {
                is RegularKotlinClassSnapshot, is JavaClassSnapshot -> it.classId in lookedUpClassIds
                is PackageFacadeKotlinClassSnapshot -> it.packageMembers.containsElementsIn(lookedUpPackageMembers)
                is MultifileClassKotlinClassSnapshot -> it.constants.containsElementsIn(lookedUpPackageMembers)
            }
        }
    }

    /**
     * Finds classes that are transitively referenced. For example, if a subclass is referenced, its supertypes will potentially be
     * referenced.
     *
     * The returned list includes the given referenced classes plus the transitively referenced ones.
     */
    private fun findTransitivelyReferencedClasses(
        allClasses: List<AccessibleClassSnapshot>,
        referencedClasses: List<AccessibleClassSnapshot>
    ): List<AccessibleClassSnapshot> {
        val classIdToClassSnapshot = allClasses.associateBy { it.classId }
        val classIds: Set<ClassId> = classIdToClassSnapshot.keys // Use Set for presence check
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val supertypesResolver = { classId: ClassId ->
            // No need to collect supertypes outside the given set of classes (e.g., "java/lang/Object")
            @Suppress("SimpleRedundantLet")
            classIdToClassSnapshot[classId]?.let {
                it.getSupertypes(classNameToClassIdResolver).filterTo(mutableSetOf()) { supertype -> supertype in classIds }
            } ?: emptySet()
        }

        val referencedClassIds = referencedClasses.mapTo(mutableSetOf()) { it.classId }
        val transitivelyReferencedClassIds: Set<ClassId> =
            ImpactAnalysis.findImpactedClassesInclusive(referencedClassIds, supertypesResolver) // Use Set for presence check

        return allClasses.filter { it.classId in transitivelyReferencedClassIds }
    }

    class MetricsReporter(
        private val metrics: BuildMetricsReporter? = null,
        private val getLookupSymbols: BuildTime? = null,
        private val findReferencedClasses: BuildTime? = null,
        private val findTransitivelyReferencedClasses: BuildTime? = null
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
    object NoChanges : ShrinkMode()

    class IncrementalNoNewLookups(
        val shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>,
    ) : ShrinkMode()

    class Incremental(
        val currentClasspathSnapshot: List<AccessibleClassSnapshot>,
        val shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>,
        val addedLookupSymbols: Set<LookupSymbolKey>
    ) : ShrinkMode()

    object NonIncremental : ShrinkMode()
}

internal fun shrinkAndSaveClasspathSnapshot(
    classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
    lookupStorage: LookupStorage,
    currentClasspathSnapshot: List<AccessibleClassSnapshot>?, // Not null iff classpathChanges is ToBeComputedByIncrementalCompiler
    shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>?, // Same as above
    metrics: BuildMetricsReporter
) {
    // In the following, we'll try to shrink the classpath snapshot incrementally when possible.
    // For incremental shrinking, we currently use only lookupStorage.addedLookupSymbols, not lookupStorage.removedLookupSymbols. It is
    // because updating the shrunk classpath snapshot for removedLookupSymbols is expensive. Therefore, the shrunk classpath snapshot may be
    // larger than necessary (and non-deterministic), but it is okay for it to be an over-approximation.
    val shrinkMode = when (classpathChanges) {
        is NoChanges -> {
            val addedLookupSymbols = lookupStorage.addedLookupSymbols
            if (addedLookupSymbols.isEmpty()) {
                ShrinkMode.NoChanges
            } else {
                val shrunkPreviousClasspathAgainstPreviousLookups =
                    metrics.measure(BuildTime.LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                        ListExternalizer(AccessibleClassSnapshotExternalizer)
                            .loadFromFile(classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile)
                    }
                ShrinkMode.Incremental(
                    currentClasspathSnapshot = metrics.measure(BuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                        CachedClasspathSnapshotSerializer
                            .load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles)
                            .removeDuplicateAndInaccessibleClasses()
                    },
                    // In the current case, there are no classpath changes, so
                    // shrunk[*Current*]ClasspathAgainstPreviousLookups == shrunk[*Previous*]ClasspathAgainstPreviousLookups
                    shrunkCurrentClasspathAgainstPreviousLookups = shrunkPreviousClasspathAgainstPreviousLookups,
                    addedLookupSymbols = addedLookupSymbols
                )
            }
        }
        is ToBeComputedByIncrementalCompiler -> {
            val addedLookupSymbols = lookupStorage.addedLookupSymbols
            if (addedLookupSymbols.isEmpty()) {
                ShrinkMode.IncrementalNoNewLookups(shrunkCurrentClasspathAgainstPreviousLookups!!)
            } else {
                ShrinkMode.Incremental(currentClasspathSnapshot!!, shrunkCurrentClasspathAgainstPreviousLookups!!, addedLookupSymbols)
            }
        }
        is NotAvailableDueToMissingClasspathSnapshot, is NotAvailableForNonIncrementalRun -> ShrinkMode.NonIncremental
    }

    // Shrink current classpath against current lookups
    val shrunkCurrentClasspath: List<AccessibleClassSnapshot>? = when (shrinkMode) {
        is ShrinkMode.NoChanges -> null
        is ShrinkMode.IncrementalNoNewLookups -> {
            // There are no new lookups, so
            // shrunkCurrentClasspathAgainst[*Current*]Lookups == shrunkCurrentClasspathAgainst[*Previous*]Lookups
            shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups
        }
        is ShrinkMode.Incremental -> metrics.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
            val shrunkClasses = shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups.mapTo(mutableSetOf()) { it.classId }
            val notYetShrunkClasses = shrinkMode.currentClasspathSnapshot.filter { it.classId !in shrunkClasses }
            val addedLookupSymbols = shrinkMode.addedLookupSymbols
                .map { LookupSymbol(name = it.name, scope = it.scope) }
                .filterLookupSymbols(notYetShrunkClasses)
            val shrunkRemainingClassesAgainstNewLookups = shrinkClasses(notYetShrunkClasses, addedLookupSymbols)

            shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups + shrunkRemainingClassesAgainstNewLookups
        }
        is ShrinkMode.NonIncremental -> {
            val classpathSnapshot = metrics.measure(BuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                CachedClasspathSnapshotSerializer
                    .load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles)
                    .removeDuplicateAndInaccessibleClasses()
            }
            metrics.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                shrinkClasspath(classpathSnapshot, lookupStorage)
            }
        }
    }

    if (shrinkMode == ShrinkMode.NoChanges) {
        // There are no updates to the file so just check that it exists
        check(classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.exists()) {
            "File '${classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.path}' does not exist"
        }
    } else {
        metrics.measure(BuildTime.SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
            ListExternalizer(AccessibleClassSnapshotExternalizer).saveToFile(
                classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile,
                shrunkCurrentClasspath!!
            )
        }
    }

    metrics.addMetric(
        BuildPerformanceMetric.ORIGINAL_CLASSPATH_SNAPSHOT_SIZE,
        classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.sumOf { it.length() }
    )
    metrics.addMetric(
        BuildPerformanceMetric.SHRUNK_CLASSPATH_SNAPSHOT_SIZE,
        classpathChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.length()
    )
}
