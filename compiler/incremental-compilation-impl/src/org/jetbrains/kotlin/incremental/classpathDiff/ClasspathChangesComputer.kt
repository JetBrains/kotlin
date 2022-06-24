/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasspath
import org.jetbrains.kotlin.incremental.classpathDiff.ImpactAnalysis.computeImpactedSetInclusive
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import java.util.*

/** Computes changes between two [ClasspathSnapshot]s .*/
object ClasspathChangesComputer {

    /**
     * Computes changes between the current and previous classpath, plus unchanged elements that are impacted by the changes.
     *
     * NOTE: We shrink the classpath first before comparing them. The original classpath may contain duplicate classes, but the shrunk
     * classpath must not contain duplicate classes.
     */
    fun computeClasspathChanges(
        classpathSnapshotFiles: ClasspathSnapshotFiles,
        lookupStorage: LookupStorage,
        storeCurrentClasspathSnapshotForReuse: (currentClasspathSnapshot: List<AccessibleClassSnapshot>, shrunkCurrentClasspathAgainstPreviousLookups: List<AccessibleClassSnapshot>) -> Unit,
        reporter: ClasspathSnapshotBuildReporter
    ): ProgramSymbolSet {
        val currentClasspathSnapshot = reporter.measure(BuildTime.LOAD_CURRENT_CLASSPATH_SNAPSHOT) {
            val classpathSnapshot =
                CachedClasspathSnapshotSerializer.load(classpathSnapshotFiles.currentClasspathEntrySnapshotFiles, reporter)
            reporter.measure(BuildTime.REMOVE_DUPLICATE_CLASSES) {
                classpathSnapshot.removeDuplicateAndInaccessibleClasses()
            }
        }
        val shrunkCurrentClasspathAgainstPreviousLookups = reporter.measure(BuildTime.SHRINK_CURRENT_CLASSPATH_SNAPSHOT) {
            shrinkClasspath(
                currentClasspathSnapshot, lookupStorage,
                ClasspathSnapshotShrinker.MetricsReporter(
                    reporter,
                    BuildTime.GET_LOOKUP_SYMBOLS, BuildTime.FIND_REFERENCED_CLASSES, BuildTime.FIND_TRANSITIVELY_REFERENCED_CLASSES
                )
            )
        }
        reporter.debug {
            "Shrunk current classpath snapshot for diffing," +
                    " retained ${shrunkCurrentClasspathAgainstPreviousLookups.size} / ${currentClasspathSnapshot.size} classes"
        }
        storeCurrentClasspathSnapshotForReuse(currentClasspathSnapshot, shrunkCurrentClasspathAgainstPreviousLookups)

        val shrunkPreviousClasspathSnapshot = reporter.measure(BuildTime.LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT) {
            ListExternalizer(AccessibleClassSnapshotExternalizer).loadFromFile(classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile)
        }
        reporter.debug {
            "Loaded shrunk previous classpath snapshot for diffing, found ${shrunkPreviousClasspathSnapshot.size} classes"
        }

        return reporter.measure(BuildTime.COMPUTE_CHANGED_AND_IMPACTED_SET) {
            computeChangedAndImpactedSet(shrunkCurrentClasspathAgainstPreviousLookups, shrunkPreviousClasspathSnapshot, reporter)
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
        reporter: ClasspathSnapshotBuildReporter
    ): ProgramSymbolSet {
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

        val changedSet = reporter.measure(BuildTime.COMPUTE_CLASS_CHANGES) {
            computeClassChanges(changedCurrentClasses, changedPreviousClasses, reporter)
        }
        reporter.reportVerboseWithLimit { "Changed set = ${changedSet.toDebugString()}" }

        if (changedSet.isEmpty()) {
            return changedSet
        }

        val changedAndImpactedSet = reporter.measure(BuildTime.COMPUTE_IMPACTED_SET) {
            // Note that changes may contain added symbols (they can also impact recompilation -- see examples in JavaClassChangesComputer).
            // So ideally, the result should be:
            //     computeImpactedSetInclusive(changes = changesOnPreviousClasspath, allClasses = classesOnPreviousClasspath) +
            //         computeImpactedSetInclusive(changes = changesOnCurrentClasspath, allClasses = classesOnCurrentClasspath)
            // However, here we only have the combined changes on both the previous and current classpath, and because it's okay to
            // over-approximate the result, we will modify the above computation into:
            //     computeImpactedSetInclusive(changes, allClasses = classesOnPreviousClasspath + classesOnCurrentClasspath)
            // Note: `allClasses` may contain overlapping ClassIds, but it won't be an issue. Also, we will replace
            // classesOnCurrentClasspath with changedClassesOnCurrentClasspath to avoid listing unchanged classes twice.
            computeImpactedSetInclusive(
                changes = changedSet,
                allClasses = (previousClassSnapshots.asSequence() + changedCurrentClasses.asSequence()).asIterable()
            )
        }
        reporter.reportVerboseWithLimit {
            "Impacted classes = " +
                    (changedAndImpactedSet.run { classes + classMembers.keys } - changedSet.run { classes + classMembers.keys })
        }

        return changedAndImpactedSet
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
    ): ProgramSymbolSet {
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
    ): ProgramSymbolSet {
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
    ): ProgramSymbolSet {
        // Note: We have removed unchanged classes earlier in computeChangedAndImpactedSet method, so here we only have changed classes.
        return ProgramSymbolSet.Collector().run {
            (currentClassSnapshots + previousClassSnapshots).forEach {
                when (it) {
                    is RegularKotlinClassSnapshot -> addClass(it.classId)
                    is PackageFacadeKotlinClassSnapshot -> addPackageMembers(it.classId.packageFqName, it.packageMemberNames)
                    is MultifileClassKotlinClassSnapshot -> addPackageMembers(it.classId.packageFqName, it.constantNames)
                }
            }
            getResult()
        }
    }

    private fun computeFineGrainedKotlinClassChanges(
        currentClassSnapshots: List<KotlinClassSnapshot>,
        previousClassSnapshots: List<KotlinClassSnapshot>
    ): ProgramSymbolSet {
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
        val dirtyData = changesCollector.getDirtyData(listOf(incrementalJvmCache), DoNothingICReporter)
        workingDir.deleteRecursively()

        return dirtyData.toProgramSymbols(currentClassSnapshots, previousClassSnapshots)
    }

    private fun DirtyData.toProgramSymbols(
        currentClassSnapshots: List<AccessibleClassSnapshot>,
        previousClassSnapshots: List<AccessibleClassSnapshot>
    ): ProgramSymbolSet {
        // Note that dirtyLookupSymbols may contain added symbols (they can also impact recompilation -- see examples in
        // JavaClassChangesComputer). Therefore, we need to consider classes on both the previous and current classpath. The combined list
        // may contain overlapping ClassIds, but it won't be an issue.
        val allClasses = (previousClassSnapshots.asSequence() + currentClassSnapshots.asSequence()).asIterable()
        return dirtyLookupSymbols.toProgramSymbolSet(allClasses).also {
            checkDirtyDataNormalization(this, it)
        }
    }

    /**
     * Checks whether [DirtyData.toProgramSymbols] completed without any inconsistencies.
     *
     * Specifically, [DirtyData] consists of:
     *   - dirtyLookupSymbols (Collection<LookupSymbol)
     *   - dirtyClassesFqNames (Collection<FqName>)
     *   - dirtyClassesFqNamesForceRecompile (Collection<FqName>)
     *
     * In [DirtyData.toProgramSymbols], we converted only dirtyLookupSymbols to [ProgramSymbol]s as dirtyLookupSymbols should contain all
     * the changes.
     *
     * In the following, we'll check that:
     *   1. There are no items in dirtyLookupSymbols that have not yet been converted to [ProgramSymbol]s.
     *   2. dirtyClassesFqNames and dirtyClassesFqNamesForceRecompile do not contain new information that can't be derived from
     *      dirtyLookupSymbols.
     */
    private fun checkDirtyDataNormalization(dirtyData: DirtyData, programSymbols: ProgramSymbolSet) {
        val changes = programSymbols.toChangesEither()

        val unmatchedLookupSymbols = dirtyData.dirtyLookupSymbols.toMutableSet().also {
            it.removeAll(changes.lookupSymbols.toSet())
        }
        val unmatchedFqNames = (dirtyData.dirtyClassesFqNames).toMutableSet().also {
            it.removeAll(changes.fqNames.toSet())
        }

        // Some LookupSymbols (reported by IncrementalJvmCache) are invalid. Examples:
        //   - LookupSymbol(name=<SAM-CONSTRUCTOR>, scope=com.example) (detected by
        //     KotlinOnlyClasspathChangesComputerTest.testTopLevelMembers): SAM-CONSTRUCTOR should have a class scope not a package scope.
        //   - LookupSymbol(name=BarUseABKt, scope=bar) (detected by
        //     IncrementalCompilationClasspathSnapshotJvmMultiProjectIT.testMoveFunctionFromLibToApp): The name of a LookupSymbol should not
        //     end with "Kt" (unless there is a Kotlin class (CLASS kind) whose name ends with "Kt", which is almost never the case).
        // Ignore these for now.
        // TODO: Fix them later
        if (unmatchedLookupSymbols.isNotEmpty()) {
            unmatchedLookupSymbols.removeAll { it.name == SAM_LOOKUP_NAME.asString() || it.name.endsWith("Kt") }
        }
        if (unmatchedFqNames.isNotEmpty()) {
            unmatchedFqNames.removeAll { it.asString().endsWith("Kt") }
        }

        check(unmatchedLookupSymbols.isEmpty()) {
            "The following LookupSymbols are not yet converted to ProgramSymbols: ${unmatchedLookupSymbols.joinToString(", ")}"
        }
        check(unmatchedFqNames.isEmpty()) {
            "The following FqNames are not found in DirtyData.dirtyLookupSymbols: ${unmatchedFqNames.joinToString(", ")}.\n" +
                    "DirtyData = $dirtyData"
        }
    }
}

internal object ImpactAnalysis {

