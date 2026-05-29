/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.jvm.FirDirectJavaActualDeclarationExtractor
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object JvmFir2IrPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFir2IrPipelineArtifact>(
    name = "JvmFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFir2IrPipelineArtifact? =
        executePhase(input, input.configuration.getCompilerExtensions(IrGenerationExtension))

    fun executePhase(input: JvmFrontendPipelineArtifact, irGenerationExtensions: List<IrGenerationExtension>): JvmFir2IrPipelineArtifact? {
        (val firResult = frontendOutput, val configuration, val environment, val sourceFiles) = input
        val fir2IrExtensions = JvmFir2IrExtensions(configuration)
        val fir2IrAndIrActualizerResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            configuration.diagnosticsCollector,
            irGenerationExtensions
        )

        val mainClassFqName = runIf(configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            findMainClass(firResult.outputs.last().fir)
        }

        return JvmFir2IrPipelineArtifact(
            fir2IrAndIrActualizerResult,
            configuration,
            environment,
            sourceFiles,
            mainClassFqName,
        )
    }

    fun AllModulesFrontendOutput.convertToIrAndActualizeForJvm(
        fir2IrExtensions: Fir2IrExtensions,
        configuration: CompilerConfiguration,
        diagnosticsReporter: BaseDiagnosticsCollector,
        irGeneratorExtensions: Collection<IrGenerationExtension>,
    ): Fir2IrActualizedResult {
        val fir2IrConfiguration = Fir2IrConfiguration.forJvmCompilation(configuration, diagnosticsReporter)

        return convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            irGeneratorExtensions,
            JvmIrMangler,
            FirJvmVisibilityConverter,
            DefaultBuiltIns.Instance,
            ::JvmIrTypeSystemContext,
            JvmIrSpecialAnnotationSymbolProvider,
            if (configuration.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
                { emptyList() }
            } else {
                { listOfNotNull(FirDirectJavaActualDeclarationExtractor.initializeIfNeeded(it)) }
            },
        )
    }
}
