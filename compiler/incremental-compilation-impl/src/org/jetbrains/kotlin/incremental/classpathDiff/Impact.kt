/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*

/**
 * A common interface for all types of impact among classes. For example, if class B extends class A, then class A impacts class B, because
 * if class A has changed, a source file that references class B will need to be recompiled (even though class B has not changed).
 */
internal sealed interface Impact {

    /** Provides an [ImpactedSymbolsResolver] to compute the set of [ProgramSymbol]s impacted by a given set of [ProgramSymbol]s. */
    fun getResolver(allClasses: Iterable<AccessibleClassSnapshot>): ImpactedSymbolsResolver

    /**
     * Provides an [ImpactingClassesResolver] to compute the set of classes impacting a given set of classes (the reverse of [getResolver]).
     *
     * The resolver computes impacting classes on demand: it is used when shrinking classpath snapshots, where only the few classes that
     * are reachable from the referenced classes are visited, so precomputing the impact graph of the entire classpath would dominate the
     * shrinking time.
     */
    fun getReverseResolver(allClasses: ClassSnapshotIndex): ImpactingClassesResolver
}

/**
 * Index of [AccessibleClassSnapshot]s by [ClassId] and by [JvmClassName].
 *
 * Classes are only grouped by package name upfront (an already computed string); the per-package lookup tables are built when a package is
 * first queried. When shrinking classpath snapshots only a few packages are queried, so this stays cheap even for a classpath with many
 * thousands of classes.
 */
internal class ClassSnapshotIndex(allClasses: Iterable<AccessibleClassSnapshot>) {

    private class PackageIndex(classes: List<AccessibleClassSnapshot>) {
        /** Note: a [ClassId] should be unique on a deduplicated classpath, but keep all classes if it isn't. */
        val classesByClassId = HashMap<ClassId, MutableList<AccessibleClassSnapshot>>()

        /** Relative class name with `$` separators (see [JvmClassName.internalNameByClassId]) -> [ClassId]. */
        val classIdsByRelativeInternalName = HashMap<String, ClassId>()

        init {
            for (clazz in classes) {
                classesByClassId.getOrPut(clazz.classId) { ArrayList(1) }.add(clazz)
                classIdsByRelativeInternalName[clazz.classId.relativeClassName.asString().replace('.', '$')] = clazz.classId
            }
        }
    }

    private val classesByPackageName = HashMap<String, MutableList<AccessibleClassSnapshot>>()
    private val packageIndices = HashMap<String, PackageIndex>()

    init {
        for (clazz in allClasses) {
            classesByPackageName.getOrPut(clazz.classId.packageFqName.asString()) { ArrayList() }.add(clazz)
        }
    }

    private fun getPackageIndex(packageName: String): PackageIndex? {
        val classes = classesByPackageName[packageName] ?: return null
        return packageIndices.getOrPut(packageName) { PackageIndex(classes) }
    }

    operator fun get(classId: ClassId): List<AccessibleClassSnapshot> =
        getPackageIndex(classId.packageFqName.asString())?.classesByClassId?.get(classId).orEmpty()

    /** Returns the [ClassId] of the class with the given [JvmClassName] if it is in this index, or null otherwise. */
    fun getClassId(className: JvmClassName): ClassId? {
        // A package name can't contain '/', so the last '/' separates the package from the relative class name
        val internalName = className.internalName
        val lastSlash = internalName.lastIndexOf('/')
        val packageName = if (lastSlash < 0) "" else internalName.substring(0, lastSlash).replace('/', '.')
        return getPackageIndex(packageName)?.classIdsByRelativeInternalName?.get(internalName.substring(lastSlash + 1))
    }
}

/**
 * Computes the set of [ProgramSymbol]s that are *directly* impacted by a given set of [ProgramSymbol]s.
 *
 * The returned set is *inclusive* (it contains the given set + the directly impacted ones).
 *
 * This is typically used when computing classpath changes: If class A has changed, and it impacts class B, then a source file that
 * references class B will need to be recompiled (even though class B has not changed).
 */
internal interface ImpactedSymbolsResolver {
    fun getImpactedClasses(classId: ClassId): Set<ClassId>
    fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers>
}

