/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

class NativeKlibCliPipeline(
    override val defaultPerformanceManager: PerformanceManager
) : AbstractCliPipeline<K2NativeKlibCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2NativeKlibCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2NativeKlibCompilerArguments>, *> {
        TODO("Not yet implemented")
    }
}