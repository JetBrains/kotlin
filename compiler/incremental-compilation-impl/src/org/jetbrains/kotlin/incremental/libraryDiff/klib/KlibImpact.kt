/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.classpathDiff.ClassMembers
import org.jetbrains.kotlin.incremental.classpathDiff.ImpactedSymbolsResolver
import org.jetbrains.kotlin.incremental.classpathDiff.ImpactingClassesResolver
import org.jetbrains.kotlin.name.ClassId

/** Describes how a change of one symbol impacts other symbols. */
internal sealed interface KlibImpact {
    fun getResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactedSymbolsResolver
    fun getReverseResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactingClassesResolver
}

/**
 * A composite [KlibImpact] containing all concrete impacts. Currently:
 *   1. [KlibSupertypesInheritorsImpact]
 */
internal object KlibAllImpacts : KlibImpact {

    private val allImpacts: List<KlibImpact> = listOf(KlibSupertypesInheritorsImpact)

    override fun getResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactedSymbolsResolver {
        val resolvers = allImpacts.map { it.getResolver(allSnapshots) }

        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> =
                resolvers.flatMapTo(mutableSetOf()) { it.getImpactedClasses(classId) }

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> =
                resolvers.flatMapTo(mutableSetOf()) { it.getImpactedClassMembers(classMembers) }
        }
    }

    override fun getReverseResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactingClassesResolver {
        val reverseResolvers = allImpacts.map { it.getReverseResolver(allSnapshots) }

        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> =
                reverseResolvers.flatMapTo(mutableSetOf()) { it.getImpactingClasses(classId) }
        }
    }
}

/**
 * If a superclass/interface has changed, its subclasses/sub-interfaces are impacted. Supertypes come from
 * [KlibClassInfo.supertypes].
 */
private object KlibSupertypesInheritorsImpact : KlibImpact {

    override fun getResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactedSymbolsResolver {
        val classIdToSubclasses =
            getClassIdToSubclassesMap(allSnapshots.flatMap { it.classInfos })

        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> =
                classIdToSubclasses[classId].orEmpty()

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> =
                classIdToSubclasses[classMembers.classId].orEmpty()
                    .mapTo(mutableSetOf()) { subclass ->
                        ClassMembers(subclass, classMembers.memberNames)
                    }
        }
    }

    override fun getReverseResolver(allSnapshots: Iterable<KlibFileSnapshot>): ImpactingClassesResolver {
        val classIdToSupertypes =
            getClassIdToSupertypesMap(allSnapshots.flatMap { it.classInfos })

        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> =
                classIdToSupertypes[classId].orEmpty()
        }
    }

    private fun getClassIdToSubclassesMap(allClassInfos: Iterable<KlibClassInfo>): Map<ClassId, Set<ClassId>> {
        val classIdToSubclasses = mutableMapOf<ClassId, MutableSet<ClassId>>()

        getClassIdToSupertypesMap(allClassInfos).forEach { [classId, supertypes] ->
            supertypes.forEach { supertype ->
                classIdToSubclasses.getOrPut(supertype) { mutableSetOf() }.add(classId)
            }
        }

        return classIdToSubclasses
    }

    private fun getClassIdToSupertypesMap(allClassInfos: Iterable<KlibClassInfo>): Map<ClassId, Set<ClassId>> {
        val allClassIds = allClassInfos.mapTo(mutableSetOf()) { it.classId }

        return buildMap {
            allClassInfos.forEach { classInfo ->
                val supertypes = classInfo.supertypes.filterTo(mutableSetOf()) { it in allClassIds }
                if (supertypes.isNotEmpty()) {
                    put(classInfo.classId, supertypes)
                }
            }
        }
    }
}
