/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.native.pipeline.NativeKlibCliPipeline
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.utils.KotlinPaths

/**
 * CLI compiler for producing Native klibs.
 *
 * This compiler produces Kotlin/Native klib artifacts without requiring the full
 * Native backend infrastructure from kotlin-native/backend.native.
 *
 * It uses [NativeKlibCliPipeline] for the phased compilation pipeline.
 */
class K2NativeKlibCompiler : CLICompiler<K2NativeKlibCompilerArguments>() {

    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform

    override fun createArguments(): K2NativeKlibCompilerArguments = K2NativeKlibCompilerArguments()

    override fun doExecutePhased(
        arguments: K2NativeKlibCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode? {
        return NativeKlibCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    override fun doExecute(
        arguments: K2NativeKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode {
        // K1 compilation is not supported for this compiler.
        // All compilation goes through doExecutePhased().
        throw UnsupportedOperationException(
            "K2NativeKlibCompiler does not support K1 compilation. Use language version 2.0 or higher."
        )
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2NativeKlibCompilerArguments,
        services: Services,
    ) {
        // Platform-specific configuration is handled in NativeKlibConfigurationPhase
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeKlibCompilerArguments) {
        // No platform-specific scripting options
    }

    override fun executableScriptFileName(): String = "kotlinc-native-klib"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2NativeKlibCompiler(), args)
        }
    }
}
