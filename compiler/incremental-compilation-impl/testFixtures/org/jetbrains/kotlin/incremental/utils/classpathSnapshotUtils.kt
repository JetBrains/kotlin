/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshotExternalizer
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.io.File


fun makeEmptyClasspathChangesForSingleModuleTests(classpathSnapshotDir: File): ClasspathChanges {
    val snapshotFiles = ClasspathSnapshotFiles(
        currentClasspathEntrySnapshotFiles = emptyList(),
        classpathSnapshotDir = classpathSnapshotDir
    )

    if (!snapshotFiles.shrunkPreviousClasspathSnapshotFile.exists()) {
        ListExternalizer(AccessibleClassSnapshotExternalizer).saveToFile(
            snapshotFiles.shrunkPreviousClasspathSnapshotFile, emptyList()
        )
    }

    return NoChanges(snapshotFiles)
}
