/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.TestStepBuilder
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.*
import org.jetbrains.kotlin.test.model.FrontendKinds

/**
 * The list of `UNUSED_*` diagnostics which are disabled by default
 * within diagnostic tests.
 */
val DEFAULT_UNUSED_DIAGNOSTICS = listOf(
    "UNUSED_VARIABLE",
    "UNUSED_PARAMETER",
    "UNUSED_ANONYMOUS_PARAMETER",
    "UNUSED_DESTRUCTURED_PARAMETER_ENTRY",
    "UNUSED_TYPEALIAS_PARAMETER",
    "UNUSED_VALUE",
    "UNUSED_CHANGED_VALUE",
    "UNUSED_EXPRESSION",
    "UNUSED_LAMBDA_EXPRESSION",
)

fun TestStepBuilder.HandlersStepBuilder.NonGroupingStage<FirOutputArtifact, FrontendKinds.FIR>.setupHandlersForDiagnosticTest() {
    useHandlers(
        ::FirDiagnosticsHandler,
        ::FirDumpHandler,
        ::FirCfgDumpHandler,
        ::FirVFirDumpHandler,
        ::FirInferenceLogsHandler,
        ::FirCfgConsistencyHandler,
        ::FirResolvedTypesVerifier,
        ::FirScopeDumpHandler,
        ::FirDistinctSourceElementsHandler,
    )
}
