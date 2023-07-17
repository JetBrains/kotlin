/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.FullDiagnosticsRenderer
import org.jetbrains.kotlin.test.frontend.fir.handlers.diagnosticCodeMetaInfos
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class IrDiagnosticsHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    private val diagnosticsService: DiagnosticsService
        get() = testServices.diagnosticsService

    private val fullDiagnosticsRenderer = FullDiagnosticsRenderer(DiagnosticsDirectives.RENDER_IR_DIAGNOSTICS_FULL_TEXT)

    override fun processModule(module: TestModule, info: IrBackendInput) {
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
                        )
                    globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnosticsMetadataInfos)
                    fullDiagnosticsRenderer.storeFullDiagnosticRender(module, diagnostics, file)
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fullDiagnosticsRenderer.assertCollectedDiagnostics(testServices, ".fir.ir.diag.txt")
    }
}