/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.TestStepBuilder
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.FrontendKinds

/**
 * Adds a handler which checks that there are no compilation errors reported at the K2 frontend step
 */
fun TestStepBuilder.HandlersStepBuilder.NonGroupingStage<FirOutputArtifact, FrontendKinds.FIR>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

fun TestStepBuilder.HandlersStepBuilder.NonGroupingStage<IrBackendInput, BackendKinds.IrBackend>.commonIrHandlersForCodegenTest() {
    useHandlers(
        ::NoIrCompilationErrorsHandler,
    )
}

fun TestStepBuilder.HandlersStepBuilder.NonGroupingStage<IrBackendInput, BackendKinds.IrBackend>.commonLoweredIrHandlersForCodegenTest() {
    useHandlers(
        ::NoIrCompilationErrorsHandler,
    )
}
