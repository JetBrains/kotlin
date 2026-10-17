/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorServiceIfRegistered
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Releases the state of the first compilation stage of a test once the stage is over, for a grouping stage which needs
 * only the KLIBs of the test. A test waits for the rest of its batch before its grouping stage, and otherwise keeps its whole
 * FIR session alive, with the metadata of its library dependencies deserialized anew for every test (~10 MB for the stdlib),
 * which limits how many tests can be in flight.
 */
class KlibOnlyFirstStageStateReleaser(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(thereWereFailures: Boolean) {
        // A failed test is disposed right away anyway.
        if (thereWereFailures) return
        testServices.artifactsProvider.retainOnly(setOf(ArtifactKinds.KLib))
        testServices.firDiagnosticCollectorServiceIfRegistered?.clearCache()
        // The meta-infos have already been compared: the checkers run after that.
        testServices.globalMetadataInfoHandler.clearReportedMetaInfos()
        for (module in testServices.moduleStructure.modules) {
            // A reported diagnostic refers to the context of its checker, and so to the whole FIR session.
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
                .diagnosticsCollector = DiagnosticsCollectorImpl()
        }
    }
}
