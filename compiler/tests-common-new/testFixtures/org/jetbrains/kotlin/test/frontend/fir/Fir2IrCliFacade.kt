/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class Fir2IrCliFacade<Phase, InputPipelineArtifact, OutputPipelineArtifact>(
    testServices: TestServices,
    private val phase: Phase,
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend,
) where Phase : PipelinePhase<InputPipelineArtifact, OutputPipelineArtifact>,
        InputPipelineArtifact : FrontendPipelineArtifact,
        OutputPipelineArtifact : Fir2IrPipelineArtifact {

    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact,
    ): IrBackendInput? {
        require(inputArtifact is FirCliBasedOutputArtifact<*>) {
            "${this::class} expects FirCliBasedOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        @Suppress("UNCHECKED_CAST")
        val cliArtifact = inputArtifact.cliArtifact as InputPipelineArtifact
        val input = cliArtifact.withNewDiagnosticCollector(
            DiagnosticsCollectorImpl()
        )
        val output = phase.executePhase(input)
            ?: return processErrorFromCliPhase(cliArtifact.configuration, testServices)
        return Fir2IrCliBasedOutputArtifact(output)
    }
}

class Fir2IrCliBasedOutputArtifact<A : Fir2IrPipelineArtifact>(val cliArtifact: A) : IrBackendInput() {
    override val irModuleFragment: org.jetbrains.kotlin.ir.declarations.IrModuleFragment
        get() = cliArtifact.result.irModuleFragment
    override val irBuiltIns: IrBuiltIns
        get() = cliArtifact.result.irBuiltIns
    override val descriptorMangler: KotlinMangler.DescriptorMangler?
        get() = null
    override val irMangler: KotlinMangler.IrMangler
        get() = cliArtifact.result.components.irMangler
    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = cliArtifact.configuration.diagnosticsCollector
}
