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