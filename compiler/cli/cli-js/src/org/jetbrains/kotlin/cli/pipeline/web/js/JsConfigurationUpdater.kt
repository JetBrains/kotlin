/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.incrementalCompilationIsEnabledForJs
import org.jetbrains.kotlin.cli.common.list
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JsCompilerImpl
import org.jetbrains.kotlin.cli.js.moduleKindMap
import org.jetbrains.kotlin.cli.js.targetVersion
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.SuccessfulPipelineExecutionException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.ir.backend.js.getJsPhases
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.serialization.js.ModuleKind

object JsConfigurationUpdater : ConfigurationUpdater<K2JSCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        if (configuration.wasmCompilation) return
        val arguments = input.arguments
        fillConfiguration(configuration, arguments)
        checkWasmArgumentsUsage(arguments, configuration.messageCollector)

        // setup phase config for the second compilation stage (JS codegen)
        if (arguments.includes != null) {
            configuration.phaseConfig = createPhaseConfig(arguments).also {
                it.list(getJsPhases(configuration))
            }
        }
    }

    /**
     * This part of the configuration update is shared between phased K2 CLI and
     * K1 implementation of [K2JsCompilerImpl.tryInitializeCompiler].
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
        configuration.generateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions
        configuration.useEs6Classes = arguments.useEsClasses ?: isES2015
        configuration.compileSuspendAsJsGenerator = arguments.useEsGenerators ?: isES2015
        configuration.compileLambdasAsEs6ArrowFunctions = arguments.useEsArrowFunctions ?: isES2015

        arguments.platformArgumentsProviderJsExpression?.let {
            configuration.definePlatformMainFunctionArguments = it
        }

        if (arguments.script) {
            messageCollector.report(ERROR, "K/JS does not support Kotlin script (*.kts) files")
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                // Stop the pipeline, return ExitCode.OK
                throw SuccessfulPipelineExecutionException()
            }
            if (arguments.includes.isNullOrEmpty()) {
                messageCollector.report(ERROR, "Specify at least one source file or directory", location = null)
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
            messageCollector.report(ERROR, "Unsupported ECMA version: ${arguments.target}")
        }
        return targetVersion
    }

    internal fun checkWasmArgumentsUsage(arguments: K2JSCompilerArguments, messageCollector: MessageCollector) {
        if (arguments.irDceDumpReachabilityInfoToFile != null) {
            messageCollector.report(STRONG_WARNING, "Dumping the reachability info to a file is not supported for Kotlin/JS.")
        }
        if (arguments.irDceDumpDeclarationIrSizesToFile != null) {
            messageCollector.report(STRONG_WARNING, "Dumping the sizes of declarations to file is not supported for Kotlin/JS.")
        }
    }
}
