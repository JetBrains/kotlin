/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCodegenPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.services.TestServices

class CodegenCliJvmFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState {
        checkTestInfrastructure(inputArtifact is LoweredJvmCliBasedOutputArtifact) {
            "BackendCliJvmFacade expects LoweredJvmCliBasedOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val input = inputArtifact.createCliArtifactWithNewDiagnosticCollector(DiagnosticsCollectorImpl())
        val output = JvmCodegenPipelinePhase.executePhase(input).let(JvmWriteOutputsPhase::executePhase)
        return output.outputs.single()
    }

    @Suppress("UNCHECKED_CAST")
    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = (this as LoweredJvmCliBasedOutputArtifact).cliArtifact.sourceFiles
}
