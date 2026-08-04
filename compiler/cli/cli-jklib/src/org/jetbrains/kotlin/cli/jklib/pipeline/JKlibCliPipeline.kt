/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.common.phaser.thenIf
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.HeaderMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.util.PerformanceManager

class JKlibCliPipeline(
    override val defaultPerformanceManager: PerformanceManager,
) : AbstractCliPipeline<K2JKlibCompilerArguments>() {
    override fun createCompoundPhase(
        arguments: K2JKlibCompilerArguments,
    ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JKlibCompilerArguments>, *> {
        val phases =
            JKlibConfigurationPhase then
                JKlibFrontendPipelinePhase.thenIf(
                    condition = ::skipIrGeneration,
                    onTrue = JKlibMetadataSerializationPhase,
                    onFalse = JKlibFir2IrPipelinePhase then JKlibKlibSerializationPhase,
                )

        return if (arguments.compileIr) {
            phases then JKlibIrCompilationPhase
        } else {
            phases
        }
    }

    /**
     * Skips IR generation when running header mode with -Xheader-mode and
     * -Xheader-mode-type=compilation, unless the module contains declarations that require IR
     * (inline functions, inline property accessors, or value classes).
     */
    private fun skipIrGeneration(artifact: JKlibFrontendPipelineArtifact): Boolean {
        val configuration = artifact.configuration
        return configuration.languageVersionSettings.getFlag(AnalysisFlags.headerMode) &&
                configuration.languageVersionSettings.getFlag(AnalysisFlags.headerModeType) == HeaderMode.COMPILATION &&
                !requireIrForHeaderCompilationMode(artifact.frontendOutput)
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun requireIrForHeaderCompilationMode(frontendOutput: AllModulesFrontendOutput): Boolean {
        return frontendOutput.outputs.any { output ->
            output.fir.any { file ->
                requireIrForHeaderCompilationMode(file.declarations)
            }
        }
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun requireIrForHeaderCompilationMode(declarations: List<FirDeclaration>): Boolean {
        return declarations.any { declaration ->
            when (declaration) {
                is FirFunction -> declaration.status.isInline
                is FirProperty -> declaration.getter?.status?.isInline == true || declaration.setter?.status?.isInline == true
                is FirClass -> declaration.status.isValue || requireIrForHeaderCompilationMode(declaration.declarations)
                else -> false
            }
        }
    }
}