/**
 * Computes the set of classes *directly* impacting a given set of classes.
 *
 * The returned set is *inclusive* (it contains the given set + the directly impacting ones).
 *
 * This is typically used when shrinking classpath snapshots: If class A impacts class B, and class B is referenced by a source file, then
 * class A will need to be retained in the shrunk classpath snapshot because the classpath changes computation will need to see class A
 * (see [ImpactedSymbolsResolver]).
 */
internal interface ImpactingClassesResolver {
    fun getImpactingClasses(classId: ClassId): Set<ClassId>
}

/**
 * A composite [Impact] containing all possible concrete impacts. Currently, the types of impact include:
 *   1. [SupertypesInheritorsImpact]
 *   2. [ConstantsInCompanionObjectsImpact]
 *   3. [TypeAliasExpansionImpact]
 */
internal object AllImpacts : Impact {

    private val allImpacts = listOf(SupertypesInheritorsImpact, ConstantsInCompanionObjectsImpact, TypeAliasExpansionImpact)

    override fun getResolver(allClasses: Iterable<AccessibleClassSnapshot>): ImpactedSymbolsResolver {
        val resolvers = allImpacts.map { it.getResolver(allClasses) }
        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> {
                return resolvers.flatMapTo(mutableSetOf()) { it.getImpactedClasses(classId) }
            }

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> {
                return resolvers.flatMapTo(mutableSetOf()) { it.getImpactedClassMembers(classMembers) }
            }
        }
    }

    override fun getReverseResolver(allClasses: ClassSnapshotIndex): ImpactingClassesResolver {
        val reverseResolvers = allImpacts.map { it.getReverseResolver(allClasses) }
        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> {
                return reverseResolvers.flatMapTo(mutableSetOf()) { it.getImpactingClasses(classId) }
            }
        }
    }
}

/**
 * Describes the impact between supertypes and inheritors: If a superclass/interface has changed, its subclasses/sub-interfaces will be
 * impacted.
 */
private object SupertypesInheritorsImpact : Impact {

    override fun getResolver(allClasses: Iterable<AccessibleClassSnapshot>): ImpactedSymbolsResolver {
        val classIdToSubclasses: Map<ClassId, Set<ClassId>> = getClassIdToSubclassesMap(allClasses)
        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> {
                return classIdToSubclasses[classId] ?: emptySet()
            }

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> {
                return classIdToSubclasses[classMembers.classId]?.let { subclasses ->
                    subclasses.mapTo(mutableSetOf()) { subclass ->
                        ClassMembers(subclass, classMembers.memberNames)
                    }
                } ?: emptySet()
            }
        }
    }

    override fun getReverseResolver(allClasses: ClassSnapshotIndex): ImpactingClassesResolver {
        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> {
                // Same as `getClassIdToSupertypesMap(allClasses)[classId]`, but computed for the requested class only
                return allClasses[classId].flatMapTo(mutableSetOf()) { clazz ->
                    when (clazz) {
                        is RegularKotlinClassSnapshot -> clazz.supertypes.mapNotNull { allClasses.getClassId(it) }
                        // See getClassIdToSupertypesMap for why supertypes of these classes are not needed
                        is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> emptyList()
                        is JavaClassSnapshot -> clazz.supertypes.mapNotNull { allClasses.getClassId(it) }
                    }
                }
            }
        }
    }

    private fun getClassIdToSubclassesMap(allClasses: Iterable<AccessibleClassSnapshot>): Map<ClassId, Set<ClassId>> {
        val classIdToSubclasses = mutableMapOf<ClassId, MutableSet<ClassId>>()
        getClassIdToSupertypesMap(allClasses).forEach { [classId, supertypes] ->
            supertypes.forEach { supertype ->
                classIdToSubclasses.getOrPut(supertype) { mutableSetOf() }.add(classId)
            }
        }
        return classIdToSubclasses
    }

    private fun getClassIdToSupertypesMap(allClasses: Iterable<AccessibleClassSnapshot>): Map<ClassId, Set<ClassId>> {
        val classNameToClassId = allClasses.associate { JvmClassName.byClassId(it.classId) to it.classId }
        return allClasses.mapNotNull { clazz ->
            // Find supertypes that are within `allClasses`, we don't care about those outside `allClasses` (e.g., `java/lang/Object`)
            val supertypes = when (clazz) {
                is RegularKotlinClassSnapshot -> clazz.supertypes.mapNotNullTo(mutableSetOf()) { classNameToClassId[it] }
                is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> {
                    // These classes may have supertypes (e.g., kotlin/collections/ArraysKt (MULTIFILE_CLASS) extends
                    // kotlin/collections/ArraysKt___ArraysKt (MULTIFILE_CLASS_PART)), but we don't have to use that info during impact
                    // analysis because those inheritors and supertypes should have the same package names, and in package facades only the
                    // package names and member names matter.
                    emptySet()
                }
                is JavaClassSnapshot -> clazz.supertypes.mapNotNullTo(mutableSetOf()) { classNameToClassId[it] }
            }
            if (supertypes.isNotEmpty()) {
                clazz.classId to supertypes
            } else null
        }.toMap()
    }
}

