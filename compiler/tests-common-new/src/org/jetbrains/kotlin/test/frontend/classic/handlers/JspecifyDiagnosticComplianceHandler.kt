/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.JspecifyMarkerCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.load.java.JSPECIFY_ANNOTATIONS_PACKAGE
import org.jetbrains.kotlin.load.java.ReportLevel
import org.jetbrains.kotlin.load.java.getDefaultReportLevelForAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCodeMetaInfo
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.sourceFileProvider

private fun TestServices.generateJspecifyMetadataInfos(
    module: TestModule, files: Iterable<TestFile>, diagnosticKind: (CodeMetaInfo) -> Any?
) {
    val jspecifyMode = module.directives[ForeignAnnotationsDirectives.JSPECIFY_STATE].singleOrNull()
        ?: getDefaultReportLevelForAnnotation(JSPECIFY_ANNOTATIONS_PACKAGE)
    val diagnosticsToJspecifyMarksForMode = diagnosticsToJspecifyMarks[jspecifyMode] ?: return

    for (testFile in files) {
        val fileLines = sourceFileProvider.getContentOfSourceFile(testFile).lines()
        val fileLinePositions =
            fileLines.map { it.length }.runningReduce { sumLength, length -> sumLength + length + 1 }
        val newMetaInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(testFile).mapNotNull { metaInfo ->
            val diagnostic = diagnosticKind(metaInfo) ?: return@mapNotNull null
            val jspecifyMark = diagnosticsToJspecifyMarksForMode[diagnostic] ?: return@mapNotNull null
            val lineIndexToPasteJspecifyMark = fileLinePositions.indexOfLast { it < metaInfo.start }
            val positionToPasteJspecifyMark = fileLinePositions[lineIndexToPasteJspecifyMark]
            val offset = fileLines[lineIndexToPasteJspecifyMark + 1].takeWhile { it == ' ' }.length
            JspecifyMarkerCodeMetaInfo(positionToPasteJspecifyMark, positionToPasteJspecifyMark, offset, jspecifyMark)
        }
        globalMetadataInfoHandler.addMetadataInfosForFile(testFile, newMetaInfos)
    }
}

internal val diagnosticsToJspecifyMarks = mapOf(
    ReportLevel.WARN to mapOf(
        ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
        ErrorsJvm.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
        ErrorsJvm.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",

        FirJvmErrors.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
        FirJvmErrors.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
        FirJvmErrors.UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS to "jspecify_nullness_mismatch",
    ),
    ReportLevel.STRICT to mapOf(
        Errors.TYPE_MISMATCH to "jspecify_nullness_mismatch",
        Errors.NULL_FOR_NONNULL_TYPE to "jspecify_nullness_mismatch",
        Errors.NOTHING_TO_OVERRIDE to "jspecify_nullness_mismatch",
        Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE to "jspecify_nullness_mismatch",
        Errors.UPPER_BOUND_VIOLATED to "jspecify_nullness_mismatch",
        Errors.UNSAFE_CALL to "jspecify_nullness_mismatch",

        FirErrors.ARGUMENT_TYPE_MISMATCH to "jspecify_nullness_mismatch",
        FirErrors.NULL_FOR_NONNULL_TYPE to "jspecify_nullness_mismatch",
        FirErrors.NOTHING_TO_OVERRIDE to "jspecify_nullness_mismatch",
        FirErrors.RETURN_TYPE_MISMATCH_ON_OVERRIDE to "jspecify_nullness_mismatch",
        FirErrors.UPPER_BOUND_VIOLATED to "jspecify_nullness_mismatch",
        FirErrors.UNSAFE_CALL to "jspecify_nullness_mismatch",
    ),
    ReportLevel.IGNORE to emptyMap()
)

class JspecifyDiagnosticComplianceHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) =
        testServices.generateJspecifyMetadataInfos(module, info.allKtFiles.keys) {
            (it as? DiagnosticCodeMetaInfo)?.diagnostic?.factory
        }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

class FirJspecifyDiagnosticComplianceHandler(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) =
        testServices.generateJspecifyMetadataInfos(module, info.allFirFiles.keys) {
            (it as? FirDiagnosticCodeMetaInfo)?.diagnostic?.factory
        }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
