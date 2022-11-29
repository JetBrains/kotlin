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
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotShrinker.shrinkClasspath
import org.jetbrains.kotlin.incremental.classpathDiff.ImpactedSymbolsComputer.computeImpactedSymbols
import org.jetbrains.kotlin.incremental.storage.FileToAbsolutePathConverter
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.loadFromFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
            //     computeImpactedSymbols(changes = changesOnPreviousClasspath, allClasses = classesOnPreviousClasspath) +
            //         computeImpactedSymbols(changes = changesOnCurrentClasspath, allClasses = classesOnCurrentClasspath)
            // However, here we only have the combined changes on both the previous and current classpath, and because it's okay to
            // over-approximate the result, we will modify the above computation into:
            //     computeImpactedSet(
            //         changes = changesOnPreviousAndCurrentClasspath,
            //         allClasses = classesOnPreviousClasspath + classesOnCurrentClasspath)
            // Note: We will replace `classesOnCurrentClasspath` with `changedClassesOnCurrentClasspath` to avoid listing unchanged classes
            // twice. `allClasses` may contain overlapping ClassIds of modified classes, but it won't be an issue.
            computeImpactedSymbols(
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
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToAbsolutePathConverter)

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

        // Get the changes and clean up
        val dirtyData = changesCollector.getDirtyData(listOf(incrementalJvmCache), DoNothingICReporter)
        workingDir.deleteRecursively()

        // Normalize the changes (convert DirtyData to `ProgramSymbol`s)
        // Note:
        //   - DirtyData may contain added symbols (they can also impact recompilation -- see examples in JavaClassChangesComputer).
        //   Therefore, we need to consider classes on both the previous and current classpath when converting DirtyData.
        //   - We have removed unchanged classes earlier in computeChangedAndImpactedSet method, so here we only have changed classes.
        //   - `changedClasses` may contain overlapping ClassIds of modified classes, but it won't be an issue.
        val changedClasses = (previousClassSnapshots.asSequence() + currentClassSnapshots.asSequence()).asIterable()
        return dirtyData.toProgramSymbols(changedClasses)
    }

    /**
     * Converts this [DirtyData] to [ProgramSymbol]s.
     *
     * Specifically, [DirtyData] consists of:
     *   - dirtyLookupSymbols (Collection<LookupSymbol)
     *   - dirtyClassesFqNames (Collection<FqName>)
     *   - dirtyClassesFqNamesForceRecompile (Collection<FqName>)
     *
     * First, we will convert `dirtyLookupSymbols` to [ProgramSymbol]s as `dirtyLookupSymbols` should contain all the changes.
     *
     * Then, we will check that:
     *   1. There are no items in `dirtyLookupSymbols` that have not yet been converted to [ProgramSymbol]s.
     *   2. `dirtyClassesFqNames` and `dirtyClassesFqNamesForceRecompile` must not contain new information that can't be derived from
     *      `dirtyLookupSymbols`.
     */
    private fun DirtyData.toProgramSymbols(changedClasses: Iterable<AccessibleClassSnapshot>): ProgramSymbolSet {
        val changedProgramSymbols = dirtyLookupSymbols.toProgramSymbolSet(changedClasses)

        // Check whether there is any info in this DirtyData that has not yet been converted to `changedProgramSymbols`
        val (changedLookupSymbols, changedFqNames) = changedProgramSymbols.toChangesEither().let {
            it.lookupSymbols.toSet() to it.fqNames.toSet()
        }
        val unmatchedLookupSymbols = this.dirtyLookupSymbols.toMutableSet().also {
            it.removeAll(changedLookupSymbols)
        }
        val unmatchedFqNames = this.dirtyClassesFqNames.toMutableSet().also {
            it.addAll(this.dirtyClassesFqNamesForceRecompile)
            it.removeAll(changedFqNames)
        }

        /* When `unmatchedLookupSymbols` or `unmatchedFqNames` is not empty, there are two cases:
         *   1. The unmatched LookupSymbols/FqNames are redundant. This is not ideal but because it does not cause incremental compilation
         *      to be incorrect, we can fix these issues later if they are not easy to fix immediately.
         *   2. The unmatched LookupSymbols/FqNames are valid changes. Since they are required for incremental compilation to be correct, we
         *      must fix these issues immediately.
         * In the following, we'll list the known issues for case 1 (and it must be case 1 only).
         * TODO: We'll fix these issues later.
         */

        // Known issue 1: DirtyData reported by IncrementalJvmCache may include both a class and class member (e.g.,
        // LookupSymbol("com.example", "A") and LookupSymbol("com.example.A", "someProperty")). When the class LookupSymbol is present, the
        // class member LookupSymbol is redundant. When converting DirtyData to ProgramSymbols, we remove redundant class member
        // `ProgramSymbol`s, so here we will find that LookupSymbol("com.example.A", "someProperty") is not yet matched. Ignore these
        // `LookupSymbol`s for now.
        val classesFqNames = changedProgramSymbols.classes.mapTo(mutableSetOf()) { it.asSingleFqName() }
        unmatchedLookupSymbols.removeAll { FqName(it.scope) in classesFqNames }

        // Known issue 2: If class A has a companion object containing a constant `CONSTANT`, and if the value of `CONSTANT` has changed,
        // then only `A.class` will change, not `A.Companion.class` (see `ConstantsInCompanionObjectImpact`). Since we distinguish between
        // changed symbols and impacted symbols, we should detect that:
        //    - A.CONSTANT has changed
        //    - A.Companion.CONSTANT is unchanged but impacted (this detection happens after the step here)
        //
        // However, currently IncrementalJvmCache will report that both `A.CONSTANT` and `A.Companion.CONSTANT` have changed (see
        // `IncrementalJvmCache.ConstantsMap.process`) as it needs to work with both the old IC and the new IC (in the old IC, changed
        // symbols and impacted symbols are not clearly separated).
        //
        // With the new IC, when converting DirtyData to ProgramSymbols (this method), because we consider only changed classes and
        // `A.Companion.class` is unchanged, we will not convert `A.Companion.CONSTANT`. Therefore, `A.Companion.CONSTANT` is unmatched,
        // and we'll need to ignore it here.
        //
        // Note: Once we are able to remove this workaround, we can remove RegularKotlinClassSnapshot.companionObjectName as this is the only
        // usage of that property.
        val companionObjectFqNames = changedClasses.mapNotNullTo(mutableSetOf()) { clazz ->
            (clazz as? RegularKotlinClassSnapshot)?.companionObjectName?.let { it ->
                clazz.classId.createNestedClassId(Name.identifier(it)).asSingleFqName()
            }
        }
        unmatchedLookupSymbols.removeAll { FqName(it.scope) in companionObjectFqNames }
        unmatchedFqNames.removeAll(companionObjectFqNames)

        // Known issue 3: LookupSymbol(name=<SAM-CONSTRUCTOR>, scope=com.example) reported by IncrementalJvmCache is invalid (detected by
        // KotlinOnlyClasspathChangesComputerTest.testTopLevelMembers): SAM-CONSTRUCTOR should have a class scope, not a package scope.
        unmatchedLookupSymbols.removeAll { it.name == SAM_LOOKUP_NAME.asString() && FqName(it.scope) !in classesFqNames }

        // Known issue 4: LookupSymbol(name=BarUseABKt, scope=bar) reported by IncrementalJvmCache is invalid (detected by
        // IncrementalCompilationClasspathSnapshotJvmMultiProjectIT.testMoveFunctionFromLibToApp): The name of a LookupSymbol should not
        // end with "Kt" (unless there is a Kotlin class (CLASS kind) whose name ends with "Kt", which is almost never the case).
        unmatchedLookupSymbols.removeAll { it.name.endsWith("Kt") }
        unmatchedFqNames.removeAll { it.asString().endsWith("Kt") }

        /*
         * End of known issues, throw an Exception.
         */
        check(unmatchedLookupSymbols.isEmpty()) {
            "The following LookupSymbols are not yet converted to ProgramSymbols: ${unmatchedLookupSymbols.joinToString(", ")}"
        }
        check(unmatchedFqNames.isEmpty()) {
            "The following FqNames can't be derived from DirtyData.dirtyLookupSymbols: ${unmatchedFqNames.joinToString(", ")}.\n" +
                    "DirtyData = $this"
        }

        return changedProgramSymbols
    }
}

