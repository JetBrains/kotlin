/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.JspecifyMarkerCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel

// Not that this diagnostic handler should be included only with `ClassicDiagnosticsHandler` and go after it
class JspecifyDiagnosticComplianceHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        val jspecifyMode = module.directives[ForeignAnnotationsDirectives.JSPECIFY_STATE].singleOrNull()
            ?: JavaTypeEnhancementState.DEFAULT_REPORT_LEVEL_FOR_JSPECIFY

        for ((testFile, ktFile) in info.allKtFiles) {
            val reportedDiagnostics =
                testServices.globalMetadataInfoHandler.getReportedMetaInfosForFile(testFile).filterIsInstance<DiagnosticCodeMetaInfo>()
            for (metaInfo in reportedDiagnostics) {
                val jspecifyMark = diagnosticsToJspecifyMarks.getValue(jspecifyMode)[metaInfo.diagnostic.factory] ?: continue
                val fileLines = ktFile.text.lines()
                val fileLinePositions =
                    fileLines.map { it.length }.runningReduce { sumLength, length -> sumLength + length + 1 }
                val lineIndexToPasteJspecifyMark = fileLinePositions.indexOfLast { it < metaInfo.start }
                val positionToPasteJspecifyMark = fileLinePositions[lineIndexToPasteJspecifyMark]
                val offset = fileLines[lineIndexToPasteJspecifyMark + 1].takeWhile { it == ' ' }.length

                testServices.globalMetadataInfoHandler.addMetadataInfosForFile(
                    testFile,
                    listOf(JspecifyMarkerCodeMetaInfo(positionToPasteJspecifyMark, positionToPasteJspecifyMark, offset, jspecifyMark))
                )
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    companion object {
        val diagnosticsToJspecifyMarks = mapOf(
            ReportLevel.WARN to mapOf(
                ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
                ErrorsJvm.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
                ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
                ErrorsJvm.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
            ),
            ReportLevel.STRICT to mapOf(
                Errors.TYPE_MISMATCH to "jspecify_nullness_mismatch",
                Errors.NULL_FOR_NONNULL_TYPE to "jspecify_nullness_mismatch",
                Errors.NOTHING_TO_OVERRIDE to "jspecify_nullness_mismatch",
                Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE to "jspecify_nullness_mismatch",
                Errors.UPPER_BOUND_VIOLATED to "jspecify_nullness_mismatch",
                Errors.UNSAFE_CALL to "jspecify_nullness_mismatch",
            ),
            ReportLevel.IGNORE to emptyMap()
        )
    }
}