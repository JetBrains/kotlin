/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderer
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class NoFirCompilationErrorsHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        var hasError = false
        val ignoreErrors = IGNORE_FIR_DIAGNOSTICS in module.directives
        for ((firFile, diagnostics) in info.firAnalyzerFacade.runCheckers()) {
            for (diagnostic in diagnostics) {
                if (diagnostic.severity == Severity.ERROR) {
                    hasError = true
                    if (!ignoreErrors) {
                        val diagnosticText = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic).hackyRender(diagnostic)
                        val range = diagnostic.textRanges.first()
                        val locationText = firFile.source?.psi?.containingFile?.let { psiFile ->
                            PsiDiagnosticUtils.atLocation(psiFile, range)
                        } ?: "${firFile.name}:$range"
                        throw IllegalStateException("${diagnostic.factory.name}: $diagnosticText at $locationText")
                    }
                }
            }
        }
        if (!hasError && ignoreErrors) {
            assertions.fail { "Test contains $IGNORE_FIR_DIAGNOSTICS directive but no errors was reported. Please remove directive" }
        }
    }

    private fun FirDiagnosticRenderer<*>.hackyRender(diagnostic: FirDiagnostic<*>): String {
        @Suppress("UNCHECKED_CAST")
        val renderer = this as FirDiagnosticRenderer<FirDiagnostic<FirSourceElement>>
        val castedDiagnostic = diagnostic as FirDiagnostic<FirSourceElement>
        return renderer.render(castedDiagnostic)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
