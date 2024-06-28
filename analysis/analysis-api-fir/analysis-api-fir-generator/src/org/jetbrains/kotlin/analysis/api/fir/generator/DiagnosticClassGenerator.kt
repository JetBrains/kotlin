/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.FirDiagnosticToKaDiagnosticConverterRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KaDiagnosticClassImplementationRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KaDiagnosticClassRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.getGenerationPath
import java.nio.file.Path

object DiagnosticClassGenerator {
    fun generate(rootPath: Path, diagnosticList: DiagnosticList, packageName: String) {
        val path = getGenerationPath(rootPath.toFile(), packageName)
        KaDiagnosticClassRenderer.render(
            file = path.resolve("KaFirDiagnostics.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        KaDiagnosticClassImplementationRenderer.render(
            file = path.resolve("KaFirDiagnosticsImpl.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        FirDiagnosticToKaDiagnosticConverterRenderer.render(
            file = path.resolve("KaFirDataClassConverters.kt"),
            diagnosticList = diagnosticList,
            packageName = packageName,
            starImportsToAdd = emptySet(),
        )

        ArgumentsConverterGenerator.render(
            file = path.resolve("KaFirArgumentsConverter.kt"),
            packageName = packageName,
        )
    }
}
