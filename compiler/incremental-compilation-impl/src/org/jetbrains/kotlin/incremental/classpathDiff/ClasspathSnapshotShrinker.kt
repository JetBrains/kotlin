/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrink
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

object ClasspathSnapshotShrinker {

    /**
     * Shrinks the given classes by retaining only classes that are referenced by the lookup symbols stored in the given [LookupStorage].
     */
    fun shrink(
        allClasses: List<ClassSnapshotWithHash>,
        lookupStorage: LookupStorage,
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): List<ClassSnapshotWithHash> {
        val lookupSymbols = metrics.measure(BuildTime.GET_LOOKUP_SYMBOLS) {
            lookupStorage.lookupSymbols
                .map { LookupSymbol(it.name, it.scope) }
                .filter(allClasses.map { it.classSnapshot })
        }
        return shrink(allClasses, lookupSymbols, metrics)
    }

    /** Shrinks the given classes by retaining only classes that are referenced by the given lookup symbols. */
    fun shrink(
        allClasses: List<ClassSnapshotWithHash>,
        lookupSymbols: List<ProgramSymbol>,
        metrics: BuildMetricsReporter = DoNothingBuildMetricsReporter
    ): List<ClassSnapshotWithHash> {
        val referencedClasses = metrics.measure(BuildTime.FIND_REFERENCED_CLASSES) {
            findReferencedClasses(allClasses, lookupSymbols)
        }
        return metrics.measure(BuildTime.FIND_TRANSITIVELY_REFERENCED_CLASSES) {
            findTransitivelyReferencedClasses(allClasses, referencedClasses)
        }
    }

