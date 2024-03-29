/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.test.DisableNextStepException
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.frontend.fir.handlers.FullDiagnosticsRenderer
import org.jetbrains.kotlin.test.frontend.fir.handlers.diagnosticCodeMetaInfos
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class IrDiagnosticsHandler(testServices: TestServices) : AbstractIrHandler(testServices, failureDisablesNextSteps = true) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::DiagnosticsService))

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_IR_DIAGNOSTICS_FULL_TEXT)

    override fun processModule(module: TestModule, info: IrBackendInput) {
        val collectedMetadataInfos = mutableListOf<FirDiagnosticCodeMetaInfo>()
        val diagnosticsByFilePath = info.diagnosticReporter.diagnosticsByFilePath
        for (currentModule in testServices.moduleStructure.modules) {
            val lightTreeComparingModeEnabled = FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in currentModule.directives
            val lightTreeEnabled = currentModule.directives.singleOrZeroValue(FirDiagnosticsDirectives.FIR_PARSER) == FirParser.LightTree
            for (file in currentModule.files) {
                val diagnostics = diagnosticsByFilePath["/" + file.relativePath]
                if (diagnostics != null && diagnostics.isNotEmpty()) {
                    val diagnosticsMetadataInfos =
                        diagnostics.diagnosticCodeMetaInfos(
                            module, file, diagnosticsService, globalMetadataInfoHandler,
                            lightTreeEnabled, lightTreeComparingModeEnabled
                        ).also { collectedMetadataInfos += it }
                    globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
                    fullDiagnosticsRenderer.storeFullDiagnosticRender(module, diagnostics, file)
                }
            }
        }
        val errorMetadataInfos = collectedMetadataInfos.filter { it.diagnostic.severity == Severity.ERROR}
        if (errorMetadataInfos.isNotEmpty())
            throw DisableNextStepException("There were FIR2IR error diagnostics: ${errorMetadataInfos.map { it.diagnostic.factory }}")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".fir.ir.diag.txt")
    }
}