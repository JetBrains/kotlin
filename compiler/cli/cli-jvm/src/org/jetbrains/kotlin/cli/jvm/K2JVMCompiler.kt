/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_WARNING
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.common.profiling.ProfilingCompilerPerformanceManager
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.configureFromArgs
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun doExecutePhased(
        arguments: K2JVMCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        return JvmCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    override fun createArguments(): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
        if (System.getenv("KOTLIN_REPORT_PERF") != null) {
            reportPerf = true
        }
    }

    override fun executableScriptFileName(): String = "kotlinc-jvm"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JVMCompiler(), args)
        }

        @K1Deprecation
        fun createCoreEnvironment(
            rootDisposable: Disposable,
            configuration: CompilerConfiguration,
            targetDescription: String
        ): KotlinCoreEnvironment? {
            val perfManager = configuration.perfManager
            perfManager?.targetDescription = targetDescription

            if (CheckDiagnosticCollector.checkHasErrors(configuration)) return null

            val environment = KotlinCoreEnvironment.createForProduction(
                rootDisposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )

            val sourceFiles = environment.getSourceFiles()
            perfManager?.addSourcesStats(sourceFiles.size, environment.countLinesOfCode(sourceFiles))

            return if (CheckDiagnosticCollector.checkHasErrors(configuration)) null else environment
        }

        internal fun createCustomPerformanceManagerOrNull(
            arguments: K2JVMCompilerArguments,
            services: Services,
        ): PerformanceManager? {
            val externalManager = services[PerformanceManager::class.java]
            if (externalManager != null) return externalManager
            val argument = arguments.profileCompilerCommand ?: return null
            return ProfilingCompilerPerformanceManager.create(argument, arguments.detailedPerf)
        }
    }

    override fun createPerformanceManager(arguments: K2JVMCompilerArguments, services: Services): PerformanceManager {
        return createCustomPerformanceManagerOrNull(arguments, services) ?: defaultPerformanceManager
    }
}

fun CompilerConfiguration.configureModuleChunk(
    arguments: K2JVMCompilerArguments,
    buildFile: File?
): ModuleChunk {
    val destination = arguments.destination?.let { File(it) }

    return if (buildFile != null) {
        fun strongWarning(message: String) {
            this.report(COMPILER_ARGUMENTS_WARNING, message)
        }

        if (destination != null) {
            strongWarning("The '-d' option with a directory destination is ignored because '-Xbuild-file' is specified")
        }
        if (arguments.javaSourceRoots.isNotEmpty()) {
            strongWarning("The '-Xjava-source-roots' option is ignored because '-Xbuild-file' is specified")
        }
        if (arguments.javaPackagePrefix != null) {
            strongWarning("The '-Xjava-package-prefix' option is ignored because '-Xbuild-file' is specified")
        }
        configureContentRootsFromClassPath(arguments)
        put(JVMConfigurationKeys.MODULE_XML_FILE, buildFile)
        CompileEnvironmentUtil.loadModuleChunk(
            buildFile,
            { report(CliDiagnostics.JVM_CLI_ERROR, it) },
            { reportException(it) }
        )
    } else {
        if (destination != null) {
            if (destination.path.endsWith(".jar")) {
                put(JVMConfigurationKeys.OUTPUT_JAR, destination)
            } else {
                put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destination)
            }
        }

        val module = ModuleBuilder(
            this[CommonConfigurationKeys.MODULE_NAME] ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME,
            destination?.path ?: ".", "java-production"
        )
        module.configureFromArgs(arguments)

        ModuleChunk(listOf(module))
    }
}

internal fun ModuleChunk.targetDescription(): String {
    return modules
        .map { input -> input.getModuleName() + "-" + input.getModuleType() }
        .let { names -> names.singleOrNull() ?: names.joinToString() }
}

fun main(args: Array<String>) = K2JVMCompiler.main(args)
