package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibExitArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibFir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibKlibSerializationPhase
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.model.BackendFacade
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.services.TestServices

import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import java.io.File
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

class BackendCliJKlibFacade(testServices: TestServices) : BackendFacade<IrBackendInput, BinaryArtifacts.KLib>(testServices, BackendKinds.IrBackend, ArtifactKinds.KLib) {
    override fun transform(module: org.jetbrains.kotlin.test.model.TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib? {
        // We accept IrBackendInput, but we expect it to be from our pipeline
        require(inputArtifact is Fir2IrCliBasedOutputArtifact<*>)
        val cliArtifact = inputArtifact.cliArtifact
        require(cliArtifact is JKlibFir2IrPipelineArtifact)

        val phaseConfig = PhaseConfig()
        val context = PipelineContext(
             testServices.compilerConfigurationProvider.getCompilerConfiguration(module).getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY),
             cliArtifact.diagnosticCollector,
             object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "Test") {},
             renderDiagnosticInternalName = false,
             kaptMode = false
        )

        JKlibKlibSerializationPhase.invokeToplevel(phaseConfig, context, cliArtifact)

        // Assuming output is "result.klib" as per default in JKlibKlibSerializationPhase
        return BinaryArtifacts.KLib(File("result.klib"), cliArtifact.diagnosticCollector)
    }
}
