/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.FirDiagnosticToKaDiagnosticConverterRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KaDiagnosticClassImplementationRenderer
import org.jetbrains.kotlin.analysis.api.fir.generator.rendererrs.KaDiagnosticClassRenderer
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.generators.util.getGenerationPath
import java.nio.file.Path

object DiagnosticClassGenerator {
    fun generate(rootPath: Path, diagnosticList: DiagnosticList, packageName: String, target: DiagnosticGenerationTarget) {
        val path = getGenerationPath(rootPath.toFile(), packageName)
        when (target) {
            DiagnosticGenerationTarget.Api -> {
                KaDiagnosticClassRenderer.render(
                    file = path.resolve("KaFirDiagnostic.kt"),
                    diagnosticList = diagnosticList,
                    packageName = packageName,
                    starImportsToAdd = emptySet(),
                )
            }

            DiagnosticGenerationTarget.Implementation -> {
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
    }
}

/**
 * The part of the diagnostic hierarchy to generate. Diagnostics are split between two modules, so each of them requests its own part.
 */
enum class DiagnosticGenerationTarget(val id: String) {
    /** The public `KaFirDiagnostic` interfaces, generated into `analysis-api-fir-diagnostics`. */
    Api("api"),

    /** The diagnostic implementations and their converters, generated into `analysis-api-fir`. */
    Implementation("implementation"),
    ;

    companion object {
        fun findById(id: String): DiagnosticGenerationTarget? = entries.find { it.id == id }
    }
}
