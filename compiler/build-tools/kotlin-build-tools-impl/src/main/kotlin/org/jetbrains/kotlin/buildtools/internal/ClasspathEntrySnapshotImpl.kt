/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.io.File

class ClasspathEntrySnapshotImpl(
    private val origin: org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot,
) :
    ClasspathEntrySnapshot {
    override val classSnapshots: Map<String, ClassSnapshot>
        get() = origin.classSnapshots.mapValues {
            when (val snapshot = it.value) {
                is AccessibleClassSnapshot -> AccessibleClassSnapshotImpl(snapshot.classAbiHash)
                is InaccessibleClassSnapshot -> InaccessibleClassSnapshotImpl
            }
        }

    override fun saveSnapshot(path: File) {
        ClasspathEntrySnapshotExternalizer.saveToFile(path, origin)
    }
}