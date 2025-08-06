/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import common.toDomain
import common.toGrpc
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationOptionsGrpc

fun CompilationOptions.toGrpc(): CompilationOptionsGrpc {
    return CompilationOptionsGrpc.newBuilder()
        .setCompilerMode(this.compilerMode.toGrpc())
        .setTargetPlatform(this.targetPlatform.toGrpc())
        .setReportSeverity(this.reportSeverity)
        .build()
}

fun CompilationOptionsGrpc.toDomain(): CompilationOptions {
    return CompilationOptions(
        compilerMode = compilerMode.toDomain(),
        targetPlatform = targetPlatform.toDomain(),
        reportSeverity = reportSeverity,
        requestedCompilationResults = requestedCompilationResultsList.toTypedArray(),
        reportCategories = reportCategoriesList.toTypedArray(),
        kotlinScriptExtensions = kotlinScriptExtensionsList.toTypedArray()
    )
}