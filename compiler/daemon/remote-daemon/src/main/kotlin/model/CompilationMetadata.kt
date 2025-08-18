/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationMetadataGrpc

data class CompilationMetadata(
    val projectName: String,
    val sourceFilesCount: Int,
    val dependencyFilesCount: Int,
    val compilerPluginFilesCount: Int,
    val compilerArguments: Map<String, String>,
    val compilationOptions: CompilationOptions
) : CompileRequest

fun CompilationMetadata.toGrpc(): CompilationMetadataGrpc {
    return CompilationMetadataGrpc.newBuilder()
        .setSourceFilesCount(sourceFilesCount)
        .setDependencyFilesCount(dependencyFilesCount)
        .setCompilerPluginFileCount(compilerPluginFilesCount)
        .putAllCompilerArguments(compilerArguments)
        .setProjectName(projectName)
        .setCompilationOptions(compilationOptions.toGrpc())
        .build()
}

fun CompilationMetadataGrpc.toDomain(): CompilationMetadata{
    return CompilationMetadata(
        projectName,
        sourceFilesCount,
        dependencyFilesCount,
        compilerPluginFileCount,
        compilerArgumentsMap,
        compilationOptions.toDomain()
    )
}


