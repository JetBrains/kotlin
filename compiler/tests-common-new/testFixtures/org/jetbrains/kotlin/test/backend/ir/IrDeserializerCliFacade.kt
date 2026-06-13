/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.LoadedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.test.frontend.fir.processErrorFromCliPhase
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

abstract class IrDeserializerCliFacade<Phase, OutputPipelineArtifact>(
    testServices: TestServices,
    private val phase: Phase,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(
    testServices,
    ArtifactKinds.KLib,
    BackendKinds.IrBackend,
) where Phase : PipelinePhase<ConfigurationPipelineArtifact, OutputPipelineArtifact>,
        OutputPipelineArtifact : LoadedIrPipelineArtifact {

    override fun transform(
        module: TestModule,
        inputArtifact: BinaryArtifacts.KLib,
    ): DeserializedFromKlibBackendInput<OutputPipelineArtifact>? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            rootDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
        )
        val output = phase.executePhase(input)
            ?: return processErrorFromCliPhase(configuration, testServices)
        return DeserializedFromKlibBackendInput(output, klib = inputArtifact.outputFile)
    }
}
