/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.web.CommonWasmConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.web.WasmCliPipeline
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms

class KotlinWasmCompiler : KotlinWebCompilerBase<KotlinWasmCompilerArguments>() {
    override val platform: TargetPlatform = WasmPlatforms.unspecifiedWasmPlatform

    override fun executableScriptFileName(): String = "kotlinc-wasm"

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

    override fun doExecutePhased(
        arguments: KotlinWasmCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector
    ): ExitCode {
        return WasmCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(KotlinWasmCompiler(), args)
        }
    }
}