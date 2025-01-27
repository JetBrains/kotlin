/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.KmpCompilationMode
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service

class NoFirCompilationErrorsHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = true,
) : FirAnalysisHandler(testServices, failureDisablesNextSteps) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives)

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirDiagnosticCollectorService))

    private val seenModules = mutableSetOf<TestModule>()

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            seenModules.add(part.module)

            val ignoreErrors = IGNORE_FIR_DIAGNOSTICS in part.module.directives

            val diagnosticsPerFile = testServices.firDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)
            for ((firFile, diagnostics) in diagnosticsPerFile) {
                for ((diagnostic, mode) in diagnostics) {
                    if (mode == KmpCompilationMode.METADATA) continue
                    if (diagnostic.severity == Severity.ERROR) {
                        if (!ignoreErrors) {
                            val diagnosticText = RootDiagnosticRendererFactory(diagnostic).render(diagnostic)
                            if (firFile != null) {
                                val range = diagnostic.textRanges.first()
                                val locationText = firFile.source?.psi?.containingFile?.let { psiFile ->
                                    PsiDiagnosticUtils.atLocation(psiFile, range)
                                } ?: "${firFile.name}:$range"
                                error("${diagnostic.factory.name}: $diagnosticText at $locationText")
                            } else {
                                error(diagnostic.factory.name)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        // The `IGNORE_FIR_DIAGNOSTICS` directive is global and could have been used for
        // a module that we haven't yet analyzed.
        // See: `compiler/testData/diagnostics/tests/multiplatform/topLevelFun/inlineFun.kt`

        val ignoreErrors = IGNORE_FIR_DIAGNOSTICS in testServices.moduleStructure.allDirectives
        val hasError = testServices.firDiagnosticCollectorService.containsErrorDiagnostics

        if (!hasError && ignoreErrors) {
            assertions.fail { "Test contains $IGNORE_FIR_DIAGNOSTICS directive but no errors was reported. Please remove directive" }
        }
    }
}
