/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DiagnosticMessagesTextHandler(
    testServices: TestServices
) : ClassicFrontendAnalysisHandler(testServices) {

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives)

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val resultDump = dumper.generateResultingDump()
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.withExtension(".diag.txt")
        assertions.assertEqualsToFile(expectedFile, resultDump)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        if (DiagnosticsDirectives.RENDER_DIAGNOSTICS_FULL_TEXT !in module.directives) return

        val diagnosticsFullTextByteArrayStream = ByteArrayOutputStream()
        val diagnosticsFullTextPrintStream = PrintStream(diagnosticsFullTextByteArrayStream)
        val diagnosticsFullTextCollector =
            GroupingMessageCollector(
                PrintingMessageCollector(diagnosticsFullTextPrintStream, MessageRenderer.SYSTEM_INDEPENDENT_RELATIVE_PATHS, true),
                false
            )

        AnalyzerWithCompilerReport.reportDiagnostics(
            info.analysisResult.bindingContext.diagnostics,
            diagnosticsFullTextCollector,
            renderInternalDiagnosticName = false
        )

        diagnosticsFullTextCollector.flush()
        diagnosticsFullTextPrintStream.flush()
        dumper.builderForModule(module).appendLine(String(diagnosticsFullTextByteArrayStream.toByteArray()))
    }

}
