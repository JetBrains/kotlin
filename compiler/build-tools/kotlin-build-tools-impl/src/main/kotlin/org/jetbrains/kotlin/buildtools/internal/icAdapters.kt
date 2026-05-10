/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.internal.BaseIncrementalCompilationConfigurationImpl.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.HasSnapshotBasedIcOptionsAccessor
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures

internal val SourcesChanges.asChangedFiles
    get() = when (this) {
        is SourcesChanges.Known -> ChangedFiles.DeterminableFiles.Known(modifiedFiles, removedFiles)
        is SourcesChanges.ToBeCalculated -> ChangedFiles.DeterminableFiles.ToBeComputed
        is SourcesChanges.Unknown -> ChangedFiles.Unknown
    }

@Suppress("DEPRECATION_ERROR")
internal val AggregatedIcConfiguration<org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalCompilationApproachParameters>.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        check(options is org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration) {
            "options expected to be an instance of ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration"
        }
        val params = parameters
        val snapshotFiles = ClasspathSnapshotFiles(params.newClasspathSnapshotFiles, params.shrunkClasspathSnapshot.parentFile)
        return when {
            !snapshotFiles.shrunkPreviousClasspathSnapshotFile.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
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

internal fun HasSnapshotBasedIcOptionsAccessor.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
    val options = this
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking = options[JvmSnapshotBasedIncrementalCompilationOptionsImpl.PRECISE_JAVA_TRACKING],
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = options[BACKUP_CLASSES],
        keepIncrementalCompilationCachesInMemory = options[KEEP_IC_CACHES_IN_MEMORY],
        enableUnsafeIncrementalCompilationForMultiplatform = options[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM],
        enableMonotonousIncrementalCompileSetExpansion = options[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION],
    )
}
internal fun JsHistoryBasedIncrementalCompilationConfigurationImpl.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures {
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking = false,
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = this[BACKUP_CLASSES],
        keepIncrementalCompilationCachesInMemory = this[KEEP_IC_CACHES_IN_MEMORY],
        enableUnsafeIncrementalCompilationForMultiplatform = this[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM],
        enableMonotonousIncrementalCompileSetExpansion = this[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION],
    )
}

internal val HasSnapshotBasedIcOptionsAccessor.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        val options: HasSnapshotBasedIcOptionsAccessor = this
        val snapshotFiles =
            ClasspathSnapshotFiles(
                options.dependenciesSnapshotFiles.map { it.toFile() },
                options.shrunkClasspathSnapshot.toFile().parentFile
            )
        return when {
            !snapshotFiles.shrunkPreviousClasspathSnapshotFile.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                snapshotFiles
            )
            options[FORCE_RECOMPILATION] -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                snapshotFiles
            )
            options[JvmSnapshotBasedIncrementalCompilationOptionsImpl.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                snapshotFiles
            )
            else -> {
                ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
            }
        }
    }
