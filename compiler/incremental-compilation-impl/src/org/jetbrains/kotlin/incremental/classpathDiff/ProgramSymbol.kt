/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Similar to [LookupSymbol] but with information about its kind (i.e., whether it's a [ClassSymbol], [ClassMember], or [PackageMember]).
 */
sealed class ProgramSymbol

data class ClassSymbol(val classId: ClassId) : ProgramSymbol()

data class ClassMember(val classId: ClassId, val memberName: String) : ProgramSymbol()

data class PackageMember(val packageFqName: FqName, val memberName: String) : ProgramSymbol()

/** Compact representation for a set of [PackageMember]s. */
class PackageMemberSet(val packageToMembersMap: Map<FqName, Set<String>>) {

    operator fun contains(other: PackageMember): Boolean {
        return packageToMembersMap[other.packageFqName]?.let { other.memberName in it } ?: false
    }

    fun containsElementsIn(other: PackageMemberSet): Boolean {
        return this.packageToMembersMap.keys.intersect(other.packageToMembersMap.keys).any { commonPackage ->
            val otherMembers = other.packageToMembersMap[commonPackage]!!
            this.packageToMembersMap[commonPackage]!!.any { it in otherMembers }
        }
    }
}

fun Set<PackageMember>.compact(): PackageMemberSet {
    val map = mutableMapOf<FqName, MutableSet<String>>()
    forEach {
        map.getOrPut(it.packageFqName) { mutableSetOf() }.add(it.memberName)
    }
    return PackageMemberSet(map)
}

fun List<PackageMemberSet>.combine(): PackageMemberSet {
    val combinedMap = mutableMapOf<FqName, MutableSet<String>>()
    forEach {
        it.packageToMembersMap.forEach { (packageFqName, memberNames) ->
            combinedMap.getOrPut(packageFqName) { mutableSetOf() }.addAll(memberNames)
        }
    }
    return PackageMemberSet(combinedMap)
}

/**
 * Finds [LookupSymbol]s that potentially refer to classes on the given classpath, and returns the [ProgramSymbol]s corresponding to those
 * [LookupSymbol]s.
 *
 * Note: Some [LookupSymbol]s may refer to a class outside the classpath (e.g., `java/lang/Object`, or a class in the sources being
 * compiled). The returned result will not include those symbols.
 *
 * The given classpath must not contain duplicate classes.
 *
 * It's okay to over-approximate the result.
 */
internal fun Collection<LookupSymbol>.filterLookupSymbols(classpath: List<AccessibleClassSnapshot>): List<ProgramSymbol> {
    val regularClassesOnClasspath: List<ClassId> = classpath.mapNotNull {
        when (it) {
            is RegularKotlinClassSnapshot -> it.classId
            is JavaClassSnapshot -> it.classId
            is PackageFacadeKotlinClassSnapshot, is MultifileClassKotlinClassSnapshot -> null
        }
    }
    val packageMembersOnClasspath: PackageMemberSet = classpath.mapNotNull {
        when (it) {
            is RegularKotlinClassSnapshot, is JavaClassSnapshot -> null
            is PackageFacadeKotlinClassSnapshot -> it.packageMembers
            is MultifileClassKotlinClassSnapshot -> it.constants
        }
    }.combine()

    // It's rare but possible for 2 ClassIds to have the same FqName (e.g., ClassId `com/example/Foo` and ClassId `com/example.Foo` both
    // have FqName `com.example.Foo`. ClassId `com/example/Foo` indicates class `Foo` in package `com/example', whereas ClassId
    // `com/example.Foo` indicates nested class `Foo` of class `example` in package `com`).
    val fqNameToClassIds: Map<FqName, List<ClassId>> = regularClassesOnClasspath.groupBy { it.asSingleFqName() }

    return this.flatMap {
        val lookupSymbolFqName = if (it.scope.isEmpty()) FqName(it.name) else FqName("${it.scope}.${it.name}")
        val lookupSymbolClassIds: List<ClassId> = fqNameToClassIds[lookupSymbolFqName] ?: emptyList()

        val scopeFqName = FqName(it.scope)
        val scopeClassIds: List<ClassId> = fqNameToClassIds[scopeFqName] ?: emptyList()

        val packageMember = PackageMember(scopeFqName, it.name)

        // A LookupSymbol may refer to one of the following types:
        //    1. A class
        //    2. A class member
        //    3. A package member
        // Note: It's also possible that a LookupSymbol may refer to more than one type (e.g., LookupSymbol(scope = "com.example.Foo",
        // name = "Bar") may refer to a nested class or a class property/function; LookupSymbol(scope = "com.example", name = "Foo") may
        // refer to a class or a package-level property/function). In the following, we will collect all possible cases (it's okay to
        // over-approximate the result).
        val classSymbols = lookupSymbolClassIds.map { classId -> ClassSymbol(classId) }

        // To check if a LookupSymbol refers to a class member, we'll need to check that (1) its scope refers to a class, and (2) its name
        // actually refers to a member of that class. Because checking (2) is expensive, and it's okay to over-approximate the result, we're
        // checking (1) only.
        val classMembers = scopeClassIds.map { classId -> ClassMember(classId, it.name) }

        val packageMembers = if (packageMember in packageMembersOnClasspath) listOf(packageMember) else emptyList()

        return@flatMap classSymbols + classMembers + packageMembers
    }
}
