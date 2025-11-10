/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibFileWriterPhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibInMemorySerializerPhase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.METADATA_ONLY_COMPILATION
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isLeafModuleInMppGraph
import java.io.File

class FirCliMetadataFrontendFacade(
    testServices: TestServices,
) : FirCliFacade<MetadataFrontendPipelinePhase, MetadataFrontendPipelineArtifact>(testServices, MetadataFrontendPipelinePhase) {
    companion object {
        fun shouldTransform(module: TestModule, testServices: TestServices): Boolean {
            if (METADATA_ONLY_COMPILATION in module.directives) return true
            if (!module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return false
            return !module.isLeafModuleInMppGraph(testServices)
        }
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldTransform(module, testServices)
    }

    override fun getPartsForDependsOnModules(
        module: TestModule,
        firOutputs: List<SingleModuleFrontendOutput>
    ): List<FirOutputPartForDependsOnModule> {
        val analyzedModule = firOutputs.single()
        return listOf(analyzedModule.toTestOutputPart(module, testServices))
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
        require(inputArtifact is FirCliBasedOutputArtifact<*>) {
            "Incompatible type of input artifact: expected ${FirCliBasedOutputArtifact::class}, actual ${inputArtifact::class}"
        }
        require(inputArtifact.cliArtifact is MetadataFrontendPipelineArtifact) {
            "Incompatible type of input artifact: expected ${MetadataFrontendPipelineArtifact::class}, actual ${inputArtifact.cliArtifact::class}"
        }
        val input = inputArtifact.cliArtifact
        val output = MetadataKlibInMemorySerializerPhase.executePhase(input).let(MetadataKlibFileWriterPhase::executePhase)
        return BinaryArtifacts.KLib(
            File(output.destination),
            SimpleDiagnosticsCollector(BaseDiagnosticsCollector.RawReporter.DO_NOTHING)
        )
    }
}

