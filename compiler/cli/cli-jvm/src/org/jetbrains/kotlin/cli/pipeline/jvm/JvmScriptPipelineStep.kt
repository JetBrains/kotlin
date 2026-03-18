/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.CliDiagnostics.SCRIPTING_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.SCRIPTING_WARNING
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.freeArgsForScript
import org.jetbrains.kotlin.cli.common.replMode
import org.jetbrains.kotlin.cli.common.scriptMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.expressionToEvaluate

object JvmScriptPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JvmScriptPipelineArtifact>(
    name = "JvmScriptPipelinePhase",
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JvmScriptPipelineArtifact? {
        val (configuration, rootDisposable) = input
        if (configuration.scriptMode && configuration.freeArgsForScript.isEmpty()) {
            configuration.report(SCRIPTING_ERROR, "Specify script source path to evaluate")
            return null
        }
        if (configuration.replMode && configuration.freeArgsForScript.isNotEmpty()) {
            configuration.report(SCRIPTING_WARNING, "The arguments are ignored in the REPL mode")
        }

        val projectEnvironment by lazy(LazyThreadSafetyMode.NONE) {
            KotlinCoreEnvironment.ProjectEnvironment(
                rootDisposable,
                KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(rootDisposable, configuration),
                configuration
            ).also {
                it.registerExtensionsFromPlugins(configuration)
            }
        }

        val result = if (configuration.scriptMode || configuration.expressionToEvaluate != null) {
            val argumentsStub = K2JVMCompilerArguments().apply {
                script = true
            }
            val scriptingEvaluator = ScriptEvaluationExtension.getInstances(projectEnvironment.project)
                .find { it.isAccepted(argumentsStub) }
            if (scriptingEvaluator == null) {
                configuration.report(SCRIPTING_ERROR, "Unable to evaluate script, no scripting plugin loaded")
                return null
            }
            scriptingEvaluator.eval(configuration, projectEnvironment)
        } else {
            if (!configuration.replMode) {
                configuration.report(
                    SCRIPTING_ERROR,
                    "Kotlin REPL is deprecated and should be enabled explicitly for now; please use the '-Xrepl' option"
                )
                return null
            }
            // arguments are unused in the ShellExtension anyway
            val argumentsStub = K2JVMCompilerArguments()
            val shell = ShellExtension.getInstances(projectEnvironment.project).find { it.isAccepted(argumentsStub) }
            if (shell == null) {
                configuration.report(SCRIPTING_ERROR, "Unable to run REPL, no scripting plugin loaded")
                return null
            }
            shell.run(argumentsStub, configuration, projectEnvironment)
        }
        return JvmScriptPipelineArtifact(result, configuration)
    }

}
