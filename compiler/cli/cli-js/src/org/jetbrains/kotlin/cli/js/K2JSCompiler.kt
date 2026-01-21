/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.toArgumentStrings
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.web.CommonJsConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.web.JsCliPipeline
import org.jetbrains.kotlin.cli.pipeline.web.LegacyJsWasmPipelineAdapter
import org.jetbrains.kotlin.cli.pipeline.web.WasmCliPipeline
import org.jetbrains.kotlin.cli.pipeline.web.WebCliPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

class K2JSCompiler : KotlinJsCompilerBase<K2JSCompilerArguments>() {
    override val builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS

    override val platform: TargetPlatform = JsPlatforms.defaultJsPlatform

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    override fun createCliPipeline(arguments: K2JSCompilerArguments): WebCliPipeline<K2JSCompilerArguments> {
        return if (arguments.wasm) {
            LegacyJsWasmPipelineAdapter(WasmCliPipeline(defaultPerformanceManager))
        } else {
            JsCliPipeline(defaultPerformanceManager)
        }
    }

    override fun createCompilerImpl(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        moduleName: String,
        outputName: String,
        outputDir: File,
        messageCollector: MessageCollector,
        performanceManager: PerformanceManager?,
    ): K2JsCompilerImplBase {
        return if (arguments.wasm) {
            val wasmArguments = arguments.toWasmArguments(messageCollector)
            K2WasmCompilerImpl(
                arguments = wasmArguments,
                configuration = configuration,
                moduleName = moduleName,
                outputName = outputName,
                outputDir = outputDir,
                messageCollector = messageCollector,
                performanceManager = performanceManager,
            )
        } else {
            K2JsCompilerImpl(
                arguments = arguments,
                configuration = configuration,
                moduleName = moduleName,
                outputName = outputName,
                outputDir = outputDir,
                messageCollector = messageCollector,
                performanceManager = performanceManager,
            )
        }
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        CommonJsConfigurationUpdater.setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
    }

    override fun initializeCommonConfiguration(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        rootDisposable: Disposable,
    ) {
        CommonJsConfigurationUpdater.initializeCommonConfiguration(configuration, arguments, rootDisposable)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JSCompiler(), args)
        }
    }
}

internal fun K2JSCompilerArguments.toWasmArguments(messageCollector: MessageCollector): KotlinWasmCompilerArguments {
    messageCollector.report(
        CompilerMessageSeverity.WARNING,
        "Wasm compilation should be done through `KotlinWasmCompiler`. Using Wasm related arguments with `K2JSCompiler` will become an error in a future compiler version."
    )
    return parseCommandLineArguments<KotlinWasmCompilerArguments>(toArgumentStrings()).also {
        it.errors?.unknownArgs?.forEach { error ->
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Argument applicable only to JS compilation ignored when converting K2JSArguments to KotlinWasmCompilerArguments: $error."
            )
        }
    }
}