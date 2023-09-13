/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles

internal val SourcesChanges.asChangedFiles
    get() = when (this) {
        is SourcesChanges.Known -> ChangedFiles.Known(modifiedFiles, removedFiles)
        is SourcesChanges.ToBeCalculated -> ChangedFiles.Unknown() // TODO: add proper support for SourcesChanges.ToBeCalculated
        is SourcesChanges.Unknown -> ChangedFiles.Unknown()
    }

internal val AggregatedIcConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        check(options is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration) {
            "options expected to be an instance of ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration"
        }
        val params = parameters
        val snapshotFiles = ClasspathSnapshotFiles(params.newClasspathSnapshotFiles, params.shrunkClasspathSnapshot.parentFile)
        return when {
            !params.shrunkClasspathSnapshot.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                snapshotFiles
            )
            options.forcedNonIncrementalMode -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                snapshotFiles
            )
            options.assuredNoClasspathSnapshotsChanges -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                snapshotFiles
            )
            else -> {
                ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
            }
        }
    }