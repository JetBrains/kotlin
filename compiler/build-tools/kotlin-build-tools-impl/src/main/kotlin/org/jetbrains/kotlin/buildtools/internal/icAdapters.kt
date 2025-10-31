/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.internal.jvm.toOptions
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import kotlin.io.path.exists

internal val SourcesChanges.asChangedFiles
    get() = when (this) {
        is SourcesChanges.Known -> ChangedFiles.DeterminableFiles.Known(modifiedFiles, removedFiles)
        is SourcesChanges.ToBeCalculated -> ChangedFiles.DeterminableFiles.ToBeComputed
        is SourcesChanges.Unknown -> ChangedFiles.Unknown
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

internal fun JvmSnapshotBasedIncrementalCompilationConfiguration.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
    val options = toOptions()
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking = options[PRECISE_JAVA_TRACKING],
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = options[BACKUP_CLASSES],
        keepIncrementalCompilationCachesInMemory = options[KEEP_IC_CACHES_IN_MEMORY],
        enableUnsafeIncrementalCompilationForMultiplatform = options[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM],
        enableMonotonousIncrementalCompileSetExpansion = options[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION],
    )
}

internal val JvmSnapshotBasedIncrementalCompilationConfiguration.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        val options = toOptions()
        val snapshotFiles =
            ClasspathSnapshotFiles(dependenciesSnapshotFiles.map { it.toFile() }, shrunkClasspathSnapshot.toFile().parentFile)
        return when {
            !shrunkClasspathSnapshot.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                snapshotFiles
            )
            options[FORCE_RECOMPILATION] -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                snapshotFiles
            )
            options[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                snapshotFiles
            )
            else -> {
                ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
            }
        }
    }