/**
 * Describes the impact between a class and its companion object when the companion object defines some constants.
 *
 * Consider the following source file:
 *    class A {
 *       companion object {
 *          const val CONSTANT = 1
 *       }
 *    }
 *
 * This source file will compile into 2 .class files:
 *   - `A.Companion.class`'s Kotlin metadata describes the name and type of `CONSTANT` but not its value. Its Java bytecode does not define
 *     the constant.
 *   - `A.class`'s Kotlin metadata does not contain `CONSTANT`. However, its Java bytecode defines the constant as follows:
 *         public static final int CONSTANT = 1;
 *
 * Therefore, if the value of the constant has changed in the source file, we will only see a change in the Java bytecode of `A.class`, not
 * in `A.Companion.class` or in the Kotlin metadata of either class.
 *
 * Hence, we will need to detect that `A.CONSTANT` impacts `A.Companion.CONSTANT` because if a source file references
 * `A.Companion.CONSTANT`, it will need to be recompiled when `A.CONSTANT`'s value in `A.class` has changed (even though `A.Companion.class`
 * has not changed).
 *
 * Note: This corner case only applies to *constants' values* defined in *companion objects* (it does not apply to constants' names and
 * types, or top-level constants, or constants in non-companion objects, or inline functions).
 */
private object ConstantsInCompanionObjectsImpact : Impact {

    override fun getResolver(allClasses: Iterable<AccessibleClassSnapshot>): ImpactedSymbolsResolver {
        val companionObjectToConstants: Map<ClassId, List<String>> = allClasses.mapNotNull { clazz ->
            (clazz as? RegularKotlinClassSnapshot)?.constantsInCompanionObject?.let { constants ->
                // We only care about companion objects that define some constants
                if (constants.isNotEmpty()) {
                    clazz.classId to constants
                } else null
            }
        }.toMap()
        val classToCompanionObject: Map<ClassId, ClassId> = companionObjectToConstants.keys.associateBy { companionObject ->
            // companionObject.outerClassId should be present in `allClasses` as this is a companion object
            companionObject.outerClassId!!
        }

        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> {
                return setOfNotNull(classToCompanionObject[classId])
            }

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> {
                return classToCompanionObject[classMembers.classId]?.let { companionObject ->
                    val constantsInCompanionObject = companionObjectToConstants[companionObject]!!
                    val impactedConstants = classMembers.memberNames.intersect(constantsInCompanionObject.toSet())
                    setOf(ClassMembers(companionObject, impactedConstants))
                } ?: emptySet()
            }
        }
    }

    override fun getReverseResolver(allClasses: ClassSnapshotIndex): ImpactingClassesResolver {
        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> {
                val isCompanionObjectWithConstants = allClasses[classId].any { clazz ->
                    // We only care about companion objects that define some constants
                    (clazz as? RegularKotlinClassSnapshot)?.constantsInCompanionObject?.isNotEmpty() == true
                }
                return if (isCompanionObjectWithConstants) {
                    // classId.outerClassId should be present in `allClasses` as this is a companion object
                    setOf(classId.outerClassId!!)
                } else emptySet()
            }
        }
    }
}

