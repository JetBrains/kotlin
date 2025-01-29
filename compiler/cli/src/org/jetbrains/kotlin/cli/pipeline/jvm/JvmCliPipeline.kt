/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.K2JVMCompilerPerformanceManager
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.CommonCompilerPerformanceManager

class JvmCliPipeline(override val defaultPerformanceManager: K2JVMCompilerPerformanceManager) : AbstractCliPipeline<K2JVMCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JVMCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, *> {
        return when {
            arguments.scriptingModeEnabled -> createScriptPipeline()
            else -> createRegularPipeline()
        }
    }

    private fun createRegularPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmBinaryPipelineArtifact> =
        JvmConfigurationPipelinePhase then
                JvmFrontendPipelinePhase then
                JvmFir2IrPipelinePhase then
                JvmBackendPipelinePhase

    private fun createScriptPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmScriptPipelineArtifact> =
        JvmConfigurationPipelinePhase then
                JvmScriptPipelinePhase

    private val K2JVMCompilerArguments.scriptingModeEnabled: Boolean
        get() = buildFile == null &&
                !version &&
                !allowNoSourceFiles &&
                (script || expression != null || freeArgs.isEmpty())

    override fun isKaptMode(arguments: K2JVMCompilerArguments): Boolean {
        return K2JVMCompiler.kaptIsEnabled(arguments)
    }

    override fun createPerformanceManager(
        arguments: K2JVMCompilerArguments,
        services: Services,
    ): CommonCompilerPerformanceManager {
        return K2JVMCompiler.createCustomPerformanceManagerOrNull(arguments, services) ?: defaultPerformanceManager
    }
}
