/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.server.CompilationMetadataProto

@Serializable
data class CompilationMetadata(
    val projectName: String,
    val totalFilesToSend: Int,
    val compilerArguments: List<String>,
    @Contextual
    val compilationOptions: CompilationOptions
) : CompileRequest

fun CompilationMetadata.toProto(): CompilationMetadataProto {
    return CompilationMetadataProto.newBuilder()
        .setTotalFilesToSend(totalFilesToSend)
        .addAllCompilerArguments(compilerArguments)
        .setProjectName(projectName)
        .apply {
            when (compilationOptions) {
                is IncrementalCompilationOptions -> setIncrementalCompilationOptions(compilationOptions.toProto())
                is CompilationOptions -> setStandardCompilationOptions(compilationOptions.toProto())
            }
        }
        .build()
}

fun CompilationMetadataProto.toDomain(): CompilationMetadata {
    return CompilationMetadata(
        projectName,
        totalFilesToSend,
        compilerArgumentsList,
        if (hasIncrementalCompilationOptions()) incrementalCompilationOptions.toDomain()
        else standardCompilationOptions.toDomain()
    )
}


