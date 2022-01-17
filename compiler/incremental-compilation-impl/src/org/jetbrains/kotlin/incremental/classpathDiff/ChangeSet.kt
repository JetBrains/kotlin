/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/** Set of classes, class members, and top-level members that are changed (or impacted by a change). */
class ChangeSet(

    /** Set of changed classes, preferably ordered by not required. */
    val changedClasses: Set<ClassId>,

    /**
     * Map from a [ClassId] to the names of changed Java fields/methods or Kotlin properties/functions within that class.
     *
     * The map and sets are preferably ordered but not required.
     *
     * The [ClassId]s must not appear in [changedClasses] to avoid redundancy.
     */
    val changedClassMembers: Map<ClassId, Set<String>>,

    /** Map from a package name to the names of changed Kotlin top-level properties/functions within that package. */
    val changedTopLevelMembers: Map<FqName, Set<String>>
) {
    init {
        check(changedClassMembers.keys.none { it in changedClasses })
    }

    class Collector {
        private val changedClasses = mutableSetOf<ClassId>()
        private val changedClassMembers = mutableMapOf<ClassId, MutableSet<String>>()
        private val changedTopLevelMembers = mutableMapOf<FqName, MutableSet<String>>()

        fun addChangedClasses(classNames: Collection<ClassId>) = changedClasses.addAll(classNames)

        fun addChangedClass(className: ClassId) = changedClasses.add(className)

        fun addChangedClassMembers(className: ClassId, memberNames: Collection<String>) {
            if (memberNames.isNotEmpty()) {
                changedClassMembers.computeIfAbsent(className) { mutableSetOf() }.addAll(memberNames)
            }
        }

        fun addChangedClassMember(className: ClassId, memberName: String) = addChangedClassMembers(className, listOf(memberName))

        fun addChangedTopLevelMembers(packageMemberSet: PackageMemberSet) {
            packageMemberSet.packageToMembersMap.forEach { (packageName, memberNames) ->
                changedTopLevelMembers.computeIfAbsent(packageName) { mutableSetOf() }.addAll(memberNames)
            }
        }

        fun addChangedTopLevelMembers(packageName: FqName, topLevelMembers: Collection<String>) {
            if (topLevelMembers.isNotEmpty()) {
                changedTopLevelMembers.computeIfAbsent(packageName) { mutableSetOf() }.addAll(topLevelMembers)
            }
        }

        fun addChangedTopLevelMember(packageName: FqName, topLevelMember: String) =
            addChangedTopLevelMembers(packageName, listOf(topLevelMember))

        fun getChanges(): ChangeSet {
            // Remove redundancy in the change set first
            changedClassMembers.keys.intersect(changedClasses).forEach { changedClassMembers.remove(it) }
            return ChangeSet(changedClasses.toSet(), changedClassMembers.toMap(), changedTopLevelMembers.toMap())
        }
    }

    fun isEmpty() = changedClasses.isEmpty() && changedClassMembers.isEmpty() && changedTopLevelMembers.isEmpty()

    operator fun plus(other: ChangeSet) = ChangeSet(
        changedClasses + other.changedClasses,
        changedClassMembers + other.changedClassMembers,
        changedTopLevelMembers + other.changedTopLevelMembers
    )

    internal fun getChanges(): ChangesEither.Known {
        val lookupSymbols = mutableSetOf<LookupSymbol>()
        val fqNames = mutableSetOf<FqName>()

        changedClasses.forEach {
            val classFqName = it.asSingleFqName()
            lookupSymbols.add(LookupSymbol(name = classFqName.shortName().asString(), scope = classFqName.parent().asString()))
            fqNames.add(classFqName)
        }

        for ((changedClass, changedClassMembers) in changedClassMembers) {
            val classFqName = changedClass.asSingleFqName()
            changedClassMembers.forEach {
                lookupSymbols.add(LookupSymbol(name = it, scope = classFqName.asString()))
            }
            fqNames.add(classFqName)
        }

        for ((changedPackage, changedTopLevelMembers) in changedTopLevelMembers) {
            changedTopLevelMembers.forEach {
                lookupSymbols.add(LookupSymbol(name = it, scope = changedPackage.asString()))
            }
            fqNames.add(changedPackage)
        }

        return ChangesEither.Known(lookupSymbols, fqNames)
    }
}
