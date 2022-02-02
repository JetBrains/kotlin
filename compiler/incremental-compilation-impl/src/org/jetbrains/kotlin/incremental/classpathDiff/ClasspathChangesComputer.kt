/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.classpathDiff.ImpactAnalysis.computeImpactedSet
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.util.*

/** Computes changes between two [ClasspathSnapshot]s .*/
object ClasspathChangesComputer {

    /**
     * Computes changes between the current and previous shrunk [ClasspathSnapshot]s, plus unchanged elements that are impacted by the
     * changes.
     *
     * NOTE: The original classpath may contain duplicate classes, but the shrunk classpath must not contain duplicate classes.
     */
    fun computeChangedAndImpactedSet(
        shrunkCurrentClasspathSnapshot: List<AccessibleClassSnapshot>,
        shrunkPreviousClasspathSnapshotFile: File,
        metrics: BuildMetricsReporter
    ): ChangeSet {
        val shrunkPreviousClasspathSnapshot = metrics.measure(BuildTime.LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT) {
            ListExternalizer(AccessibleClassSnapshotExternalizer).loadFromFile(shrunkPreviousClasspathSnapshotFile)
        }
        return metrics.measure(BuildTime.COMPUTE_CHANGED_AND_IMPACTED_SET) {
            computeChangedAndImpactedSet(shrunkCurrentClasspathSnapshot, shrunkPreviousClasspathSnapshot, metrics)
        }
    }

    /**
     * Computes changes between the current and previous lists of classes, plus unchanged elements that are impacted by the changes.
     *
     * NOTE: Each list of classes must not contain duplicates.
     */
    fun computeChangedAndImpactedSet(
        currentClassSnapshots: List<AccessibleClassSnapshot>,
        previousClassSnapshots: List<AccessibleClassSnapshot>,
        metrics: BuildMetricsReporter
    ): ChangeSet {
        val currentClasses: Map<ClassId, AccessibleClassSnapshot> = currentClassSnapshots.associateBy { it.classId }
        val previousClasses: Map<ClassId, AccessibleClassSnapshot> = previousClassSnapshots.associateBy { it.classId }

        val changedCurrentClasses: List<AccessibleClassSnapshot> = currentClasses.mapNotNull { (classId, currentClass) ->
            val previousClass = previousClasses[classId]
            if (previousClass == null || currentClass.classAbiHash != previousClass.classAbiHash) {
                currentClass
            } else null
        }

        val changedPreviousClasses: List<AccessibleClassSnapshot> = previousClasses.mapNotNull { (classId, previousClass) ->
            val currentClass = currentClasses[classId]
            if (currentClass == null || currentClass.classAbiHash != previousClass.classAbiHash) {
                previousClass
            } else null
        }

        val classChanges = metrics.measure(BuildTime.COMPUTE_CLASS_CHANGES) {
            computeClassChanges(changedCurrentClasses, changedPreviousClasses, metrics)
        }

        if (classChanges.isEmpty()) {
            return classChanges
        }

        return metrics.measure(BuildTime.COMPUTE_IMPACTED_SET) {
            computeImpactedSet(classChanges, previousClassSnapshots)
        }
    }

    /**
     * Computes changes between the current and previous lists of classes. The returned result does not need to include elements that are
     * impacted by the changes.
     *
     * NOTE: Each list of classes must not contain duplicates.
     */
    private fun computeClassChanges(
        currentClassSnapshots: List<AccessibleClassSnapshot>,
        previousClassSnapshots: List<AccessibleClassSnapshot>,
        metrics: BuildMetricsReporter
    ): ChangeSet {
        val (currentKotlinClassSnapshots, currentJavaClassSnapshots) = currentClassSnapshots.partition { it is KotlinClassSnapshot }
        val (previousKotlinClassSnapshots, previousJavaClassSnapshots) = previousClassSnapshots.partition { it is KotlinClassSnapshot }

        @Suppress("UNCHECKED_CAST")
        val kotlinClassChanges = metrics.measure(BuildTime.COMPUTE_KOTLIN_CLASS_CHANGES) {
            computeKotlinClassChanges(
                currentKotlinClassSnapshots as List<KotlinClassSnapshot>,
                previousKotlinClassSnapshots as List<KotlinClassSnapshot>
            )
        }

        @Suppress("UNCHECKED_CAST")
        val javaClassChanges = metrics.measure(BuildTime.COMPUTE_JAVA_CLASS_CHANGES) {
            JavaClassChangesComputer.compute(
                currentJavaClassSnapshots as List<JavaClassSnapshot>,
                previousJavaClassSnapshots as List<JavaClassSnapshot>
            )
        }

        return kotlinClassChanges + javaClassChanges
    }

