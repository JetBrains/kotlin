/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibCliPipeline
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibIrCompilationArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.utils.KotlinPaths

/**
 * This class is the entry-point for compiling Kotlin code into a Klib with references to jars.
 *
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class K2JKlibCompiler : CLICompiler<K2JKlibCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun createArguments() = K2JKlibCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JKlibCompilerArguments,
        services: Services,
    ) {
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JKlibCompilerArguments) {}

    public override fun doExecute(
        arguments: K2JKlibCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode = error("K1 compiler entry point is no supported.")

    public override fun doExecutePhased(
        arguments: K2JKlibCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode = JKlibCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)

    /** Entry point used by J2CL to get the IR tree. */
    @Suppress("UNUSED")
    fun compileKlibAndDeserializeIr(
        arguments: K2JKlibCompilerArguments,
        messageCollector: MessageCollector,
        rootDisposable: Disposable,
    ): JKlibIrCompilationArtifact? {
        arguments.compileIr = true
        val result =
            JKlibCliPipeline(defaultPerformanceManager)
                .executeAndReturnPipeLineArtifact(
                    arguments,
                    Services.EMPTY,
                    messageCollector,
                    rootDisposable,
                )

        return if (result is JKlibIrCompilationArtifact) {
            result
        } else {
            check(result.exitCode != ExitCode.OK)
            // Error occurred during pipeline execution, let the caller handles the error gracefully.
            null
        }
    }

    override fun executableScriptFileName(): String = "kotlinc"

    public override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JKlibCompiler(), args)
        }
    }
}
