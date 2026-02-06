/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.cli.CliDiagnostics.WEB_ARGUMENT_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.WEB_ARGUMENT_WARNING
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.incrementalCompilationIsEnabledForJs
import org.jetbrains.kotlin.cli.common.list
import org.jetbrains.kotlin.cli.js.initializeFinalArtifactConfiguration
import org.jetbrains.kotlin.cli.js.moduleKindMap
import org.jetbrains.kotlin.cli.js.targetVersion
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.ConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.SuccessfulPipelineExecutionException
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.ir.backend.js.jsLowerings
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.platform.js.JsPlatforms

object JsConfigurationUpdater : ConfigurationUpdater<K2JSCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        if (configuration.wasmCompilation) return
        val arguments = input.arguments
        fillConfiguration(configuration, arguments)
        checkWasmArgumentsUsage(arguments, configuration)

        // setup phase config for the second compilation stage (JS codegen)
        if (arguments.includes != null) {
            configuration.phaseConfig = createPhaseConfig(arguments).also {
                if (arguments.listPhases) it.list(jsLowerings)
            }
        }
    }

    private fun fillConfiguration(configuration: CompilerConfiguration, arguments: K2JSCompilerArguments) {
        val targetVersion = initializeAndCheckTargetVersion(arguments, configuration)
        configuration.optimizeGeneratedJs = arguments.optimizeGeneratedJs
        val isES2015 = targetVersion == EcmaVersion.es2015
        configuration.moduleKind = configuration.moduleKind
            ?: moduleKindMap[arguments.moduleKind]
                    ?: ModuleKind.ES.takeIf { isES2015 }
                    ?: ModuleKind.UMD

        initializeFinalArtifactConfiguration(configuration, arguments)

        configuration.keep = arguments.irKeep?.split(",")?.filterNot { it.isEmpty() }.orEmpty()
        configuration.safeExternalBoolean = arguments.irSafeExternalBoolean
        configuration.minimizedMemberNames = arguments.irMinimizedMemberNames
        configuration.propertyLazyInitialization = arguments.irPropertyLazyInitialization
        configuration.generatePolyfills = arguments.generatePolyfills
        configuration.generateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions
        configuration.useEs6Classes = arguments.useEsClasses ?: isES2015
        configuration.compileSuspendAsJsGenerator = arguments.useEsGenerators ?: isES2015
        configuration.compileLambdasAsEs6ArrowFunctions = arguments.useEsArrowFunctions ?: isES2015
        configuration.compileLongAsBigint = arguments.compileLongAsBigInt ?: false

        configuration.targetPlatform = JsPlatforms.defaultJsPlatform

        arguments.irSafeExternalBooleanDiagnostic?.let {
            configuration.safeExternalBooleanDiagnostic = it
        }

        arguments.platformArgumentsProviderJsExpression?.let {
            configuration.definePlatformMainFunctionArguments = it
        }

        if (arguments.script) {
            configuration.report(WEB_ARGUMENT_ERROR, "K/JS does not support Kotlin script (*.kts) files")
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                // Stop the pipeline, return ExitCode.OK
                throw SuccessfulPipelineExecutionException()
            }
            if (arguments.includes.isNullOrEmpty()) {
                configuration.report(WEB_ARGUMENT_ERROR, "Specify at least one source file or directory", location = null)
            }
        }
    }

    private fun initializeAndCheckTargetVersion(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration
    ): EcmaVersion? {
        val targetVersion = arguments.targetVersion

        if (targetVersion == null) {
            configuration.report(WEB_ARGUMENT_ERROR, "Unsupported ECMA version: ${arguments.target}")
        }
        return targetVersion
    }

    internal fun checkWasmArgumentsUsage(arguments: K2JSCompilerArguments, configuration: CompilerConfiguration) {
        if (arguments.irDceDumpReachabilityInfoToFile != null) {
            configuration.report(WEB_ARGUMENT_WARNING, "Dumping the reachability info to a file is not supported for Kotlin/JS.")
        }
        if (arguments.irDceDumpDeclarationIrSizesToFile != null) {
            configuration.report(WEB_ARGUMENT_WARNING, "Dumping the sizes of declarations to file is not supported for Kotlin/JS.")
        }
    }
}