    private fun computeKotlinClassChanges(
        currentClassSnapshots: List<KotlinClassSnapshot>,
        previousClassSnapshots: List<KotlinClassSnapshot>
    ): ChangeSet {
        val (coarseGrainedCurrentClassSnapshots, fineGrainedCurrentClassSnapshots) =
            currentClassSnapshots.partition { it.classMemberLevelSnapshot == null }
        val (coarseGrainedPreviousClassSnapshots, fineGrainedPreviousClassSnapshots) =
            previousClassSnapshots.partition { it.classMemberLevelSnapshot == null }

        return computeCoarseGrainedKotlinClassChanges(coarseGrainedCurrentClassSnapshots, coarseGrainedPreviousClassSnapshots) +
                computeFineGrainedKotlinClassChanges(fineGrainedCurrentClassSnapshots, fineGrainedPreviousClassSnapshots)
    }

    private fun computeCoarseGrainedKotlinClassChanges(
        currentClassSnapshots: List<KotlinClassSnapshot>,
        previousClassSnapshots: List<KotlinClassSnapshot>
    ): ChangeSet {
        // Note: We have removed unchanged classes earlier in computeChangedAndImpactedSet method, so here we only have changed classes.
        return ChangeSet.Collector().run {
            (currentClassSnapshots + previousClassSnapshots).forEach {
                when (it) {
                    is RegularKotlinClassSnapshot -> addChangedClass(it.classId)
                    is PackageFacadeKotlinClassSnapshot -> addChangedTopLevelMembers(it.packageMembers)
                    is MultifileClassKotlinClassSnapshot -> addChangedTopLevelMembers(it.constants)
                }
            }
            getChanges()
        }
    }

    private fun computeFineGrainedKotlinClassChanges(
        currentClassSnapshots: List<KotlinClassSnapshot>,
        previousClassSnapshots: List<KotlinClassSnapshot>
    ): ChangeSet {
        val workingDir =
            FileUtil.createTempDirectory(this::class.java.simpleName, "_WorkingDir_${UUID.randomUUID()}", /* deleteOnExit */ true)
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToCanonicalPathConverter)

        // Step 1:
        //   - Add previous class snapshots to incrementalJvmCache.
        //   - Internally, incrementalJvmCache maintains a set of dirty classes to detect removed classes. Add previous classes to this set
        //     to detect removed classes later (see step 2).
        //   - The ChangesCollector result will contain symbols in the previous classes (we actually don't need them, but it's part of the
        //     API's effects).
        val unusedChangesCollector = ChangesCollector()
        previousClassSnapshots.forEach {
            incrementalJvmCache.saveClassToCache(
                kotlinClassInfo = it.classMemberLevelSnapshot!!,
                sourceFiles = null,
                changesCollector = unusedChangesCollector
            )
            incrementalJvmCache.markDirty(it.classMemberLevelSnapshot!!.className)
        }

        // Step 2:
        //   - Add current class snapshots to incrementalJvmCache. This will overwrite any previous class snapshots that have the same
        //     `JvmClassName`. The remaining previous class snapshots will be removed in step 3.
        //   - Internally, incrementalJvmCache will remove current classes from the set of dirty classes. After this, the remaining dirty
        //     classes will be classes that are present on the previous classpath but not on the current classpath (i.e., removed classes).
        //   - The intermediate ChangesCollector result will contain symbols in added classes and changed (added/modified/removed) symbols
        //     in modified classes. We will collect symbols in removed classes in step 3.
        val changesCollector = ChangesCollector()
        currentClassSnapshots.forEach {
            incrementalJvmCache.saveClassToCache(
                kotlinClassInfo = it.classMemberLevelSnapshot!!,
                sourceFiles = null,
                changesCollector = changesCollector
            )
        }

        // Step 3:
        //   - Detect removed classes: They are the remaining dirty classes.
        //   - Remove class snapshots of removed classes from incrementalJvmCache.
        //   - The final ChangesCollector result will contain symbols in added classes, changed (added/modified/removed) symbols in modified
        //     classes, and symbols in removed classes.
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)

        // Normalize the changes and clean up
        val dirtyData = changesCollector.getDirtyData(listOf(incrementalJvmCache), EmptyICReporter)
        workingDir.deleteRecursively()

        return dirtyData.normalize(currentClassSnapshots, previousClassSnapshots)
    }

    private fun DirtyData.normalize(
        currentClassSnapshots: List<AccessibleClassSnapshot>,
        previousClassSnapshots: List<AccessibleClassSnapshot>
    ): ChangeSet {
        val changedLookupSymbols =
            dirtyLookupSymbols.filterLookupSymbols(currentClassSnapshots).toSet() +
                    dirtyLookupSymbols.filterLookupSymbols(previousClassSnapshots)

        val changes = ChangeSet.Collector().run {
            changedLookupSymbols.forEach {
                when (it) {
                    is ClassSymbol -> addChangedClass(it.classId)
                    is ClassMember -> addChangedClassMember(it.classId, it.memberName)
                    is PackageMember -> addChangedTopLevelMember(it.packageFqName, it.memberName)
                }
            }
            getChanges()
        }

        // DirtyData contains:
        //   1. dirtyLookupSymbols => This contains all info we need (extracted above).
        //   2. dirtyClassesFqNames => This should be derived from dirtyLookupSymbols.
        //   3. dirtyClassesFqNamesForceRecompile => Should be irrelevant.
        // Double-check that the assumption at bullet 2 above is correct.
        val derivedDirtyFqNames: Set<FqName> = dirtyLookupSymbols.flatMap {
            val lookupSymbolFqName = if (it.scope.isEmpty()) FqName(it.name) else FqName("${it.scope}.${it.name}")
            val scopeFqName = FqName(it.scope)
            listOf(lookupSymbolFqName, scopeFqName)
        }.toSet()
        (dirtyClassesFqNames - derivedDirtyFqNames).let {
            check(it.isEmpty()) { "FqNames found in dirtyClassesFqNames but not in dirtyLookupSymbols: $it" }
        }

        return changes
    }
}

