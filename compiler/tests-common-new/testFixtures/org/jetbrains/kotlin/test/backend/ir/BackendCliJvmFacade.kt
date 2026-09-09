/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmLoweredIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendDiagnosticsPhaseFacade
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.testInfraError

open class JvmLoweringCliFacade(testServices: TestServices) :
    IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean = true

    protected open fun createDiagnosticsCollector(): BaseDiagnosticsCollector = DiagnosticsCollectorImpl()

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        checkTestInfrastructure(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "JvmLoweringCliFacade expects Fir2IrCliBasedOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        checkTestInfrastructure(inputArtifact.cliArtifact is JvmFir2IrPipelineArtifact) {
            "JvmLoweringCliFacade expects JvmFir2IrPipelineArtifact as input, but ${inputArtifact.cliArtifact::class} was found"
        }
        val diagnosticsCollector = createDiagnosticsCollector()
        val input = inputArtifact.cliArtifact.withNewDiagnosticCollector(diagnosticsCollector)
        return JvmLoweredIrBackendInput(input, JvmBackendPipelinePhase.runLowerings(input))
    }
}

/**
 * Runs JVM lowerings with a reporter that can hand diagnostics off to the following codegen phase.
 */
class JvmDiagnosticsLoweringCliFacade(testServices: TestServices) : JvmLoweringCliFacade(testServices) {
    override fun createDiagnosticsCollector(): BaseDiagnosticsCollector = JvmBackendPhaseDiagnosticsCollector()
}

/**
 * Makes diagnostics consumed after JVM lowerings unavailable to JVM code generation without discarding them.
 */
class JvmCodegenDiagnosticsPhaseFacade(testServices: TestServices) :
    BackendDiagnosticsPhaseFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean = true

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        checkTestInfrastructure(inputArtifact is JvmLoweredIrBackendInput) {
            "JvmCodegenDiagnosticsPhaseFacade expects JvmLoweredIrBackendInput as input, but ${inputArtifact::class} was found"
        }
        return inputArtifact.startCodegenDiagnosticsPhase()
    }
}

class BackendCliJvmFacade(testServices: TestServices) : AbstractJvmIrBackendFacade(testServices) {
    override fun produceGenerationState(inputArtifact: IrBackendInput): GenerationState {
        val loweredInput = when (inputArtifact) {
            is JvmCodegenIrBackendInput -> inputArtifact.loweredInput
            is JvmLoweredIrBackendInput -> inputArtifact
            else -> testInfraError(
                "BackendCliJvmFacade expects JvmLoweredIrBackendInput or JvmCodegenIrBackendInput as input, " +
                        "but ${inputArtifact::class} was found"
            )
        }
        val output = JvmBackendPipelinePhase.runCodegen(loweredInput.loweredArtifact).let(JvmWriteOutputsPhase::executePhase)
        return output.outputs.single()
    }

    override val IrBackendInput.sourceFiles: Collection<KtSourceFile>
        get() = when (this) {
            is JvmCodegenIrBackendInput -> loweredInput.cliArtifact.sourceFiles
            is JvmLoweredIrBackendInput -> cliArtifact.sourceFiles
            else -> error("Unexpected backend input: ${this::class}")
        }
}

class JvmLoweredIrBackendInput(
    val cliArtifact: JvmFir2IrPipelineArtifact,
    val loweredArtifact: JvmLoweredIrPipelineArtifact,
) : IrBackendInput() {
    private val phaseDiagnosticsCollector: JvmBackendPhaseDiagnosticsCollector?
        get() = cliArtifact.configuration.diagnosticsCollector as? JvmBackendPhaseDiagnosticsCollector

    override val irModuleFragment: IrModuleFragment
        get() = cliArtifact.result.irModuleFragment
    override val irBuiltIns: IrBuiltIns
        get() = cliArtifact.result.irBuiltIns
    override val irMangler: KotlinMangler.IrMangler
        get() = cliArtifact.result.components.irMangler
    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = phaseDiagnosticsCollector?.loweringDiagnosticsCollector ?: cliArtifact.configuration.diagnosticsCollector

    /**
     * Exposes a new artifact for code generation after lowered-IR handlers have consumed lowering diagnostics.
     */
    fun startCodegenDiagnosticsPhase(): JvmCodegenIrBackendInput {
        val phaseDiagnosticsCollector = checkNotNull(phaseDiagnosticsCollector) {
            "JVM lowering did not use a phase-aware diagnostics collector"
        }
        return JvmCodegenIrBackendInput(this, phaseDiagnosticsCollector.startCodegenPhase())
    }
}

class JvmCodegenIrBackendInput(
    val loweredInput: JvmLoweredIrBackendInput,
    override val diagnosticReporter: BaseDiagnosticsCollector,
) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = loweredInput.irModuleFragment
    override val irBuiltIns: IrBuiltIns
        get() = loweredInput.irBuiltIns
    override val irMangler: KotlinMangler.IrMangler
        get() = loweredInput.irMangler
}

/**
 * Keeps the reporter captured by [GenerationState] and its backend context stable while separating lowering and
 * codegen diagnostics.
 */
private class JvmBackendPhaseDiagnosticsCollector : BaseDiagnosticsCollector() {
    val loweringDiagnosticsCollector = DiagnosticsCollectorImpl()
    private var delegate: BaseDiagnosticsCollector = loweringDiagnosticsCollector
    private var codegenPhaseStarted = false

    override val diagnostics: List<KtDiagnostic>
        get() = delegate.diagnostics
    override val diagnosticsByFile: Map<KtSourceFile?, List<KtDiagnostic>>
        get() = delegate.diagnosticsByFile
    override val hasErrors: Boolean
        get() = delegate.hasErrors
    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        delegate.report(diagnostic, context)
    }

    fun startCodegenPhase(): BaseDiagnosticsCollector {
        check(!codegenPhaseStarted) { "Codegen diagnostics phase has already started" }
        codegenPhaseStarted = true
        return DiagnosticsCollectorImpl().also { delegate = it }
    }
}
