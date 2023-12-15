/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.FirDiagnosticToKtDiagnosticConverterRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KtDiagnosticClassImplementationRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KtDiagnosticClassRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import java.io.File

object DiagnosticClassGenerator {
    fun generate(path: File, diagnosticList: DiagnosticList, packageName: String) {
        KtDiagnosticClassRenderer.render(path.resolve("KtFirDiagnostics.kt"), diagnosticList, packageName, emptySet())
        KtDiagnosticClassImplementationRenderer.render(path.resolve("KtFirDiagnosticsImpl.kt"), diagnosticList, packageName, emptySet())
        FirDiagnosticToKtDiagnosticConverterRenderer.render(
            path.resolve("KtFirDataClassConverters.kt"),
            diagnosticList,
            packageName,
            emptySet()
        )
        ArgumentsConverterGenerator.render(path.resolve("KtFirArgumentsConverter.kt"), packageName)
    }
}
