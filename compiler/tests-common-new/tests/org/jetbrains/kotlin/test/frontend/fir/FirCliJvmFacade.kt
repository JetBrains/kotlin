/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade.Companion.shouldRunFirFrontendFacade
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class FirCliJvmFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(cliBasedFacadesMarkerRegistrationData)

    override fun shouldTransform(module: TestModule): Boolean {
        return shouldRunFirFrontendFacade(module, testServices)
    }

    override fun analyze(module: TestModule): FirOutputArtifact? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            rootDisposable = testServices.compilerConfigurationProvider.testRootDisposable,
        )

        val output = JvmFrontendPipelinePhase.executePhase(input) ?: reportErrorFromCliPhase()

        val firOutputs = output.result.outputs
        val modulesFromTheSameStructure = module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
            .associateBy { "<${it.name}>"}
        val testFirOutputs = firOutputs.map {
            val correspondingModule = modulesFromTheSameStructure.getValue(it.session.moduleData.name.asString())
            val testFilePerFirFile = correspondingModule.files.mapNotNull { testFile ->
                val firFile = it.fir.firstOrNull { firFile ->
                    val path = testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(testFile).canonicalPath
                    path == firFile.sourceFile?.path
                } ?: return@mapNotNull null
                testFile to firFile
            }
            FirOutputPartForDependsOnModule(
                module = correspondingModule,
                session = it.session,
                scopeSession = it.scopeSession,
                firAnalyzerFacade = null,
                firFiles = testFilePerFirFile.toMap()
            )
        }

        return FirCliBasedJvmOutputArtifact(output, testFirOutputs)
    }
}

class FirCliBasedJvmOutputArtifact(
    val cliArtifact: JvmFrontendPipelineArtifact,
    partsForDependsOnModules: List<FirOutputPartForDependsOnModule>
) : FirOutputArtifact(partsForDependsOnModules)

fun reportErrorFromCliPhase(): Nothing {
    error("CLI phased returned null")
}
