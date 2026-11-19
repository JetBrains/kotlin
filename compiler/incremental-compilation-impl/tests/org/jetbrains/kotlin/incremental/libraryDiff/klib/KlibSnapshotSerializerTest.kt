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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

/** The klib counterpart of `ClasspathSnapshotSerializerTest`: a snapshot must be the same after a save / read round trip. */
@Disabled("KT-90559: KlibSnapshotSerializer is not implemented yet")
class KlibSnapshotSerializerTest : KlibSnapshotTestCommon() {

    @Test
    fun testRoundTrip_ClassMemberLevel() = testRoundTrip(CLASS_MEMBER_LEVEL)

    @Test
    fun testRoundTrip_ClassLevel() = testRoundTrip(CLASS_LEVEL)

    private fun testRoundTrip(granularity: ClassSnapshotGranularity) {
        val snapshot = snapshotKlib(File(klibTestDataDir, "KlibEntrySnapshotterTest/testClassesAndPackageMembers/src"), granularity)
        val deserialized = KlibEntrySnapshotExternalizer.fromByteArray(KlibEntrySnapshotExternalizer.toByteArray(snapshot))

        assertEquals(snapshot.toDebugString(), deserialized.toDebugString())
    }

    /** The snapshot classes have no `equals`, so compare a full text dump, sorted by `id` (the set order is not part of the contract). */
    private fun KlibEntrySnapshot.toDebugString(): String = klibSnapshots.sortedBy { it.id }.joinToString("\n") { snapshot ->
        when (snapshot) {
            is PackageFragmentSnapshot -> with(snapshot) {
                "id=$id, contentHash=$contentHash, packageName=$packageName, " +
                        "classInfos=${classInfos.map { "${it.classId}: ${it.supertypes}" }}, " +
                        "packageMemberNames=$packageMemberNames, " +
                        "packageFragmentProto=${packageFragmentProto?.map { it.contentToString() }}"
            }
        }
    }
}