    /**
     * Computes the set of [ProgramSymbol]s that are impacted by the given changes.
     *
     * For example, if a superclass has changed, any of its subclasses will be impacted even if it has not changed because unchanged source
     * files in the previous compilation that depended on the subclasses will need to be recompiled.
     *
     * The returned set includes the given changes plus the impacted ones.
     */
    fun computeImpactedSetInclusive(changes: ProgramSymbolSet, allClasses: Iterable<AccessibleClassSnapshot>): ProgramSymbolSet {
        val classIdToSubclasses = getClassIdToSubclassesMap(allClasses)
        val impactedClassesResolver = { classId: ClassId -> classIdToSubclasses[classId] ?: emptySet() }

        return ProgramSymbolSet.Collector().run {
            addClasses(findImpactedClassesInclusive(changes.classes, impactedClassesResolver))

            for ((classId, memberNames) in changes.classMembers) {
                findImpactedClassesInclusive(setOf(classId), impactedClassesResolver).forEach { impactedClassId ->
                    addClassMembers(impactedClassId, memberNames)
                }
            }

            for ((packageFqName, memberNames) in changes.packageMembers) {
                addPackageMembers(packageFqName, memberNames)
            }

            getResult()
        }
    }

    private fun getClassIdToSubclassesMap(allClasses: Iterable<AccessibleClassSnapshot>): Map<ClassId, Set<ClassId>> {
        val classIds: Set<ClassId> = allClasses.mapTo(mutableSetOf()) { it.classId } // Use Set for presence check
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val classIdToSubclasses = mutableMapOf<ClassId, MutableSet<ClassId>>()
        allClasses.forEach { classSnapshot ->
            // No need to collect supertypes outside the given set of classes (e.g., "java/lang/Object")
            classSnapshot.getSupertypes(classNameToClassIdResolver).intersect(classIds).forEach { supertype ->
                classIdToSubclasses.getOrPut(supertype) { mutableSetOf() }.add(classSnapshot.classId)
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
internal fun AccessibleClassSnapshot.getSupertypes(classIdResolver: (JvmClassName) -> ClassId?): Set<ClassId> {
    return when (this) {
        is RegularKotlinClassSnapshot -> supertypes.mapNotNullTo(mutableSetOf()) { classIdResolver.invoke(it) }
        is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> {
            // These classes may have supertypes (e.g., kotlin/collections/ArraysKt (MULTIFILE_CLASS) extends
            // kotlin/collections/ArraysKt___ArraysKt (MULTIFILE_CLASS_PART)), but we don't have to use that info during impact analysis
            // because those inheritors and supertypes should have the same package names, and in package facades only the package names and
            // member names matter.
            emptySet()
        }
        is JavaClassSnapshot -> supertypes.mapNotNullTo(mutableSetOf()) { classIdResolver.invoke(it) }
    }
}
