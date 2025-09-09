/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import common.toDomain
import common.toProto
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.daemon.common.MultiModuleICSettings
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.server.ClasspathChangesProto
import org.jetbrains.kotlin.server.ClasspathSnapshotFilesProto
import org.jetbrains.kotlin.server.CompilationOptionsProto
import org.jetbrains.kotlin.server.IncrementalCompilationFeaturesProto
import org.jetbrains.kotlin.server.IncrementalCompilationOptionsProto
import org.jetbrains.kotlin.server.IncrementalModuleEntryProto
import org.jetbrains.kotlin.server.IncrementalModuleEntrySetProto
import org.jetbrains.kotlin.server.IncrementalModuleInfoProto
import org.jetbrains.kotlin.server.KnownProto
import org.jetbrains.kotlin.server.MultiModuleICSettingsProto
import org.jetbrains.kotlin.server.SourcesChangesProto
import org.jetbrains.kotlin.server.ToBeCalculatedProto
import org.jetbrains.kotlin.server.UnknownProto
import java.io.File

fun CompilationOptions.toProto(): CompilationOptionsProto {
    return CompilationOptionsProto.newBuilder()
        .setCompilerMode(this.compilerMode.toProto())
        .setTargetPlatform(this.targetPlatform.toProto())
        .setReportSeverity(this.reportSeverity)
        .build()
}

fun CompilationOptionsProto.toDomain(): CompilationOptions {
    return CompilationOptions(
        compilerMode = compilerMode.toDomain(),
        targetPlatform = targetPlatform.toDomain(),
        reportSeverity = reportSeverity,
        requestedCompilationResults = requestedCompilationResultsList.toTypedArray(),
        reportCategories = reportCategoriesList.toTypedArray(),
        kotlinScriptExtensions = kotlinScriptExtensionsList.toTypedArray()
    )
}

fun SourcesChanges.toProto(): SourcesChangesProto {
    return SourcesChangesProto.newBuilder()
        .apply {
            when (this@toProto) {
                is SourcesChanges.Unknown -> setUnknown(UnknownProto.getDefaultInstance())
                is SourcesChanges.ToBeCalculated -> setToBeCalculated(ToBeCalculatedProto.getDefaultInstance())
                is SourcesChanges.Known -> setKnown(
                    KnownProto.newBuilder()
                        .addAllModifiedFiles(this@toProto.modifiedFiles.map { it.absolutePath })
                        .addAllRemovedFiles(this@toProto.removedFiles.map { it.absolutePath })
                        .build()
                )
            }
        }
        .build()
}


fun SourcesChangesProto.toDomain(): SourcesChanges {
    return when (kindCase) {
        SourcesChangesProto.KindCase.UNKNOWN -> SourcesChanges.Unknown
        SourcesChangesProto.KindCase.TO_BE_CALCULATED -> SourcesChanges.ToBeCalculated
        SourcesChangesProto.KindCase.KNOWN -> SourcesChanges.Known(
            modifiedFiles = known.modifiedFilesList.map { File(it) },
            removedFiles = known.removedFilesList.map { File(it) }
        )
        SourcesChangesProto.KindCase.KIND_NOT_SET -> SourcesChanges.Unknown
        null -> SourcesChanges.Unknown
    }
}

