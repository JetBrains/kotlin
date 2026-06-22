/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.config.InferenceLogsFormat
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceLogger
import org.jetbrains.kotlin.fir.resolve.inference.inferenceLogger
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.inferenceLogsFormats
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.inferencelogs.FirInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.FixationOnlyInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.MarkdownInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.MermaidInferenceLogsDumper
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirInferenceLogsHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, FirDiagnosticsDirectives)

    private val inferenceLoggers = mutableMapOf<FirSession, FirInferenceLogger>()

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in testServices.moduleStructure.allDirectives) return

        for (part in info.partsForDependsOnModules) {
            inferenceLoggers[part.session] = part.session.inferenceLogger ?: continue
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in testServices.moduleStructure.allDirectives || inferenceLoggers.isEmpty()) return

        val enabledFormats = testServices.moduleStructure.allDirectives.inferenceLogsFormats
        testServices.assertions.assertAll(
            InferenceLogsFormat.entries.map { format ->
                return@map {
                    val renderedDump = runIf(format in enabledFormats) { format.dumper.renderDump(inferenceLoggers) }
                    assertEqualsToDump(format.fileExtension, renderedDump)
                }
            }
        )
    }

    private val InferenceLogsFormat.fileExtension: String
        get() = when (this) {
            InferenceLogsFormat.MARKDOWN -> ".inference.md"
            InferenceLogsFormat.MERMAID -> ".inference.mmd"
            InferenceLogsFormat.FIXATION -> ".fixation.txt"
        }

    private val InferenceLogsFormat.dumper: FirInferenceLogsDumper
        get() = when (this) {
            InferenceLogsFormat.MARKDOWN -> MarkdownInferenceLogsDumper()
            InferenceLogsFormat.MERMAID -> MermaidInferenceLogsDumper()
            InferenceLogsFormat.FIXATION -> FixationOnlyInferenceLogsDumper()
        }
}
