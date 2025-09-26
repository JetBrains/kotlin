/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import common.toDomain
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.server.IncrementalCompilationOptionsProto
import java.io.File

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