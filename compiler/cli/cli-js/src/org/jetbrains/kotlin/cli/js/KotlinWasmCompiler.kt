/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.web.CommonWasmConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.web.WasmCliPipeline
import org.jetbrains.kotlin.cli.pipeline.web.WebCliPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

class KotlinWasmCompiler : KotlinJsCompilerBase<KotlinWasmCompilerArguments>() {
    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: KotlinWasmCompilerArguments,
        services: Services,
    ) {
        CommonWasmConfigurationUpdater.setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
    }

    override fun createArguments(): KotlinWasmCompilerArguments {
        return KotlinWasmCompilerArguments()
    }

    override val builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.WASM

    override fun createCliPipeline(arguments: KotlinWasmCompilerArguments): WebCliPipeline<KotlinWasmCompilerArguments> {
        return WasmCliPipeline(defaultPerformanceManager)
    }

    override fun createCompilerImpl(
        arguments: KotlinWasmCompilerArguments,
        configuration: CompilerConfiguration,
        moduleName: String,
        outputName: String,
        outputDir: File,
        messageCollector: MessageCollector,
        performanceManager: PerformanceManager?,
    ): K2JsCompilerImplBase = K2WasmCompilerImpl(
        arguments = arguments,
        configuration = configuration,
        moduleName = moduleName,
        outputName = outputName,
        outputDir = outputDir,
        messageCollector = messageCollector,
        performanceManager = performanceManager,
    )

    override fun initializeCommonConfiguration(
        configuration: CompilerConfiguration,
        arguments: KotlinWasmCompilerArguments,
        rootDisposable: Disposable,
    ) {
        CommonWasmConfigurationUpdater.initializeCommonConfiguration(configuration, arguments, rootDisposable)
    }
}