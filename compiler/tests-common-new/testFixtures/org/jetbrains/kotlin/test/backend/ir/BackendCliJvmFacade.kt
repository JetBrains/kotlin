/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.services.TestServices

class BackendCliJvmFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState? {
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "BackendCliJvmFacade expects Fir2IrCliBasedJvmOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        require(inputArtifact.cliArtifact is JvmFir2IrPipelineArtifact) {
            "BackendCliJvmFacade expects JvmFir2IrPipelineArtifact as input, but ${inputArtifact.cliArtifact::class} was found"
        }
        val messageCollector = inputArtifact.cliArtifact.configuration.messageCollector
        val input = inputArtifact.cliArtifact.copy(
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter()
        )
        val output = JvmBackendPipelinePhase.executePhase(input)?.let(JvmWriteOutputsPhase::executePhase)
            ?: return processErrorFromCliPhase(messageCollector, testServices)
        return output.outputs.single()
    }

    @Suppress("UNCHECKED_CAST")
    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = (this as Fir2IrCliBasedOutputArtifact<JvmFir2IrPipelineArtifact>).cliArtifact.sourceFiles
}
