/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm

import org.jetbrains.kotlin.buildtools.api.v2.jvm.AccessibleJvmClassSnapshot
import org.jetbrains.kotlin.buildtools.api.v2.jvm.InaccessibleJvmClassSnapshot
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClassSnapshot
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.nio.file.Path

class JvmClasspathEntrySnapshotImpl(
    private val origin: org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot,
) : JvmClasspathEntrySnapshot {

    override val classSnapshots: Map<String, JvmClassSnapshot> = origin.classSnapshots.mapValues {
        when (val snapshot = it.value) {
            is AccessibleClassSnapshot -> AccessibleJvmClassSnapshotImpl(snapshot.classAbiHash)
            is InaccessibleClassSnapshot -> InaccessibleJvmClassSnapshotImpl
        }
    }

    override fun saveSnapshot(path: Path) {
        return ClasspathEntrySnapshotExternalizer.saveToFile(path.toFile(), origin)
    }

}

object InaccessibleJvmClassSnapshotImpl : InaccessibleJvmClassSnapshot

class AccessibleJvmClassSnapshotImpl(override val classAbiHash: Long) : AccessibleJvmClassSnapshot