/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_CHANGED_AND_IMPACTED_SET
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_CLASS_CHANGES
import org.jetbrains.kotlin.build.report.metrics.COMPUTE_IMPACTED_SET
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClassMembers
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.getClassId

/** Computes changes between two klib snapshots. */
internal object KlibChangesComputer {

    /**
     * Computes changes between the current and previous klib snapshots, plus unchanged elements that are impacted by the changes.
     *
     * Like on the JVM, the current snapshot is first shrunk against the lookups of the previous build, then compared with the shrunk
     * snapshot saved by the previous build.
     */
    fun computeKlibChanges(
        lookupStorage: LookupStorage,
        lazyKlibSnapshot: LazyKlibSnapshot,
        reporter: ClasspathSnapshotBuildReporter,
    ): ProgramSymbolSet {
        val currentSnapshots = lazyKlibSnapshot.getCurrentKlibSnapshot(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        val shrunkCurrentSnapshots = lazyKlibSnapshot
            .getComputedShrunkKlibSnapshotAgainstPreviousLookups(lookupStorage, LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        reporter.debug {
            "Shrunk current klib snapshot for diffing, retained ${shrunkCurrentSnapshots.size} / ${currentSnapshots.size} snapshots"
        }

        val shrunkPreviousSnapshots = lazyKlibSnapshot
            .getSavedShrunkKlibSnapshotAgainstPreviousLookups(LazySnapshotLoadingMetrics.OnClasspathDiffComputation)
        reporter.debug { "Loaded shrunk previous klib snapshot for diffing, found ${shrunkPreviousSnapshots.size} snapshots" }

        return reporter.measure(COMPUTE_CHANGED_AND_IMPACTED_SET) {
            computeChangedAndImpactedSet(shrunkCurrentSnapshots, shrunkPreviousSnapshots, reporter)
        }
    }

    /**
     * Computes changes between the current and previous lists of snapshots, plus unchanged elements that are impacted by the changes.
     *
     * Snapshots are matched by content hash, and each snapshot can be matched to only one snapshot of the other build. The content hash
     * is enough because a snapshot's package is derived from its content.
     */
    fun computeChangedAndImpactedSet(
        currentSnapshots: List<KlibFileSnapshot>,
        previousSnapshots: List<KlibFileSnapshot>,
        reporter: ClasspathSnapshotBuildReporter,
    ): ProgramSymbolSet {
        val currentSnapshotsByHash = currentSnapshots.groupBy { it.contentHash }
        val previousSnapshotsByHash = previousSnapshots.groupBy { it.contentHash }

        val changedCurrentSnapshots = currentSnapshotsByHash.flatMap { [hash, snapshots] ->
            snapshots.drop(previousSnapshotsByHash[hash]?.size ?: 0)
        }
        val changedPreviousSnapshots = previousSnapshotsByHash.flatMap { [hash, snapshots] ->
            snapshots.drop(currentSnapshotsByHash[hash]?.size ?: 0)
        }

        val changedSet = reporter.measure(COMPUTE_CLASS_CHANGES) {
            computeKlibFileChanges(changedCurrentSnapshots, changedPreviousSnapshots)
        }
        reporter.reportVerboseWithLimit { "Changed set = ${changedSet.toDebugString()}" }

        if (changedSet.isEmpty()) {
            return changedSet
        }

        val changedAndImpactedSet = reporter.measure(COMPUTE_IMPACTED_SET) {
            // Over-approximate like on the JVM: compute the impact over the previous snapshots plus the changed current ones
            // (see `ClasspathChangesComputer.computeChangedAndImpactedSet`).
            computeImpactedSymbols(
                changes = changedSet,
                allSnapshots = (previousSnapshots.asSequence() + changedCurrentSnapshots.asSequence()).asIterable(),
            )
        }
        reporter.reportVerboseWithLimit {
            "Impacted classes = " +
                    (changedAndImpactedSet.run { classes + classMembers.keys } - changedSet.run { classes + classMembers.keys })
        }

        return changedAndImpactedSet
    }

    /**
     * Computes changes between the current and previous lists of changed snapshots. The result does not need to include elements that
     * are impacted by the changes.
     */
    private fun computeKlibFileChanges(
        currentSnapshots: List<KlibFileSnapshot>,
        previousSnapshots: List<KlibFileSnapshot>,
    ): ProgramSymbolSet {
        val granularityChangedHashes = findPackageFragmentWithGranularityChanges(currentSnapshots, previousSnapshots)

        val [coarseGrainedCurrent, fineGrainedCurrent] = currentSnapshots.partition {
            it.classMemberLevelSnapshot == null || it.contentHash in granularityChangedHashes
        }
        val [coarseGrainedPrevious, fineGrainedPrevious] = previousSnapshots.partition {
            it.classMemberLevelSnapshot == null || it.contentHash in granularityChangedHashes
        }

        return computeCoarseGrainedPackageFragmentChanges(coarseGrainedPrevious, coarseGrainedCurrent) +
                computeFineGrainedPackageFragmentChanges(fineGrainedPrevious, fineGrainedCurrent)
    }

    /** Fine-grained changes are computed per package, so if a package's granularity changes, all its snapshots are coarse-grained. */
    private fun findPackageFragmentWithGranularityChanges(
        currentSnapshots: List<KlibFileSnapshot>,
        previousSnapshots: List<KlibFileSnapshot>,
    ): Set<Long> {
        val current = currentSnapshots.groupBy { it.packageName }
        val previous = previousSnapshots.groupBy { it.packageName }

        return buildSet {
            for (packageName in current.keys intersect previous.keys) {
                if (current.getValue(packageName).any { it.classMemberLevelSnapshot == null } !=
                    previous.getValue(packageName).any { it.classMemberLevelSnapshot == null }
                ) {
                    current.getValue(packageName).mapTo(this) { it.contentHash }
                    previous.getValue(packageName).mapTo(this) { it.contentHash }
                }
            }
        }
    }

    private fun computeCoarseGrainedPackageFragmentChanges(
        previousSnapshots: List<KlibFileSnapshot>,
        currentSnapshots: List<KlibFileSnapshot>,
    ): ProgramSymbolSet {
        // Note: We have removed unchanged snapshots earlier in computeChangedAndImpactedSet method, so here we only have changed ones.
        return ProgramSymbolSet.Collector().run {
            (currentSnapshots + previousSnapshots).forEach {
                addClasses(it.classInfos.map { classInfo -> classInfo.classId })
                addPackageMembers(it.packageName, it.packageMemberNames)
            }
            getResult()
        }
    }

    private fun computeFineGrainedPackageFragmentChanges(
        previousSnapshots: List<KlibFileSnapshot>,
        currentSnapshots: List<KlibFileSnapshot>,
    ): ProgramSymbolSet {
        fun List<KlibFileSnapshot>.groupProtoDataByPackage(): Map<FqName, List<ProtoData>> =
            groupBy({ it.packageName }, { requireNotNull(it.classMemberLevelSnapshot).proto }).mapValues { it.value.flatten() }

        val previousProtoDataMap = previousSnapshots.groupProtoDataByPackage()
        val currentProtoDataMap = currentSnapshots.groupProtoDataByPackage()

        return ProgramSymbolSet.Collector().run {
            for (packageName in previousProtoDataMap.keys + currentProtoDataMap.keys) {
                val previousProtoData = previousProtoDataMap[packageName].orEmpty()
                val currentProtoData = currentProtoDataMap[packageName].orEmpty()

                collectClassProtoChanges(
                    previousProtoData.filterIsInstance<ClassProtoData>(),
                    currentProtoData.filterIsInstance<ClassProtoData>(),
                )

                collectPackagePartProtoChanges(
                    packageName,
                    previousProtoData.filterIsInstance<PackagePartProtoData>(),
                    currentProtoData.filterIsInstance<PackagePartProtoData>(),
                )
            }

            getResult()
        }
    }

    private fun ProgramSymbolSet.Collector.collectClassProtoChanges(
        previousClassProtoData: List<ClassProtoData>,
        currentClassProtoData: List<ClassProtoData>,
    ) {
        val previousClassProtoDataMap = previousClassProtoData.associateFirstByClassId()
        val currentClassProtoDataMap = currentClassProtoData.associateFirstByClassId()

        for (classId in previousClassProtoDataMap.keys + currentClassProtoDataMap.keys) {
            val previousClassProto = previousClassProtoDataMap[classId]
            val currentClassProto = currentClassProtoDataMap[classId]

            if (previousClassProto == null || currentClassProto == null) {
                addClass(classId)
                continue
            }

            DifferenceCalculatorForClass(previousClassProto, currentClassProto).difference().let {
                if (it.isClassAffected) {
                    addClass(classId)
                }

                addClassMembers(classId, it.changedMembersNames)
            }
        }
    }

    /**
     * If several klibs declare the same class, the compiler uses the copy from the first klib, so only changes to that copy matter.
     * This relies on the snapshots being in classpath order.
     */
    private fun List<ClassProtoData>.associateFirstByClassId(): Map<ClassId, ClassProtoData> =
        buildMap {
            for (classProtoData in this@associateFirstByClassId) {
                putIfAbsent(classProtoData.nameResolver.getClassId(classProtoData.proto.fqName), classProtoData)
            }
        }

    private fun ProgramSymbolSet.Collector.collectPackagePartProtoChanges(
        packageName: FqName,
        previousPackagePartProtoData: List<PackagePartProtoData>,
        currentPackagePartProtoData: List<PackagePartProtoData>,
    ) {
        val difference = DifferenceCalculatorForPackageFacades(previousPackagePartProtoData, currentPackagePartProtoData).difference()
        addPackageMembers(packageName, difference.changedMembersNames)
    }

    /**
     * Computes the set of symbols that are *transitively* impacted by [changes], e.g. subclasses of a changed class.
     *
     * The returned set is *inclusive* (it contains [changes] + the impacted symbols).
     */
    private fun computeImpactedSymbols(changes: ProgramSymbolSet, allSnapshots: Iterable<KlibFileSnapshot>): ProgramSymbolSet {
        val impactedSymbolsResolver = KlibAllImpacts.getResolver(allSnapshots)

        return ProgramSymbolSet.Collector().apply {
            addClasses(findReachableNodes(changes.classes, impactedSymbolsResolver::getImpactedClasses))

            val classMembers = changes.classMembers.map { [classId, memberNames] -> ClassMembers(classId, memberNames) }
            findReachableNodes(classMembers, impactedSymbolsResolver::getImpactedClassMembers).forEach {
                addClassMembers(it.classId, it.memberNames)
            }

            // Changed package members do not impact other symbols. Unlike the JVM, klibs have no package facade classes, so
            // impacted classes never imply impacted package members.
            changes.packageMembers.forEach { [packageFqName, memberNames] ->
                addPackageMembers(packageFqName, memberNames)
            }
        }.getResult()
    }
}
