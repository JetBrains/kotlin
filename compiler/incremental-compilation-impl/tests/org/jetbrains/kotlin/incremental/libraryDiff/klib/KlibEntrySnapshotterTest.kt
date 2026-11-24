/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KlibEntrySnapshotterTest : KlibSnapshotTestCommon() {

    private val testDataDir = File(klibTestDataDir, "KlibEntrySnapshotterTest")
    private val classesAndPackageMembersDir = File(testDataDir, "testClassesAndPackageMembers/src")

    @Test
    fun testOneSnapshotPerSourceFile() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots

        assertEquals(
            setOf("com/example/Classes.kt", "com/example/TopLevel.kt", "com/example/ClassAndTopLevelMembers.kt"),
            snapshots.mapTo(mutableSetOf()) { it.sourceFilePath },
        )
        snapshots.forEach { assertEquals(FqName("com.example"), it.packageName) }
    }

    @Test
    fun testAccessibleClassesAndSupertypes() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots.associateBy { it.sourceFilePath }
        val snapshot = snapshots.getValue("com/example/Classes.kt")
        val classInfos = snapshot.classInfos.associateBy { it.classId }

        assertEquals(
            setOf(
                "com/example/Base",
                "com/example/SomeInterface",
                "com/example/Derived",
                "com/example/Outer",
                "com/example/Outer.PublicNested"
            ),
            classInfos.keys.mapTo(mutableSetOf()) { it.asString() },
        )
        assertEquals(
            setOf(ClassId.fromString("com/example/Base"), ClassId.fromString("com/example/SomeInterface")),
            classInfos.getValue(ClassId.fromString("com/example/Derived")).supertypes.toSet(),
        )
    }

    @Test
    fun testNonPrivatePackageMemberNames() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots.associateBy { it.sourceFilePath }

        assertEquals(
            setOf("publicFunction", "publicProperty", "internalFunction", "PublicTypeAlias"),
            snapshots.getValue("com/example/TopLevel.kt").packageMemberNames,
        )
        assertEquals(emptySet(), snapshots.getValue("com/example/Classes.kt").packageMemberNames)
    }

    @Test
    fun testClassAndTopLevelMembersInOneFile() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots.associateBy { it.sourceFilePath }
        val snapshot = snapshots.getValue("com/example/ClassAndTopLevelMembers.kt")

        assertEquals(listOf(ClassId.fromString("com/example/SomeClass")), snapshot.classInfos.map { it.classId })
        assertEquals(setOf("topLevelFunction", "topLevelProperty"), snapshot.packageMemberNames)
    }

    @Test
    fun testGranularity() {
        snapshotKlib(classesAndPackageMembersDir, CLASS_MEMBER_LEVEL).klibFileSnapshots
            .forEach { assertNotNull(it.classMemberLevelSnapshot) }
        snapshotKlib(classesAndPackageMembersDir, CLASS_LEVEL).klibFileSnapshots
            .forEach { assertNull(it.classMemberLevelSnapshot) }
    }

    @Test
    fun testContentHashIsStable() {
        val first = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots.associate { it.sourceFilePath to it.contentHash }
        val second = snapshotKlib(classesAndPackageMembersDir).klibFileSnapshots.associate { it.sourceFilePath to it.contentHash }

        assertEquals(first, second)
    }

    @Test
    fun testContentHashChangesOnlyOnAbiChange() {
        fun contentHashes(dir: String) = snapshotKlib(File(testDataDir, "testContentHash/src/$dir"))
            .klibFileSnapshots.associate { it.sourceFilePath to it.contentHash }

        val current = contentHashes("current")
        val previous = contentHashes("previous")

        assertNotEquals(previous.getValue("com/example/AbiChanged.kt"), current.getValue("com/example/AbiChanged.kt"))
        assertEquals(previous.getValue("com/example/BodyChanged.kt"), current.getValue("com/example/BodyChanged.kt"))
    }
}
