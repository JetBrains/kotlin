/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

class KotlinWasmCompiler : KotlinJsCompilerBase<K2WasmCompilerArguments>() {
    override fun createArguments(): K2WasmCompilerArguments {
        return K2WasmCompilerArguments()
    }

    override fun createCompilerImpl(
        arguments: K2WasmCompilerArguments,
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
}