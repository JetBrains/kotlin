/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.jklib.pipeline.IrCompilationResult
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibIrCompilationResultPhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jklib.JKlibIrMangler
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.frontend.fir.FirCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade.Companion.shouldRunFirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

/**
 * Produces the IR that `K2JKlibCompiler.compileToIr` hands over to its consumer: the output of FIR2IR, wrapped by
 * [JKlibIrCompilationResultPhase].
 */
class Fir2IrCliJKlibFacade(testServices: TestServices) :
    Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(testServices, FrontendKinds.FIR, BackendKinds.IrBackend) {

    override fun shouldTransform(module: TestModule): Boolean {
        return super.shouldTransform(module) && shouldRunFirFrontendFacade(module, testServices)
    }

    override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): IrBackendInput {
        checkTestInfrastructure(inputArtifact is FirCliBasedOutputArtifact<*>) {
            "${this::class} expects FirCliBasedOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val cliArtifact = inputArtifact.cliArtifact as JKlibFrontendPipelineArtifact
        val input = cliArtifact.withNewDiagnosticCollector(DiagnosticsCollectorImpl())
        val fir2IrArtifact = JKlibFir2IrPipelinePhase.executePhase(input)
        return JKlibIrCompilationResultBackendInput(JKlibIrCompilationResultPhase.executePhase(fir2IrArtifact), fir2IrArtifact)
    }
}

/**
 * @property fir2IrArtifact The input of [JKlibIrCompilationResultPhase], used to serialize the module into the KLIB that
 *  the modules depending on it compile against.
 */
class JKlibIrCompilationResultBackendInput(
    val compilationResult: IrCompilationResult,
    val fir2IrArtifact: JKlibFir2IrPipelineArtifact,
) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = compilationResult.moduleFragment

    override val irBuiltIns: IrBuiltIns
        get() = compilationResult.pluginContext.irBuiltIns

    override val irMangler: KotlinMangler.IrMangler
        get() = JKlibIrMangler()

    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = compilationResult.configuration.diagnosticsCollector
}
