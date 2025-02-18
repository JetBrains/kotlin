/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibSerializerPhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

class FirCliMetadataFrontendFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    companion object {
        fun shouldTransform(module: TestModule, testServices: TestServices): Boolean {
            if (!module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return false
            return !module.isLeafModuleInMppGraph(testServices)
        }
    }

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(cliBasedFacadesMarkerRegistrationData)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldTransform(module, testServices)
    }

    override fun analyze(module: TestModule): FirOutputArtifact? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            rootDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
        )
        val output = MetadataFrontendPipelinePhase.executePhase(input)

        val analyzedModule = output.result.outputs.single()
        val part = analyzedModule.toTestOutputPart(module, testServices)
        return FirCliBasedMetadataFrontendOutputArtifact(output, listOf(part))
    }
}

class FirCliMetadataSerializerFacade(val testServices: TestServices) : AbstractTestFacade<FirOutputArtifact, BinaryArtifacts.KLib>() {
    override val inputKind: TestArtifactKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val outputKind: TestArtifactKind<BinaryArtifacts.KLib>
        get() = ArtifactKinds.KLib

    override fun shouldTransform(module: TestModule): Boolean {
        return FirCliMetadataFrontendFacade.shouldTransform(module, testServices)
    }

    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact,
    ): BinaryArtifacts.KLib? {
        require(inputArtifact is FirCliBasedMetadataFrontendOutputArtifact) {
            "Incompatible type of input artifact: expected ${FirCliBasedMetadataFrontendOutputArtifact::class}, actual ${inputArtifact::class}"
        }
        val input = inputArtifact.cliArtifact
        val output = MetadataKlibSerializerPhase.executePhase(input)
        return BinaryArtifacts.KLib(
            File(output.destination),
            SimpleDiagnosticsCollector(BaseDiagnosticsCollector.RawReporter.DO_NOTHING)
        )
    }
}

class FirCliBasedMetadataFrontendOutputArtifact(
    val cliArtifact: MetadataFrontendPipelineArtifact,
    partsForDependsOnModules: List<FirOutputPartForDependsOnModule>
) : FirOutputArtifact(partsForDependsOnModules)
