/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.server.ClasspathChangesProto
import org.jetbrains.kotlin.server.ClasspathSnapshotFilesProto
import java.io.File

fun ClasspathChanges.toProto(): ClasspathChangesProto {
    return ClasspathChangesProto.newBuilder()
        .apply {
            when (this@toProto) {
                is ClasspathChanges.ClasspathSnapshotEnabled -> {
                    val classpathSnapshotEnabledBuilder = ClasspathChangesProto.ClasspathSnapshotEnabledProto.newBuilder()
                        .setClasspathSnapshotFiles(
                            ClasspathSnapshotFilesProto.newBuilder()
                                .addAllCurrentClasspathEntrySnapshotFiles(this@toProto.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map { it.absolutePath })
                                .setClasspathSnapshotDir(
                                    this@toProto.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.parentFile?.absolutePath
                                )
                                .build()
                        )
                    when (this@toProto) {
                        is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges -> {
                            classpathSnapshotEnabledBuilder.setIncrementalRun(
                                ClasspathChangesProto.ClasspathSnapshotEnabledProto.IncrementalRunProto.newBuilder()
                                    .setNoChanges(ClasspathChangesProto.ClasspathSnapshotEnabledProto.IncrementalRunProto.NoChangesProto.getDefaultInstance())
                                    .build()
                            )
                        }
                        is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler -> {
                            classpathSnapshotEnabledBuilder.setIncrementalRun(
                                ClasspathChangesProto.ClasspathSnapshotEnabledProto.IncrementalRunProto.newBuilder()
                                    .setToBeComputedByIncrementalCompiler(ClasspathChangesProto.ClasspathSnapshotEnabledProto.IncrementalRunProto.ToBeComputedByIncrementalCompilerProto.getDefaultInstance())
                                    .build()
                            )
                        }
                        is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot -> {
                            classpathSnapshotEnabledBuilder.setNotAvailableDueToMissingClasspathSnapshot(
                                ClasspathChangesProto.ClasspathSnapshotEnabledProto.NotAvailableDueToMissingClasspathSnapshotProto.getDefaultInstance()
                            )
                        }
                        is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> {
                            classpathSnapshotEnabledBuilder.setNotAvailableForNonIncrementalRun(
                                ClasspathChangesProto.ClasspathSnapshotEnabledProto.NotAvailableForNonIncrementalRunProto.getDefaultInstance()
                            )
                        }
                    }
                    setClasspathSnapshotEnabled(classpathSnapshotEnabledBuilder.build())
                }
                ClasspathChanges.ClasspathSnapshotDisabled -> {
                    setClasspathSnapshotDisabled(ClasspathChangesProto.ClasspathSnapshotDisabledProto.getDefaultInstance())
                }
                ClasspathChanges.NotAvailableForJSCompiler -> {
                    setNotAvailableForJsCompiler(ClasspathChangesProto.NotAvailableForJSCompilerProto.getDefaultInstance())
                }
            }
        }
        .build()
}

fun ClasspathChangesProto.toDomain(): ClasspathChanges {
    return when {
        hasClasspathSnapshotDisabled() -> ClasspathChanges.ClasspathSnapshotDisabled
        hasNotAvailableForJsCompiler() -> ClasspathChanges.NotAvailableForJSCompiler
        hasClasspathSnapshotEnabled() -> {
            val filesProto = classpathSnapshotEnabled.classpathSnapshotFiles
            val snapshotFiles = ClasspathSnapshotFiles(
                currentClasspathEntrySnapshotFiles = filesProto.currentClasspathEntrySnapshotFilesList.map { File(it) },
                classpathSnapshotDir = File(filesProto.classpathSnapshotDir)
            )

            when{
                classpathSnapshotEnabled.hasIncrementalRun() -> {
                    when {
                        classpathSnapshotEnabled.incrementalRun.hasNoChanges() -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(snapshotFiles)
                        classpathSnapshotEnabled.incrementalRun.hasToBeComputedByIncrementalCompiler() -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
                        else -> {
                            ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(snapshotFiles)
                        }

                    }
                }
                classpathSnapshotEnabled.hasNotAvailableDueToMissingClasspathSnapshot() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(snapshotFiles)
                classpathSnapshotEnabled.hasNotAvailableForNonIncrementalRun() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(snapshotFiles)
                else -> {
                    // TODO double check default
                    ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(snapshotFiles)
                }
            }
        }
        else -> {
            // TODO double check default
            ClasspathChanges.ClasspathSnapshotDisabled
        }
    }
}