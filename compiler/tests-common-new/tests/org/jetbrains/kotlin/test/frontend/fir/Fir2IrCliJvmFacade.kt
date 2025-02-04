/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class Fir2IrCliJvmFacade(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact,
    ): IrBackendInput? {
        require(inputArtifact is FirCliBasedJvmOutputArtifact) {
            "Fir2IrCliJvmFacade expects FirCliBasedJvmOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val messageCollector = inputArtifact.cliArtifact.configuration.messageCollector
        val input = inputArtifact.cliArtifact.copy(
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)
        )
        val output = JvmFir2IrPipelinePhase.executePhase(input)
            ?: return processErrorFromCliPhase(messageCollector, testServices)
        return Fir2IrCliBasedJvmOutputArtifact(output)
    }
}

class Fir2IrCliBasedJvmOutputArtifact(val cliArtifact: JvmFir2IrPipelineArtifact) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = cliArtifact.result.irModuleFragment
    override val irPluginContext: IrPluginContext
        get() = cliArtifact.result.pluginContext
    override val descriptorMangler: KotlinMangler.DescriptorMangler?
        get() = null
    override val irMangler: KotlinMangler.IrMangler
        get() = cliArtifact.result.components.irMangler
    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = cliArtifact.diagnosticCollector
}
