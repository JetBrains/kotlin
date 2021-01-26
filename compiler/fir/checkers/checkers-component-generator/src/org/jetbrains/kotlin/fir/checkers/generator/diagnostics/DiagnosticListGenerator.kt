/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.fir.checkers.generator.getGenerationPath
import java.io.File

fun generateDiagnostics(rootPath: File, packageName: String, diagnosticList: DiagnosticList) {
    val generationPath = getGenerationPath(rootPath, packageName)
    ErrorListDiagnosticListRenderer.render(generationPath.resolve("FirErrors.kt"), diagnosticList, packageName)
}
