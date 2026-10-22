/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jklib.pipeline.JKlibCliPipeline
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifactWithExitCode
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

/**
 * This class is the entry-point for compiling Kotlin code into a Klib with references to jars.
 *
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class K2JKlibCompiler : CLICompiler<K2JKlibCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun createArguments(): K2JKlibCompilerArguments = K2JKlibCompilerArguments()

    public override fun doExecutePhased(
        arguments: K2JKlibCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode = JKlibCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)

    /**
     * Entry point used by J2CL to get the IR tree.
     *
     * Returns `null` if the compilation failed, in which case the errors have been reported to [messageCollector].
     */
    @Suppress("UNUSED")
    fun compileToIr(
        arguments: K2JKlibCompilerArguments,
        messageCollector: MessageCollector,
        rootDisposable: Disposable,
    ): IrCompilationResult? {
        arguments.compileIr = true
        val result =
            JKlibCliPipeline(defaultPerformanceManager)
                .executeAndReturnPipeLineArtifact(
                    arguments,
                    Services.EMPTY,
                    messageCollector,
                    rootDisposable,
                )

        return if (result is IrCompilationResult) {
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

/** The IR of a module compiled by [K2JKlibCompiler.compileToIr]. */
data class IrCompilationResult(
    val moduleFragment: IrModuleFragment,
    val pluginContext: IrPluginContext,
    override val configuration: CompilerConfiguration,
    override val exitCode: ExitCode = ExitCode.OK,
) : PipelineArtifactWithExitCode() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}
