/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.perfManager

/**
 * Phase that creates the KotlinCoreEnvironment for native klib compilation.
 */
object NativeEnvironmentPhase : PipelinePhase<ConfigurationPipelineArtifact, NativeConfigurationArtifact>(
    name = "NativeEnvironmentPhase",
    postActions = setOf(PerformanceNotifications.InitializationFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): NativeConfigurationArtifact {
        val (configuration, rootDisposable) = input
        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable,
            configuration,
            EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        )
        val perfManager = configuration.perfManager
        val sourceFiles = environment.getSourceFiles()
        perfManager?.addSourcesStats(sourceFiles.size, environment.countLinesOfCode(sourceFiles))
        return NativeConfigurationArtifact(
            configuration = configuration,
            environment = environment,
        )
    }
}
