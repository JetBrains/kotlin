/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.BreadthFirstSearch.findReachableNodes
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.LookupSymbolSet
import org.jetbrains.kotlin.incremental.classpathDiff.toLookupSymbol
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.filterToSetOrEmpty

internal object KlibSnapshotShrinker {

    /** Retains only the snapshots referenced, directly or transitively, by the lookup symbols stored in [lookupStorage]. */
    fun shrink(allSnapshots: List<KlibSnapshot>, lookupStorage: LookupStorage): List<KlibSnapshot> =
        shrink(allSnapshots, lookupStorage.lookupSymbols)

    /**
     * Retains only the snapshots referenced by [lookupSymbols], directly or transitively (e.g. via supertypes), so that the impact
     * of changes can still be computed (see [KlibChangesComputer.computeChangedAndImpactedSet]).
     */
    fun shrink(allSnapshots: List<KlibSnapshot>, lookupSymbols: Collection<LookupSymbolKey>): List<KlibSnapshot> {
        val lookupSymbolSet =
            LookupSymbolSet(lookupSymbols.asSequence().map { LookupSymbol(name = it.name, scope = it.scope) }.asIterable())

        val transitivelyReferencedClassIds = findTransitivelyReferencedClassIds(
            allSnapshots,
            findReferencedClassIds(allSnapshots, lookupSymbolSet)
        )

        return allSnapshots.filter { snapshot ->
            val hasReferencedClass = snapshot.classIds().any { it in transitivelyReferencedClassIds }
            if (hasReferencedClass) return@filter true

            when (snapshot) {
                is PackageFragmentSnapshot -> {
                    val lookupNamesInPackage = lookupSymbolSet.getLookupNamesInScope(snapshot.packageName)
                    snapshot.packageMemberNames.any { it in lookupNamesInPackage }
                }
            }
        }
    }

    private fun findReferencedClassIds(allSnapshots: List<KlibSnapshot>, lookupSymbols: LookupSymbolSet): Set<ClassId> =
        allSnapshots
            .flatMap { it.classIds() }
            .filterToSetOrEmpty { classId ->
                ClassSymbol(classId).toLookupSymbol() in lookupSymbols
                        || lookupSymbols.getLookupNamesInScope(classId.asSingleFqName()).isNotEmpty()
            }

    /** The returned set is *inclusive* (it contains [referencedClassIds] + their supertypes, directly or transitively). */
    private fun findTransitivelyReferencedClassIds(allSnapshots: List<KlibSnapshot>, referencedClassIds: Set<ClassId>): Set<ClassId> {
        if (referencedClassIds.isEmpty()) return emptySet()

        val impactingClassesResolver = KlibAllImpacts.getReverseResolver(allSnapshots)
        return findReachableNodes(referencedClassIds, impactingClassesResolver::getImpactingClasses)
    }

    private fun KlibSnapshot.classIds(): List<ClassId> = when (this) {
        is PackageFragmentSnapshot -> classInfos.map { it.classId }
    }
}

/** Shrinks the current klib snapshot against the lookups of this build and saves it for the next build. */
internal fun shrinkAndSaveKlibSnapshot(
    classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled,
    lookupStorage: LookupStorage,
    lazyKlibSnapshot: LazyKlibSnapshot,
    reporter: ClasspathSnapshotBuildReporter,
) {
    TODO("KT-90559 step 1")
}
