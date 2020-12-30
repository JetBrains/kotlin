/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumperImpl

class FirDumpHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumperImpl()

    override val directivesContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (FirDiagnosticsDirectives.FIR_DUMP !in module.directives) return
        val builderForModule = dumper.builderForModule(module)
        val firFiles = info.firFiles
        firFiles.values.forEach { builderForModule.append(it.render()) }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        // TODO: change according to multiple testdata files
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}.fir.txt")
        val actualText = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
    }
}
