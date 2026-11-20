/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmLoweredIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmLoweringsPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.codegen.state.AllowedOnlyInTestsAPI
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class LoweringCliJvmFacade(testServices: TestServices) : IrPreSerializationLoweringFacade<IrBackendInput>(
    testServices,
    inputKind = BackendKinds.IrBackend,
    outputKind = BackendKinds.IrBackend,
) {
    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput,
    ): IrBackendInput {
        checkTestInfrastructure(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "LoweringCliJvmFacade expects Fir2IrCliBasedJvmOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val cliArtifact = inputArtifact.cliArtifact
        checkTestInfrastructure(cliArtifact is JvmFir2IrPipelineArtifact) {
            "LoweringCliJvmFacade expects JvmFir2IrPipelineArtifact as input, but ${cliArtifact::class} was found"
        }
        val input = cliArtifact.withNewDiagnosticCollector(DiagnosticsCollectorImpl())
        val output = JvmLoweringsPipelinePhase.executePhase(input)
        return LoweredJvmCliBasedOutputArtifact(output)
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return true
    }
}

class LoweredJvmCliBasedOutputArtifact(val cliArtifact: JvmLoweredIrPipelineArtifact) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = lastInput.module
    override val irBuiltIns: IrBuiltIns
        get() = lastInput.context.irBuiltIns
    override val irMangler: KotlinMangler.IrMangler
        get() = JvmIrMangler
    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = cliArtifact.configuration.diagnosticsCollector

    private val lastInput: JvmIrCodegenFactory.CodegenInput
        get() = cliArtifact.codegenInputs.last()

    fun createCliArtifactWithNewDiagnosticCollector(newDiagnosticsCollector: BaseDiagnosticsCollector): JvmLoweredIrPipelineArtifact {
        val newConfiguration = cliArtifact.configuration.copy().apply {
            this.diagnosticsCollector = newDiagnosticsCollector
        }
        return JvmLoweredIrPipelineArtifact(
            newConfiguration,
            cliArtifact.environment,
            cliArtifact.mainClassFqName,
            cliArtifact.codegenInputs.onEach {
                @OptIn(AllowedOnlyInTestsAPI::class)
                it.replaceConfigurationAndDiagnosticReporter(newConfiguration, newDiagnosticsCollector)
            },
            cliArtifact.sourceFiles,
        )
    }
}
