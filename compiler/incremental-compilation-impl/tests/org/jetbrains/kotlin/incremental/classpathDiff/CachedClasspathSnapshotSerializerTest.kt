/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.incremental.storage.saveToFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CachedClasspathSnapshotSerializerTest {

    @field:TempDir
    lateinit var tmpDir: File

    private val reporter = ClasspathSnapshotBuildReporter(DoNothingBuildReporter)

    @Test
    fun `test overwritten snapshot file is not served from cache`() {
        val snapshotFile = tmpDir.resolve("snapshot.bin")

        val snapshot1 = ClasspathEntrySnapshot(linkedMapOf("com/example/A.class" to InaccessibleClassSnapshot))
        ClasspathEntrySnapshotExternalizer.saveToFile(snapshotFile, snapshot1)
        val loaded1 = CachedClasspathSnapshotSerializer.load(listOf(snapshotFile), reporter)
        assertEquals(1, loaded1.classpathEntrySnapshots.single().classSnapshots.size)

        // Overwrite the same file with different content. The serialized sizes differ (2 entries vs 1),
        // so FileKey changes even if the OS timestamp resolution prevents lastModified from changing.
        val snapshot2 = ClasspathEntrySnapshot(
            linkedMapOf(
                "com/example/A.class" to InaccessibleClassSnapshot,
                "com/example/B.class" to InaccessibleClassSnapshot,
            )
        )
        ClasspathEntrySnapshotExternalizer.saveToFile(snapshotFile, snapshot2)
        val loaded2 = CachedClasspathSnapshotSerializer.load(listOf(snapshotFile), reporter)
        assertEquals(2, loaded2.classpathEntrySnapshots.single().classSnapshots.size)
    }
}