private object ImpactedSymbolsComputer {

    /**
     * Computes the set of [ProgramSymbol]s that are *transitively* impacted by the given set of [ProgramSymbol]s. For example, if a
     * superclass has changed/been impacted, its subclasses will be impacted.
     *
     * The returned set is *inclusive* (it contains the given set + the directly/transitively impacted ones).
     */
    fun computeImpactedSymbols(changes: ProgramSymbolSet, allClasses: Iterable<AccessibleClassSnapshot>): ProgramSymbolSet {
        val impactedSymbolsResolver = AllImpacts.getResolver(allClasses)
        return ProgramSymbolSet.Collector().apply {
            // Add impacted classes
            val impactedClasses = findReachableNodes(changes.classes, impactedSymbolsResolver::getImpactedClasses)
            addClasses(impactedClasses)

            // Add impacted class members
            val classMembers = changes.classMembers.map { ClassMembers(it.key, it.value) }
            val impactedClassMembers = findReachableNodes(classMembers, impactedSymbolsResolver::getImpactedClassMembers)
            impactedClassMembers.forEach {
                addClassMembers(it.classId, it.memberNames)
            }

            // Package members are currently not impacted, so we just copy the original set over
            changes.packageMembers.forEach { (packageFqName, memberNames) ->
                addPackageMembers(packageFqName, memberNames)
            }
        }.getResult()
    }

}
