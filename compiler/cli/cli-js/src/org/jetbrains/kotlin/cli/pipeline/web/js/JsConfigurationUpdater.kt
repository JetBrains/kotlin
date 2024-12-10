/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.incrementalCompilationIsEnabledForJs
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.moduleKindMap
import org.jetbrains.kotlin.cli.js.targetVersion
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.SuccessfulPipelineExecutionException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.compileLambdasAsEs6ArrowFunctions
import org.jetbrains.kotlin.js.config.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.js.config.definePlatformMainFunctionArguments
import org.jetbrains.kotlin.js.config.generateDts
import org.jetbrains.kotlin.js.config.generateInlineAnonymousFunctions
import org.jetbrains.kotlin.js.config.generatePolyfills
import org.jetbrains.kotlin.js.config.moduleKind
import org.jetbrains.kotlin.js.config.optimizeGeneratedJs
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.propertyLazyInitialization
import org.jetbrains.kotlin.js.config.target
import org.jetbrains.kotlin.js.config.useEs6Classes
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File
import java.io.IOException
import kotlin.collections.get

object JsConfigurationUpdater : ConfigurationUpdater<K2JSCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        fillConfiguration(configuration, input.arguments)
    }

    /**
     * This part of the configuration update is shared between phased K2 CLI and
     * K1 implementation of [org.jetbrains.kotlin.cli.js.K2JsCompilerImpl.tryInitializeCompiler].
     */
    internal fun fillConfiguration(configuration: CompilerConfiguration, arguments: K2JSCompilerArguments) {
        val messageCollector = configuration.messageCollector
        val targetVersion = initializeAndCheckTargetVersion(arguments, configuration, messageCollector)
        configuration.optimizeGeneratedJs = arguments.optimizeGeneratedJs
        val isES2015 = targetVersion == EcmaVersion.es2015
        val moduleKind = configuration.moduleKind
            ?: moduleKindMap[arguments.moduleKind]
            ?: ModuleKind.ES.takeIf { isES2015 }
            ?: ModuleKind.UMD

        configuration.moduleKind = moduleKind
        configuration.propertyLazyInitialization = arguments.irPropertyLazyInitialization
        configuration.generatePolyfills = arguments.generatePolyfills
        configuration.generateDts = arguments.generateDts
        configuration.generateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions
        configuration.useEs6Classes = arguments.useEsClasses ?: isES2015
        configuration.compileSuspendAsJsGenerator = arguments.useEsGenerators ?: isES2015
        configuration.compileLambdasAsEs6ArrowFunctions = arguments.useEsArrowFunctions ?: isES2015

        arguments.platformArgumentsProviderJsExpression?.let {
            configuration.definePlatformMainFunctionArguments = it
        }

        try {
            configuration.outputDir = File(arguments.outputDir!!).canonicalFile
        } catch (_: IOException) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Could not resolve output directory", location = null)
        }

        if (arguments.script) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "K/JS does not support Kotlin script (*.kts) files")
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                // Stop the pipeline, return ExitCode.OK
                throw SuccessfulPipelineExecutionException()
            }
            if (arguments.includes.isNullOrEmpty()) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "Specify at least one source file or directory", location = null)
            }
        }
    }

    private fun initializeAndCheckTargetVersion(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        messageCollector: MessageCollector,
    ): EcmaVersion? {
        val targetVersion = arguments.targetVersion?.also {
            configuration.target = it
        }

        if (targetVersion == null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Unsupported ECMA version: ${arguments.target}")
        }
        return targetVersion
    }
}
