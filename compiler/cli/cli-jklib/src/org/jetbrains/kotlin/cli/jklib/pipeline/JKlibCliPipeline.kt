/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

class JKlibCliPipeline(override val defaultPerformanceManager: PerformanceManager) : AbstractCliPipeline<K2JKlibCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JKlibCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JKlibCompilerArguments>, *> {
        return JKlibConfigurationPhase then
                JKlibFrontendPipelinePhase then
                JKlibFir2IrPipelinePhase then
                JKlibKlibSerializationPhase
    }
}