    /**
     * Finds classes that are referenced by the given lookup symbols.
     *
     * Note: It's okay to over-approximate referenced classes.
     */
    private fun findReferencedClasses(
        allClasses: List<ClassSnapshotWithHash>,
        lookupSymbols: List<ProgramSymbol>
    ): List<ClassSnapshotWithHash> {
        val lookedUpClassIds: Set<ClassId> = lookupSymbols.mapNotNullTo(mutableSetOf()) {
            when (it) {
                is ClassSymbol -> it.classId
                is ClassMember -> it.classId
                is PackageMember -> null
            }
        }
        val lookedUpPackageMembers: Set<PackageMember> = lookupSymbols.filterIsInstanceTo(mutableSetOf())

        return allClasses.filter {
            val isPackageFacade =
                it.classSnapshot is KotlinClassSnapshot && it.classSnapshot.classInfo.classKind != KotlinClassHeader.Kind.CLASS
            if (isPackageFacade) {
                // If packageMembers == null (e.g., if classKind == KotlinClassHeader.Kind.MULTIFILE_CLASS -- see
                // `KotlinClassSnapshot.packageMembers`'s kdoc), it means that we don't have the information, so we will always include the
                // class (it's okay to over-approximate the result).
                (it.classSnapshot as KotlinClassSnapshot).packageMembers?.any { member -> member in lookedUpPackageMembers } ?: true
            } else {
                it.classSnapshot.getClassId() in lookedUpClassIds
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
        allClasses: List<ClassSnapshotWithHash>,
        referencedClasses: List<ClassSnapshotWithHash>
    ): List<ClassSnapshotWithHash> {
        val classIdToClassSnapshot = allClasses.associateBy { it.classSnapshot.getClassId() }
        val classIds: Set<ClassId> = classIdToClassSnapshot.keys // Use Set for presence check
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val supertypesResolver = { classId: ClassId ->
            // No need to collect supertypes outside the given set of classes (e.g., "java/lang/Object")
            @Suppress("SimpleRedundantLet")
            classIdToClassSnapshot[classId]?.let {
                it.classSnapshot.getSupertypes(classNameToClassIdResolver).filter { supertype -> supertype in classIds }.toSet()
            } ?: emptySet()
        }

        val referencedClassIds = referencedClasses.map { it.classSnapshot.getClassId() }.toSet()
        val transitivelyReferencedClassIds: Set<ClassId> =
            ImpactAnalysis.findImpactedClassesInclusive(referencedClassIds, supertypesResolver) // Use Set for presence check

        return allClasses.filter { it.classSnapshot.getClassId() in transitivelyReferencedClassIds }
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
internal fun ClasspathSnapshot.removeDuplicateAndInaccessibleClasses(): List<ClassSnapshotWithHash> {
    return getNonDuplicateClassSnapshots().filter { it.classSnapshot !is InaccessibleClassSnapshot }
}

/**
 * Returns all [ClassSnapshot]s in this [ClasspathSnapshot].
 *
 * If there are duplicate classes on the classpath, retain only the first one to match the compiler's behavior.
 */
private fun ClasspathSnapshot.getNonDuplicateClassSnapshots(): List<ClassSnapshotWithHash> {
    val classSnapshots = LinkedHashMap<String, ClassSnapshotWithHash>(classpathEntrySnapshots.sumOf { it.classSnapshots.size })
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
        val shrunkCurrentClasspathAgainstPreviousLookups: List<ClassSnapshotWithHash>,
    ) : ShrinkMode()

    class Incremental(
        val currentClasspathSnapshot: List<ClassSnapshotWithHash>,
        val shrunkCurrentClasspathAgainstPreviousLookups: List<ClassSnapshotWithHash>,
        val addedLookupSymbols: Set<LookupSymbolKey>
    ) : ShrinkMode()

    object NonIncremental : ShrinkMode()
}

internal fun shrinkAndSaveClasspathSnapshot(
    classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
    lookupStorage: LookupStorage,
    currentClasspathSnapshot: List<ClassSnapshotWithHash>?, // Not null iff classpathChanges is ToBeComputedByIncrementalCompiler
    shrunkCurrentClasspathAgainstPreviousLookups: List<ClassSnapshotWithHash>?, // Same as above
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
                        ListExternalizer(ClassSnapshotWithHashExternalizer)
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
    val shrunkCurrentClasspath: List<ClassSnapshotWithHash>? = when (shrinkMode) {
        is ShrinkMode.NoChanges -> null
        is ShrinkMode.IncrementalNoNewLookups -> {
            // There are no new lookups, so
            // shrunkCurrentClasspathAgainst[*Current*]Lookups == shrunkCurrentClasspathAgainst[*Previous*]Lookups
            shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups
        }
        is ShrinkMode.Incremental -> metrics.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
            val shrunkClasses = shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups.map { it.classSnapshot.getClassId() }.toSet()
            val notYetShrunkClasses = shrinkMode.currentClasspathSnapshot.filter { it.classSnapshot.getClassId() !in shrunkClasses }
            val addedLookupSymbols = shrinkMode.addedLookupSymbols
                .map { LookupSymbol(it.name, it.scope) }
                .filter(shrinkMode.currentClasspathSnapshot.map { it.classSnapshot })
            // Don't provide a BuildMetricsReporter for the following call as the sub-BuildTimes in it have a different parent
            val shrunkRemainingClassesAgainstNewLookups = shrink(notYetShrunkClasses, addedLookupSymbols)

            shrinkMode.shrunkCurrentClasspathAgainstPreviousLookups + shrunkRemainingClassesAgainstNewLookups
        }
        is ShrinkMode.NonIncremental -> {
            val classpathSnapshot = metrics.measure(BuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                CachedClasspathSnapshotSerializer
                    .load(classpathChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles)
                    .removeDuplicateAndInaccessibleClasses()
            }
            metrics.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                // Don't provide a BuildMetricsReporter for the following call as the sub-BuildTimes in it have a different parent
                shrink(classpathSnapshot, lookupStorage)
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
            ListExternalizer(ClassSnapshotWithHashExternalizer).saveToFile(
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
