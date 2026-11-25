/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.storage.fromByteArray
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class KlibSnapshotSerializerTest {

    @ParameterizedTest(name = "{0}: {displayName}")
    @MethodSource("snapshots")
    fun `test KlibSnapshotExternalizer`(originalSnapshot: KlibFileSnapshot) {
        val serializedSnapshot = KlibSnapshotExternalizer.toByteArray(originalSnapshot)
        val deserializedSnapshot = KlibSnapshotExternalizer.fromByteArray(serializedSnapshot)

        assertSnapshotEquals(originalSnapshot, deserializedSnapshot)
    }

    private fun assertSnapshotEquals(expected: KlibFileSnapshot, actual: KlibFileSnapshot) {
        assertEquals(expected.contentHash, actual.contentHash)
        assertEquals(expected.packageName, actual.packageName)
        assertEquals(expected.classInfos.map { it.classId to it.supertypes }, actual.classInfos.map { it.classId to it.supertypes })
        assertEquals(expected.packageMemberNames, actual.packageMemberNames)
        assertEquals(expected.classMemberLevelSnapshot?.packageName, actual.classMemberLevelSnapshot?.packageName)
        assertEquals(
            expected.classMemberLevelSnapshot?.packageFragmentProto?.toList(),
            actual.classMemberLevelSnapshot?.packageFragmentProto?.toList(),
        )
    }

    companion object {
        @JvmStatic
        fun snapshots(): List<Named<KlibFileSnapshot>> = ClassSnapshotGranularity.entries.map { granularity ->
            named("[$granularity]", snapshot(granularity))
        }

        private fun snapshot(granularity: ClassSnapshotGranularity) = KlibFileSnapshot(
            packageName = FqName("com.example"),
            contentHash = 42L,
            classInfos = listOf(
                KlibClassInfo(
                    ClassId.fromString("com/example/SomeClass"),
                    supertypes = listOf(ClassId.fromString("com/example/SomeInterface"))
                ),
                KlibClassInfo(ClassId.fromString("com/example/Outer.Nested"), supertypes = emptyList()),
            ),
            packageMemberNames = setOf("topLevelFunction", "topLevelProperty"),
            classMemberLevelSnapshot = when (granularity) {
                CLASS_LEVEL -> null
                CLASS_MEMBER_LEVEL -> KlibFileInfo(FqName("com.example"), byteArrayOf(1, 2, 3))
            },
        )
    }
}
