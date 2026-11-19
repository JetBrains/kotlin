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
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KlibEntrySnapshotterTest : KlibSnapshotTestCommon() {

    private val testDataDir = File(klibTestDataDir, "KlibEntrySnapshotterTest")
    private val classesAndPackageMembersDir = File(testDataDir, "testClassesAndPackageMembers/src")

    @Test
    fun testOneSnapshotPerSourceFile() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibSnapshots

        assertEquals(
            setOf("com/example/ClassesKt", "com/example/TopLevelKt", "com/example/ClassAndTopLevelMembersKt"),
            snapshots.mapTo(mutableSetOf()) { it.id },
        )
        snapshots.forEach {
            when (it) {
                is PackageFragmentSnapshot -> assertEquals(FqName("com.example"), it.packageName)
            }
        }
    }

    @Test
    fun testAccessibleClassesAndSupertypes() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibSnapshots.associateBy { it.id }
        val snapshot = assertIs<PackageFragmentSnapshot>(snapshots.getValue("com/example/ClassesKt"))
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
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibSnapshots.associateBy { it.id }

        assertEquals(
            setOf("publicFunction", "publicProperty", "internalFunction", "PublicTypeAlias"),
            assertIs<PackageFragmentSnapshot>(snapshots.getValue("com/example/TopLevelKt")).packageMemberNames,
        )
        assertEquals(emptySet(), assertIs<PackageFragmentSnapshot>(snapshots.getValue("com/example/ClassesKt")).packageMemberNames)
    }

    @Test
    fun testClassAndTopLevelMembersInOneFile() {
        val snapshots = snapshotKlib(classesAndPackageMembersDir).klibSnapshots.associateBy { it.id }
        val snapshot = assertIs<PackageFragmentSnapshot>(snapshots.getValue("com/example/ClassAndTopLevelMembersKt"))

        assertEquals(listOf(ClassId.fromString("com/example/SomeClass")), snapshot.classInfos.map { it.classId })
        assertEquals(setOf("topLevelFunction", "topLevelProperty"), snapshot.packageMemberNames)
    }

    @Test
    fun testGranularity() {
        fun KlibSnapshot.memberLevelData(): Any? = when (this) {
            is PackageFragmentSnapshot -> packageFragmentProto
        }

        snapshotKlib(classesAndPackageMembersDir, CLASS_MEMBER_LEVEL).klibSnapshots.forEach { assertNotNull(it.memberLevelData()) }
        snapshotKlib(classesAndPackageMembersDir, CLASS_LEVEL).klibSnapshots.forEach { assertNull(it.memberLevelData()) }
    }

    @Test
    fun testContentHashIsStable() {
        val first = snapshotKlib(classesAndPackageMembersDir).klibSnapshots.associate { it.id to it.contentHash }
        val second = snapshotKlib(classesAndPackageMembersDir).klibSnapshots.associate { it.id to it.contentHash }

        assertEquals(first, second)
    }

    @Test
    fun testContentHashChangesOnlyOnAbiChange() {
        fun contentHashes(dir: String) = snapshotKlib(File(testDataDir, "testContentHash/src/$dir"))
            .klibSnapshots.associate { it.id to it.contentHash }

        val current = contentHashes("current")
        val previous = contentHashes("previous")

        assertNotEquals(previous.getValue("com/example/AbiChangedKt"), current.getValue("com/example/AbiChangedKt"))
        assertEquals(previous.getValue("com/example/BodyChangedKt"), current.getValue("com/example/BodyChangedKt"))
    }
}
