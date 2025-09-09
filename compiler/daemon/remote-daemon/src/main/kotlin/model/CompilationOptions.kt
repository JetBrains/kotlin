/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import common.toDomain
import common.toProto
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.server.CompilationOptionsProto

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

