/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager

object WebFir2IrPipelinePhase : PipelinePhase<WebFrontendPipelineArtifact, JsFir2IrPipelineArtifact>(
    name = "JsFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: WebFrontendPipelineArtifact): JsFir2IrPipelineArtifact? {
        val (analyzedOutput, configuration, diagnosticsReporter, moduleStructure, hasErrors) = input
        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)
        return JsFir2IrPipelineArtifact(
            fir2IrActualizedResult,
            analyzedOutput,
            configuration,
            diagnosticsReporter,
            moduleStructure,
            hasErrors = hasErrors || configuration.messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
        )
    }

    fun transformFirToIr(
        moduleStructure: ModulesStructure,
        firOutputs: List<ModuleCompilerAnalyzedOutput>,
        diagnosticsReporter: BaseDiagnosticsCollector,
    ): Fir2IrActualizedResult {
        val fir2IrExtensions = Fir2IrExtensions.Default

        var builtInsModule: KotlinBuiltIns? = null
        val dependencies = mutableListOf<ModuleDescriptorImpl>()

        val librariesDescriptors = moduleStructure.klibs.all.map { resolvedLibrary ->
            val storageManager = LockBasedStorageManager("ModulesStructure")

            val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary,
                moduleStructure.compilerConfiguration.languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
            )
            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(ArrayList(dependencies))

            val isBuiltIns = resolvedLibrary.unresolvedDependencies.isEmpty()
            if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

            moduleDescriptor
        }

        val firResult = FirResult(firOutputs)
        return firResult.convertToIrAndActualize(
            fir2IrExtensions,
            Fir2IrConfiguration.forKlibCompilation(moduleStructure.compilerConfiguration, diagnosticsReporter),
            IrGenerationExtension.getInstances(moduleStructure.project),
            irMangler = JsManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
            typeSystemContextProvider = ::IrTypeSystemContextImpl,
            specialAnnotationsProvider = null,
            extraActualDeclarationExtractorsInitializer = { emptyList() },
        ) { irModuleFragment ->
            (irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
        }
    }
}
