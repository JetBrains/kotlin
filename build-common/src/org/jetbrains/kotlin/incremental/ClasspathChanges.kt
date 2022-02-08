/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled
import java.io.File
import java.io.Serializable

/**
 * Changes to the classpath of the `KotlinCompile` task, or information to compute them later by the Kotlin incremental compiler (see
 * [ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler].
 */
sealed class ClasspathChanges : Serializable {

    sealed class ClasspathSnapshotEnabled : ClasspathChanges() {

        abstract val classpathSnapshotFiles: ClasspathSnapshotFiles

        sealed class IncrementalRun : ClasspathSnapshotEnabled() {

            class NoChanges(override val classpathSnapshotFiles: ClasspathSnapshotFiles) : IncrementalRun()

            class ToBeComputedByIncrementalCompiler(override val classpathSnapshotFiles: ClasspathSnapshotFiles) : IncrementalRun()
        }

        class NotAvailableDueToMissingClasspathSnapshot(override val classpathSnapshotFiles: ClasspathSnapshotFiles) :
            ClasspathSnapshotEnabled()

        class NotAvailableForNonIncrementalRun(override val classpathSnapshotFiles: ClasspathSnapshotFiles) : ClasspathSnapshotEnabled()
    }

    object ClasspathSnapshotDisabled : ClasspathChanges()

    object NotAvailableForJSCompiler : ClasspathChanges()
}

class ClasspathSnapshotFiles(
    val currentClasspathEntrySnapshotFiles: List<File>,
    classpathSnapshotDir: File
) : Serializable {

    val shrunkPreviousClasspathSnapshotFile: File = File(classpathSnapshotDir, "shrunk-classpath-snapshot.bin")

    companion object {
        private const val serialVersionUID = 0L
    }
}
