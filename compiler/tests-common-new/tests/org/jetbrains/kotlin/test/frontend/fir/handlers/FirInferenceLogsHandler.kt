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
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.inferenceLogsFormats
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.inferencelogs.FixationOnlyInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.FirInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.MarkdownInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.inferencelogs.MermaidInferenceLogsDumper
import org.jetbrains.kotlin.test.utils.originalTestDataFile
import org.jetbrains.kotlin.test.utils.withExtension
import java.io.File

class FirInferenceLogsHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    private val inferenceLoggers = mutableMapOf<FirSession, FirInferenceLogger>()

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in testServices.moduleStructure.allDirectives) return

        for (part in info.partsForDependsOnModules) {
            inferenceLoggers[part.session] = part.session.inferenceLogger ?: continue
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        ensureNoStrayDumps()
        if (FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in testServices.moduleStructure.allDirectives || inferenceLoggers.isEmpty()) return

        testServices.assertions.assertAll(
            testServices.moduleStructure.allDirectives.inferenceLogsFormats.map { format ->
                val dumper = format.dumper
                val dumpFile = format.file
                { testServices.assertions.assertEqualsToFile(dumpFile, dumper.renderDump(inferenceLoggers)) }
            }
        )
    }

    private fun ensureNoStrayDumps() {
        val allowedFormats = testServices.moduleStructure.allDirectives.inferenceLogsFormats

        for (format in InferenceLogsFormat.entries) {
            if (format !in allowedFormats) {
                testServices.assertions.assertFileDoesntExist(format.file) { "`$format` dump file detected but '${FirDiagnosticsDirectives.DUMP_INFERENCE_LOGS}' is not set to emit it." }
            }
        }
    }

    private val InferenceLogsFormat.file: File
        get() {
            // K1 doesn't support constraint dumps, no need to care about ".fir.inference.md"
            val originalFile = testServices.moduleStructure.originalTestDataFiles.first().originalTestDataFile
            val additionalExtension = if (testServices.moduleStructure.allDirectives.contains(USE_LATEST_LANGUAGE_VERSION)) {
                ".latestLV"
            } else {
                ""
            }
            val dumpFile = originalFile.withExtension("$additionalExtension$fileExtension")
            return dumpFile
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