/**
 * Describes the impact of a top-level type alias on the class it expands to.
 *
 * == Assuming we have "typealias A = B" ==
 * A type alias A = B exposes B's members (e.g., A.foo when B.foo exists). Therefore:
 *   - If B (or one of its members) has changed, A (and the corresponding member of A) is impacted, so a source file that references
 *     A must be recompiled. This is the forward direction ([getResolver]).
 *   - When a package facade is retained during classpath shrinking (e.g., because one of its type aliases is referenced), the classes its
 *     aliases expand to must be retained too; otherwise a later change in the expanded class could not be detected as a change in the
 *     alias. This is the reverse direction ([getReverseResolver]).
 */
private object TypeAliasExpansionImpact : Impact {

    /**
     * == Assuming we have "typealias A = B" ==
     * Forward direction is keyed by the expanded class B: only real classes ever appear in the changed set, and the alias A has no
     * class file of its own. So "B (or B.foo) changed" maps to the alias A (and A.foo) that mirror it, so consumers of A recompile.
     */
    override fun getResolver(allClasses: Iterable<AccessibleClassSnapshot>): ImpactedSymbolsResolver {
        val expandedClassToAliases: Map<ClassId, Set<ClassId>> = getExpandedClassToAliasesMap(allClasses)

        return object : ImpactedSymbolsResolver {
            override fun getImpactedClasses(classId: ClassId): Set<ClassId> {
                return expandedClassToAliases[classId] ?: emptySet()
            }

            override fun getImpactedClassMembers(classMembers: ClassMembers): Set<ClassMembers> =
                expandedClassToAliases[classMembers.classId]
                    ?.map { aliasClassId -> ClassMembers(aliasClassId, classMembers.memberNames) }
                    ?.toSet() ?: emptySet()
        }
    }

    private fun getExpandedClassToAliasesMap(allClasses: Iterable<AccessibleClassSnapshot>): Map<ClassId, Set<ClassId>> =
        buildMap<ClassId, MutableSet<ClassId>> {
            allClasses.filterIsInstance<PackageFacadeKotlinClassSnapshot>().flatMap { it.typeAliases.orEmpty() }.forEach { snapshot ->
                getOrPut(snapshot.expandedClassId) { mutableSetOf() }.add(snapshot.aliasClassId)
            }
        }

    /**
     * == Assuming we have "typealias A = B" ==
     * Reverse direction is keyed by the facade that declares the alias, NOT by the alias A (which has no class file and so never appears
     * in the shrinker's referenced set). When A is referenced, the facade is what gets retained (via its packageMemberNames); retaining
     * it must also retain B, otherwise a later change in B could not be detected. This is deliberately not the exact inverse of the
     * forward map above.
     */
    override fun getReverseResolver(allClasses: ClassSnapshotIndex): ImpactingClassesResolver {
        return object : ImpactingClassesResolver {
            override fun getImpactingClasses(classId: ClassId): Set<ClassId> {
                return allClasses[classId].filterIsInstance<PackageFacadeKotlinClassSnapshot>()
                    .flatMapTo(mutableSetOf()) { facade -> facade.typeAliases.orEmpty().map { it.expandedClassId } }
            }
        }
    }
}

internal object BreadthFirstSearch {

    /**
     * Finds the set of nodes that are *transitively* reachable from the given set of nodes.
     *
     * The returned set is *inclusive* (it contains the given set + the directly/transitively reachable ones).
     */
    fun <T> findReachableNodes(nodes: Iterable<T>, edgesProvider: (T) -> Iterable<T>): Set<T> {
        val visitedAndToVisitNodes = nodes.toMutableSet()
        val nodesToVisit = ArrayDeque(nodes.toSet())

        while (nodesToVisit.isNotEmpty()) {
            val nodeToVisit = nodesToVisit.removeFirst()
            val nextNodesToVisit = edgesProvider.invoke(nodeToVisit) - visitedAndToVisitNodes
            visitedAndToVisitNodes.addAll(nextNodesToVisit)
            nodesToVisit.addAll(nextNodesToVisit)
        }
        return visitedAndToVisitNodes
    }
}