fun ClasspathChanges.toProto(): ClasspathChangesProto {
    return ClasspathChangesProto.newBuilder()
        .apply {
            when (this@toProto) {
                is ClasspathChanges.ClasspathSnapshotEnabled -> {
                    val classpathSnapshotEnabledBuilder = ClasspathChangesProto.ClasspathSnapshotEnabledProto.newBuilder()
                        .setClasspathSnapshotFiles(
                            ClasspathSnapshotFilesProto.newBuilder()
                                .addAllCurrentClasspathEntrySnapshotFiles(this@toProto.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map { it.absolutePath })
                                .setShrunkPreviousClasspathSnapshotFile(
                                    this@toProto.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.absolutePath
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
                classpathSnapshotDir = File(filesProto.shrunkPreviousClasspathSnapshotFile) // TODO this is wrong
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


fun MultiModuleICSettings.toProto(): MultiModuleICSettingsProto {
    return MultiModuleICSettingsProto.newBuilder()
        .setBuildHistoryFile(buildHistoryFile.absolutePath)
        .setUseModuleDetection(useModuleDetection)
        .build()
}

fun MultiModuleICSettingsProto.toDomain(): MultiModuleICSettings {
    return MultiModuleICSettings(
        File(buildHistoryFile),
        useModuleDetection
    )
}

// TODO fix this project path
fun IncrementalModuleEntry.toProto(): IncrementalModuleEntryProto {
    return IncrementalModuleEntryProto.newBuilder()
        .setProjectPath("todo_project_path")
        .setName(name)
        .setBuildDir(buildDir.absolutePath)
        .setBuildHistoryFile(buildHistoryFile.absolutePath)
        .setAbiSnapshot(abiSnapshot.absolutePath)
        .build()
}

fun IncrementalModuleEntryProto.toDomain(): IncrementalModuleEntry {
    return IncrementalModuleEntry(
        projectPath,
        name,
        File(buildDir),
        File(buildHistoryFile),
        File(abiSnapshot)
    )
}

fun IncrementalModuleInfo.toProto(): IncrementalModuleInfoProto {
    return IncrementalModuleInfoProto.newBuilder()
        .setRootProjectBuildDir(rootProjectBuildDir.absolutePath)
        .putAllDirToModule(
            dirToModule.mapKeys { it.key.absolutePath }
                .mapValues { it.value.toProto() }
        )
        .putAllNameToModules(
            nameToModules.mapValues { (_, moduleSet) ->
                IncrementalModuleEntrySetProto.newBuilder()
                    .addAllEntries(moduleSet.map { it.toProto() })
                    .build()
            }
        )
        .putAllJarToClassListFile(
            jarToClassListFile.mapKeys { it.key.absolutePath }
                .mapValues { it.value.absolutePath }
        )
        .putAllJarToModule(
            jarToModule.mapKeys { it.key.absolutePath }
                .mapValues { it.value.toProto() }
        )
        .putAllJarToAbiSnapshot(
            jarToAbiSnapshot.mapKeys { it.key.absolutePath }
                .mapValues { it.value.absolutePath }
        )
        .build()
}

fun IncrementalModuleInfoProto.toDomain(): IncrementalModuleInfo {
    return IncrementalModuleInfo(
        rootProjectBuildDir = File(rootProjectBuildDir),
        dirToModule = dirToModuleMap.mapKeys { File(it.key) }
            .mapValues { it.value.toDomain() },
        nameToModules = nameToModulesMap.mapValues { (_, moduleSetProto) ->
            moduleSetProto.entriesList.map { it.toDomain() }.toSet()
        },
        jarToClassListFile = jarToClassListFileMap.mapKeys { File(it.key) }
            .mapValues { File(it.value) },
        jarToModule = jarToModuleMap.mapKeys { File(it.key) }
            .mapValues { it.value.toDomain() },
        jarToAbiSnapshot = jarToAbiSnapshotMap.mapKeys { File(it.key) }
            .mapValues { File(it.value) }
    )
}

fun IncrementalCompilationFeatures.toProto(): IncrementalCompilationFeaturesProto {
    return IncrementalCompilationFeaturesProto.newBuilder()
        .setUsePreciseJavaTracking(usePreciseJavaTracking)
        .setWithAbiSnapshot(withAbiSnapshot)
        .setPreciseCompilationResultsBackup(preciseCompilationResultsBackup)
        .setKeepIncrementalCompilationCachesInMemory(keepIncrementalCompilationCachesInMemory)
        .setEnableUnsafeIncrementalCompilationForMultiplatform(enableUnsafeIncrementalCompilationForMultiplatform)
        .setEnableMonotonousIncrementalCompileSetExpansion(enableMonotonousIncrementalCompileSetExpansion)
        .build()
}

fun IncrementalCompilationFeaturesProto.toDomain(): IncrementalCompilationFeatures {
    return IncrementalCompilationFeatures(
        usePreciseJavaTracking,
        withAbiSnapshot,
        preciseCompilationResultsBackup,
        keepIncrementalCompilationCachesInMemory,
        enableUnsafeIncrementalCompilationForMultiplatform,
        enableMonotonousIncrementalCompileSetExpansion
    )
}

fun IncrementalCompilationOptions.toProto(): IncrementalCompilationOptionsProto {
    return IncrementalCompilationOptionsProto.newBuilder()
        .setSourceChanges(sourceChanges.toProto())
        .setClasspathChanges(classpathChanges.toProto())
        .setWorkingDir(workingDir.absolutePath)
        .setCompilationOptions(
            CompilationOptions(
                compilerMode,
                targetPlatform,
                reportCategories,
                reportSeverity,
                requestedCompilationResults,
                kotlinScriptExtensions,
            ).toProto()
        )
        .setUseJvmFirRunner(useJvmFirRunner)
        .addAllOutputFiles((outputFiles ?: emptyList()).map { it.absolutePath })
        .apply {
            this@toProto.multiModuleICSettings?.toProto()?.let { setMultiModuleIcSettings(it) }
        }
        .apply {
            this@toProto.modulesInfo?.toProto()?.let { setModulesInfo(it) }
            this@toProto.rootProjectDir?.absolutePath?.let { setRootProjectDir(it) }
            this@toProto.buildDir?.absolutePath?.let { setBuildDir(it) }
        }
        .setIcFeatures(icFeatures.toProto())
        .build()
}


fun IncrementalCompilationOptionsProto.toDomain(): IncrementalCompilationOptions {
    return IncrementalCompilationOptions(
        sourceChanges.toDomain(),
        classpathChanges.toDomain(),
        File(workingDir),
        compilationOptions.compilerMode.toDomain(),
        compilationOptions.targetPlatform.toDomain(),
        compilationOptions.reportCategoriesList.toTypedArray(),
        compilationOptions.reportSeverity,
        compilationOptions.requestedCompilationResultsList.toTypedArray(),
        useJvmFirRunner,
        outputFilesList.map { File(it) },
        multiModuleIcSettings?.toDomain(),
        modulesInfo?.toDomain(),
        rootProjectDir?.let { File(it) },
        buildDir?.let { File(it) },
        compilationOptions.kotlinScriptExtensionsList.toTypedArray(),
        icFeatures.toDomain()
    )
}