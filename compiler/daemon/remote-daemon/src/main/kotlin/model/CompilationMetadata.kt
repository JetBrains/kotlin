/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationMetadataProto

@Serializable
data class CompilationMetadata(
    val projectName: String,
    val sourceFilesCount: Int,
    val dependencyFilesCount: Int,
    val compilerPluginFilesCount: Int,
    val compilerArguments: List<String>,
    @Contextual
    val compilationOptions: CompilationOptions
) : CompileRequest

fun CompilationMetadata.toProto(): CompilationMetadataProto {
    return CompilationMetadataProto.newBuilder()
        .setSourceFilesCount(sourceFilesCount)
        .setDependencyFilesCount(dependencyFilesCount)
        .setCompilerPluginFileCount(compilerPluginFilesCount)
        .addAllCompilerArguments(compilerArguments)
        .setProjectName(projectName)
        .setCompilationOptions(compilationOptions.toProto())
        .build()
}

fun CompilationMetadataProto.toDomain(): CompilationMetadata {
    return CompilationMetadata(
        projectName,
        sourceFilesCount,
        dependencyFilesCount,
        compilerPluginFileCount,
        compilerArgumentsList,
        compilationOptions.toDomain()
    )
}


