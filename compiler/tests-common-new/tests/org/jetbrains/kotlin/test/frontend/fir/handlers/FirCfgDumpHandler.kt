/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FirControlFlowGraphRenderVisitor
import org.jetbrains.kotlin.test.directives.DumpCfgOption
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

// TODO: adapt to multifile and multimodule tests
class FirCfgDumpHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private val builder = StringBuilder()
    private var alreadyDumped: Boolean = false

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (alreadyDumped || FirDiagnosticsDirectives.DUMP_CFG !in module.directives) return
        val options = module.directives[FirDiagnosticsDirectives.DUMP_CFG].map { it.uppercase() }

        val file = info.mainFirFiles.values.first()
        val renderLevels = DumpCfgOption.LEVELS in options
        val renderFlow = DumpCfgOption.FLOW in options
        file.accept(FirControlFlowGraphRenderVisitor(builder, renderLevels, renderFlow))
        alreadyDumped = true
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!alreadyDumped) return
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}.dot")
        assertions.assertEqualsToFile(expectedFile, builder.toString())
    }
}
