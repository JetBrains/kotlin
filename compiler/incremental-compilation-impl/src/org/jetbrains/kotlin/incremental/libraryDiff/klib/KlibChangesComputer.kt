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
import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForClass
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade
import org.jetbrains.kotlin.incremental.DifferenceCalculatorForPackageFacade.Companion.getNonPrivateMembers
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClassMembers
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ProgramSymbolSet
import org.jetbrains.kotlin.incremental.snapshots.LazySnapshotLoadingMetrics
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import kotlin.math.max

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
     * NOTE: Duplicate ids are not handled yet (see KT-90559 progress doc).
     */
    fun computeChangedAndImpactedSet(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
        reporter: ClasspathSnapshotBuildReporter,
    ): ProgramSymbolSet {
        val current: Map<String, KlibSnapshot> = currentSnapshots.associateBy { it.id }
        val previous: Map<String, KlibSnapshot> = previousSnapshots.associateBy { it.id }

        val changedCurrentSnapshots = current.values.filter { it.contentHash != previous[it.id]?.contentHash }
        val changedPreviousSnapshots = previous.values.filter { it.contentHash != current[it.id]?.contentHash }

        val changedSet = reporter.measure(COMPUTE_CLASS_CHANGES) {
            computePackageChanges(changedCurrentSnapshots, changedPreviousSnapshots)
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
     *
     * Snapshots without member-level data, or whose granularity changed between builds, are compared at the coarse level.
     */
    private fun computePackageChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet {
        val granularityChangedIds = findSnapshotsWithGranularityChange(currentSnapshots, previousSnapshots)

        val [coarseGrainedCurrent, fineGrainedCurrent] = currentSnapshots.partition {
            !it.hasMemberLevelData() || it.id in granularityChangedIds
        }
        val [coarseGrainedPrevious, fineGrainedPrevious] = previousSnapshots.partition {
            !it.hasMemberLevelData() || it.id in granularityChangedIds
        }

        return computeCoarseGrainedChanges(coarseGrainedCurrent, coarseGrainedPrevious) +
                computeFineGrainedChanges(fineGrainedCurrent, fineGrainedPrevious)
    }

    private fun findSnapshotsWithGranularityChange(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): Set<String> {
        val current = currentSnapshots.associateBy { it.id }
        val previous = previousSnapshots.associateBy { it.id }

        return (current.keys intersect previous.keys).filterTo(mutableSetOf()) { id ->
            current.getValue(id).hasMemberLevelData() != previous.getValue(id).hasMemberLevelData()
        }
    }

    private fun KlibSnapshot.hasMemberLevelData(): Boolean = when (this) {
        is PackageFragmentSnapshot -> packageFragmentProto != null
    }

    private fun computeCoarseGrainedChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet {
        // Note: We have removed unchanged snapshots earlier in computeChangedAndImpactedSet method, so here we only have changed ones.
        return ProgramSymbolSet.Collector().run {
            (currentSnapshots + previousSnapshots).forEach {
                when (it) {
                    is PackageFragmentSnapshot -> {
                        addClasses(it.classInfos.map { classInfo -> classInfo.classId })
                        addPackageMembers(it.packageName, it.packageMemberNames)
                    }
                }
            }
            getResult()
        }
    }

    private fun computeFineGrainedChanges(
        currentSnapshots: List<KlibSnapshot>,
        previousSnapshots: List<KlibSnapshot>,
    ): ProgramSymbolSet {
        val previousProtoDataMap = previousSnapshots.associate { it.id to createProtoData(it) }
        val currentProtoDataMap = currentSnapshots.associate { it.id to createProtoData(it) }

        return ProgramSymbolSet.Collector().run {
            for (id in previousProtoDataMap.keys + currentProtoDataMap.keys) {
                val previousProtoData = previousProtoDataMap[id].orEmpty()
                val currentProtoData = currentProtoDataMap[id].orEmpty()

                collectClassProtoChanges(
                    previousProtoData.filterIsInstance<ClassProtoData>(),
                    currentProtoData.filterIsInstance<ClassProtoData>(),
                )
                collectPackagePartProtoChanges(
                    previousProtoData.filterIsInstance<PackagePartProtoData>(),
                    currentProtoData.filterIsInstance<PackagePartProtoData>(),
                )
            }

            getResult()
        }
    }

    private fun createProtoData(snapshot: KlibSnapshot): List<ProtoData> {
        val result = mutableListOf<ProtoData>()

        when (snapshot) {
            is PackageFragmentSnapshot -> {
                val accessibleClassIds = snapshot.classInfos.mapTo(HashSet(), KlibClassInfo::classId)

                for (proto in requireNotNull(snapshot.packageFragmentProto)) {
                    val fragment = parsePackageFragment(proto)
                    val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)

                    for (classProto in fragment.class_List) {
                        val classId = nameResolver.getClassId(classProto.fqName)
                        if (classId in accessibleClassIds) {
                            result.add(ClassProtoData(classProto, nameResolver))
                        }
                    }

                    result.add(PackagePartProtoData(fragment.`package`, nameResolver, snapshot.packageName))
                }
            }
        }

        return result
    }

    private fun ProgramSymbolSet.Collector.collectClassProtoChanges(
        previousClassProtoData: List<ClassProtoData>,
        currentClassProtoData: List<ClassProtoData>,
    ) {
        val previousClassProtoDataMap =
            previousClassProtoData.associateBy { (proto, nameResolver) -> nameResolver.getClassId(proto.fqName) }
        val currentClassProtoDataMap =
            currentClassProtoData.associateBy { (proto, nameResolver) -> nameResolver.getClassId(proto.fqName) }

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

    private fun ProgramSymbolSet.Collector.collectPackagePartProtoChanges(
        previousPackagePartProtoData: List<PackagePartProtoData>,
        currentPackagePartProtoData: List<PackagePartProtoData>,
    ) {
        for (i in 0 until max(previousPackagePartProtoData.size, currentPackagePartProtoData.size)) {
            val previousPackagePartProto = previousPackagePartProtoData.getOrNull(i)
            val currentPackagePartProto = currentPackagePartProtoData.getOrNull(i)

            when {
                previousPackagePartProto == null ->
                    addPackageMembers(currentPackagePartProto!!.packageFqName, currentPackagePartProto.getNonPrivateMembers())
                currentPackagePartProto == null ->
                    addPackageMembers(previousPackagePartProto.packageFqName, previousPackagePartProto.getNonPrivateMembers())
                else -> {
                    val difference = DifferenceCalculatorForPackageFacade(previousPackagePartProto, currentPackagePartProto).difference()
                    addPackageMembers(previousPackagePartProto.packageFqName, difference.changedMembersNames)
                }
            }
        }
    }

    /**
     * Computes the set of symbols that are *transitively* impacted by [changes], e.g. subclasses of a changed class.
     *
     * The returned set is *inclusive* (it contains [changes] + the impacted symbols).
     */
    private fun computeImpactedSymbols(changes: ProgramSymbolSet, allSnapshots: Iterable<KlibSnapshot>): ProgramSymbolSet {
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
