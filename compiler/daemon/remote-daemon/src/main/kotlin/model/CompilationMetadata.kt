/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationMetadataGrpc

data class CompilationMetadata(
    val projectName: String,
    val fileCount: Int,
    val compilerArguments: List<String>,
    val compilationOptions: CompilationOptions
)


fun CompilationMetadata.toGrpc(): CompilationMetadataGrpc{

    val builder = CompilationMetadataGrpc.newBuilder()

    compilerArguments.forEachIndexed { i, arg ->
        builder.setCompilerArguments(i, arg)
    }

    return builder
        .setFileCount(fileCount)
        .setProjectName(projectName)
        .setCompilationOptions(compilationOptions.toGrpc())
        .build()
}

fun CompilationMetadataGrpc.toDomain(): CompilationMetadata{
    return CompilationMetadata(
        projectName,
        fileCount,
        compilerArgumentsList,
        compilationOptions.toDomain()
    )
}


