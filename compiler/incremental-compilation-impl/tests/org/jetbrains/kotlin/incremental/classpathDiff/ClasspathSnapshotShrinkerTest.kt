/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ClasspathSnapshotShrinkerTest {

    private val root = classId("", "Root")
    private val outerBase = classId("com.example", "Outer.Base")
    private val sub = classId("com.example", "Sub")
    private val dollarName = classId("com.example", "Weird\$Name")
    private val subOfDollarName = classId("com.example.other", "SubOfWeird")
    private val withCompanion = classId("com.example", "WithConstants")
    private val companion = classId("com.example", "WithConstants.Companion")
    private val facade = classId("com.example", "UtilsKt")
    private val unrelated = classId("org.unrelated", "Unrelated")

    private val allClasses = listOf(
        kotlinClass(root, supertypes = listOf("java/lang/Object")),
        javaClass(outerBase, supertypes = listOf("Root")),
        kotlinClass(sub, supertypes = listOf("com/example/Outer\$Base")),
        kotlinClass(dollarName),
        javaClass(subOfDollarName, supertypes = listOf("com/example/Weird\$Name")),
        kotlinClass(withCompanion),
        kotlinClass(companion, constantsInCompanionObject = listOf("CONSTANT")),
        PackageFacadeKotlinClassSnapshot(facade, 0L, null, packageMemberNames = setOf("util"), typeAliases = null),
        kotlinClass(unrelated, supertypes = listOf("com/example/Sub")),
    )

    private fun shrink(vararg lookups: Pair<String, String>): Set<ClassId> =
        ClasspathSnapshotShrinker.shrinkClasses(allClasses, lookups.map { LookupSymbolKey(it.first, it.second) })
            .map { it.classId }.toSet()

    @Test
    fun `supertypes are retained transitively across nested, Java, and root-package classes`() {
        assertEquals(setOf(sub, outerBase, root), shrink("Sub" to "com.example"))
    }

    @Test
    fun `supertype whose simple name contains a dollar sign is retained`() {
        assertEquals(setOf(subOfDollarName, dollarName), shrink("SubOfWeird" to "com.example.other"))
    }

    @Test
    fun `nested class is referenced by a lookup in the scope of its outer class or in its own scope`() {
        assertEquals(setOf(outerBase, root), shrink("Base" to "com.example.Outer"))
        assertEquals(setOf(outerBase, root), shrink("someMember" to "com.example.Outer.Base"))
    }

    @Test
    fun `companion object defining constants retains its outer class`() {
        assertEquals(setOf(companion, withCompanion), shrink("CONSTANT" to "com.example.WithConstants.Companion"))
    }

    @Test
    fun `package facade is referenced by a lookup of one of its members`() {
        assertEquals(setOf(facade), shrink("util" to "com.example"))
        assertEquals(emptySet<ClassId>(), shrink("notAMember" to "com.example"))
    }

    @Test
    fun `nothing is retained when lookups do not reference the classpath`() {
        assertEquals(emptySet<ClassId>(), shrink("fresh" to "p3.C150", "fresh" to "p3", "fresh" to "kotlin", "fresh" to ""))
    }

    private fun classId(packageName: String, relativeName: String) = ClassId(FqName(packageName), FqName(relativeName), isLocal = false)

    private fun kotlinClass(classId: ClassId, supertypes: List<String> = emptyList(), constantsInCompanionObject: List<String>? = null) =
        RegularKotlinClassSnapshot(
            classId, classAbiHash = 0L, classMemberLevelSnapshot = null, supertypes.map { JvmClassName.byInternalName(it) },
            companionObjectName = null, constantsInCompanionObject
        )

    private fun javaClass(classId: ClassId, supertypes: List<String>) =
        JavaClassSnapshot(classId, classAbiHash = 0L, classMemberLevelSnapshot = null, supertypes.map { JvmClassName.byInternalName(it) })
}
