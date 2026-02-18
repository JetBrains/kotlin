/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibCheckers
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.collectJsExportNames
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.incrementalDataProvider
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.library.isJsStdlib
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.storage.LockBasedStorageManager

object WebFir2IrPipelinePhase : PipelinePhase<WebFrontendPipelineArtifact, JsFir2IrPipelineArtifact>(
    name = "JsFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: WebFrontendPipelineArtifact): JsFir2IrPipelineArtifact {
        val (firResult, configuration, moduleStructure, hasErrors) = input
        val diagnosticsReporter = configuration.diagnosticsCollector
        val fir2IrActualizedResult = transformFirToIr(moduleStructure, firResult.outputs, diagnosticsReporter)
        if (!configuration.wasmCompilation)
            runJsKlibCallCheckers(diagnosticsReporter, configuration, firResult.outputs, fir2IrActualizedResult)

        return JsFir2IrPipelineArtifact(
            fir2IrActualizedResult,
            firResult,
            configuration,
            moduleStructure,
            hasErrors = hasErrors || configuration.messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
        )
    }

    private fun transformFirToIr(
        moduleStructure: ModulesStructure,
        firOutputs: List<SingleModuleFrontendOutput>,
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
                lookupTracker = LookupTracker.DO_NOTHING
            )
            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(ArrayList(dependencies))

            val isBuiltIns = resolvedLibrary.isJsStdlib || resolvedLibrary.isWasmStdlib
            if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

            moduleDescriptor
        }

        val firResult = AllModulesFrontendOutput(firOutputs)
        return firResult.convertToIrAndActualize(
            fir2IrExtensions,
            Fir2IrConfiguration.forKlibCompilation(moduleStructure.compilerConfiguration, diagnosticsReporter),
            moduleStructure.compilerConfiguration.getCompilerExtensions(IrGenerationExtension),
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


private fun runJsKlibCallCheckers(
    diagnosticReporter: BaseDiagnosticsCollector,
    configuration: CompilerConfiguration,
    firOutputs: List<SingleModuleFrontendOutput>,
    fir2IrActualizedResult: Fir2IrActualizedResult,
) {
    val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings)

    val fir2KlibMetadataSerializer = Fir2KlibMetadataSerializer(
        configuration,
        firOutputs,
        fir2IrActualizedResult,
        exportKDoc = false,
        produceHeaderKlib = false,
    )
    val cleanFiles = configuration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles).orEmpty()
    val cleanFilesIrData = cleanFiles.map { it.irData ?: error("Metadata-only KLIBs are not supported in Kotlin/JS") }

    val irModuleFragment = fir2IrActualizedResult.irModuleFragment
    irModuleFragment.acceptVoid(
        JsKlibCheckers.makeChecker(
            irDiagnosticReporter,
            configuration,
            doCheckCalls = true,
            doModuleLevelChecks = true,
            cleanFiles = cleanFilesIrData,
            exportedNames = irModuleFragment.collectJsExportNames(),
        )
    )
}
