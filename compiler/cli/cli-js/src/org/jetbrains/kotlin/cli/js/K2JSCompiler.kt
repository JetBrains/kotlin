/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.cli.CliDiagnostics.WEB_ARGUMENT_WARNING
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyK2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.web.CommonJsConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.web.JsCliPipeline
import org.jetbrains.kotlin.cli.pipeline.web.WasmCliPipeline
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms

class K2JSCompiler : KotlinWebCompilerBase<K2JSCompilerArguments>() {
    override val platform: TargetPlatform = JsPlatforms.defaultJsPlatform
    override fun executableScriptFileName(): String = "kotlinc-js"

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    override fun doExecutePhased(
        arguments: K2JSCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        @Suppress("DEPRECATION")
        return if (arguments.wasm) {
            WasmCliPipeline(defaultPerformanceManager).execute(arguments.toWasmArguments(), services, basicMessageCollector)
        } else {
            JsCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
        }
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        CommonJsConfigurationUpdater.setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JSCompiler(), args)
        }
    }
}

private fun K2JSCompilerArguments.toWasmArguments(): KotlinWasmCompilerArguments {
    return copyK2JSCompilerArguments(this, KotlinWasmCompilerArguments())
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
    configuration: CompilerConfiguration
): RuntimeDiagnostic? = when (value?.lowercase()) {
    RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        configuration.report(WEB_ARGUMENT_WARNING, "Unknown runtime diagnostic '$value'")
        null
    }
}
