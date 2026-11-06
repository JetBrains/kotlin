/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.test.TestStepBuilder
import org.jetbrains.kotlin.test.backend.handlers.IrPreprocessedInlineFunctionDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureLoweredIrHandlersStep
import org.jetbrains.kotlin.test.builders.loweredIrHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
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

/**
 * Adds IR dump handlers for lowered IR: the IR text dump after inlining and the dump of preprocessed inline functions.
 *
 * The "lowered IR handlers" step must already exist, see [loweredIrHandlersStep].
 */
fun TestConfigurationBuilder.configureLoweredIrDumpHandlers() {
    configureLoweredIrHandlersStep {
        useHandlers(
            { testServices, artifactKind ->
                IrTextDumpHandler(
                    testServices = testServices,
                    artifactKind = artifactKind,
                    customExtension = "inlined.ir",
                    directive = CodegenTestDirectives.DUMP_IR_AFTER_INLINE,
                    directiveForIrDifference = CodegenTestDirectives.DUMP_IR_AFTER_INLINE_DIFFERENCE,
                    showOffsets = true,
                )
            },
            ::IrPreprocessedInlineFunctionDumpHandler,
        )
    }
}
