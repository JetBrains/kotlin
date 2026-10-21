/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibPreSerializationLoweringPhase
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.IrPreSerializationLoweringFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class PreSerializationLoweringCliJKlibFacade(
    testServices: TestServices,
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun shouldTransform(module: TestModule): Boolean = true

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        checkTestInfrastructure(inputArtifact is Fir2IrCliBasedOutputArtifact<*>) {
            "${this::class} expects Fir2IrCliBasedOutputArtifact as input, but ${inputArtifact::class} was found"
        }
        val cliArtifact = inputArtifact.cliArtifact
        checkTestInfrastructure(cliArtifact is JKlibFir2IrPipelineArtifact) {
            "Fir2IrCliBasedOutputArtifact should have JKlibFir2IrPipelineArtifact as cliArtifact, but has ${cliArtifact::class}"
        }
        // Attach a new empty diagnostic collector to prevent double-reporting of the diagnostics of the Fir2IR phase.
        val input = cliArtifact.withNewDiagnosticCollector(DiagnosticsCollectorImpl())
        return Fir2IrCliBasedOutputArtifact(JKlibPreSerializationLoweringPhase.executePhase(input))
    }
}
