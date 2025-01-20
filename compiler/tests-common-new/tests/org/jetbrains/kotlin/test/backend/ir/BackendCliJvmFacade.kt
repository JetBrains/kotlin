/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedJvmOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.reportErrorFromCliPhase
import org.jetbrains.kotlin.test.services.TestServices

class BackendCliJvmFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState? {
        require(inputArtifact is Fir2IrCliBasedJvmOutputArtifact) {
            "BackendCliJvmFacade expects Fir2IrCliBasedJvmOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val input = inputArtifact.cliArtifact.copy(
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(
                inputArtifact.cliArtifact.configuration.messageCollector
            )
        )
        val output = JvmBackendPipelinePhase.executePhase(input, ignoreErrors = true) ?: reportErrorFromCliPhase()
        return output.outputs.single()
    }

    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = (this as Fir2IrCliBasedJvmOutputArtifact).cliArtifact.sourceFiles
}