internal object ImpactAnalysis {

    /**
     * Computes the set of classes, class members, and top-level members that are impacted by the given changes.
     *
     * For example, if a superclass has changed, any of its subclasses will be impacted even if it has not changed because unchanged source
     * files in the previous compilation that depended on the subclasses will need to be recompiled.
     *
     * The returned set is also a [ChangeSet], which includes the given changes plus the impacted ones.
     */
    fun computeImpactedSet(changes: ChangeSet, previousClassSnapshots: List<AccessibleClassSnapshot>): ChangeSet {
        val classIdToSubclasses = getClassIdToSubclassesMap(previousClassSnapshots)
        val impactedClassesResolver = { classId: ClassId -> classIdToSubclasses[classId] ?: emptySet() }

        return ChangeSet.Collector().run {
            addChangedClasses(findImpactedClassesInclusive(changes.changedClasses, impactedClassesResolver))
            for ((changedClass, changedClassMembers) in changes.changedClassMembers) {
                findImpactedClassesInclusive(setOf(changedClass), impactedClassesResolver).forEach {
                    addChangedClassMembers(it, changedClassMembers)
                }
            }
            for ((changedPackage, changedClassMembers) in changes.changedTopLevelMembers) {
                addChangedTopLevelMembers(changedPackage, changedClassMembers)
            }
            getChanges()
        }
    }

    private fun getClassIdToSubclassesMap(classSnapshots: List<AccessibleClassSnapshot>): Map<ClassId, Set<ClassId>> {
        val classIds: Set<ClassId> = classSnapshots.map { it.classId }.toSet() // Use Set for presence check
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val classIdToSubclasses = mutableMapOf<ClassId, MutableSet<ClassId>>()
        classSnapshots.forEach { classSnapshot ->
            val classId = classSnapshot.classId
            classSnapshot.getSupertypes(classNameToClassIdResolver).forEach { supertype ->
                // No need to collect supertypes outside the given set of classes (e.g., "java/lang/Object")
                if (supertype in classIds) {
                    classIdToSubclasses.computeIfAbsent(supertype) { mutableSetOf() }.add(classId)
                }
            }
        }
        return classIdToSubclasses
    }

    /**
     * Finds directly and transitively impacted classes of the given classes. The return set includes both the given classes and the
     * impacted classes.
     */
    fun findImpactedClassesInclusive(classIds: Set<ClassId>, impactedClassesResolver: (ClassId) -> Set<ClassId>): Set<ClassId> {
        // Standard Breadth-First Search
        val visitedAndToVisitClasses = classIds.toMutableSet()
        val classesToVisit = ArrayDeque(classIds)

        while (classesToVisit.isNotEmpty()) {
            val classToVisit = classesToVisit.removeFirst()
            val nextClassesToVisit = impactedClassesResolver.invoke(classToVisit) - visitedAndToVisitClasses
            visitedAndToVisitClasses.addAll(nextClassesToVisit)
            classesToVisit.addAll(nextClassesToVisit)
        }
        return visitedAndToVisitClasses
    }
}

/**
 * Returns the [ClassId]s of the supertypes of this class (could be empty in some cases).
 *
 * @param classIdResolver Resolves the [ClassId] from the [JvmClassName] of a supertype. It may return null if the supertype is outside the
 *     considered set of classes (e.g., "java/lang/Object"). Those supertypes do not need to be included in the returned result.
 */
internal fun AccessibleClassSnapshot.getSupertypes(classIdResolver: (JvmClassName) -> ClassId?): List<ClassId> {
    return when (this) {
        is RegularKotlinClassSnapshot -> supertypes.mapNotNull { classIdResolver.invoke(it) }
        is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> {
            // These classes may have supertypes (e.g., kotlin/collections/ArraysKt (MULTIFILE_CLASS) extends
            // kotlin/collections/ArraysKt___ArraysKt (MULTIFILE_CLASS_PART)), but we don't have to use that info during impact analysis
            // because those inheritors and supertypes should have the same package names, and in package facades only the package names and
            // member names matter.
            emptyList()
        }
        is JavaClassSnapshot -> supertypes.mapNotNull { classIdResolver.invoke(it) }
    }
}
