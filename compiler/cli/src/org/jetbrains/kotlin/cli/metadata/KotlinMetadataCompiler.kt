/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataCliPipeline
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataConfigurationUpdater
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.KotlinPaths

/**
 * This class is the entry-point for compiling Kotlin code into a metadata KLib.
 *
 * **Note: `2` in the name stands for Kotlin `TO` metadata compiler.
 * This entry-point used by both K1 and K2.**
 *
 * Please see `/docs/fir/k2_kmp.md` for more info on the K2/FIR implementation.
 */
class KotlinMetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {

    override fun doExecutePhased(
        arguments: K2MetadataCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode? {
        val pipeline = MetadataCliPipeline(defaultPerformanceManager)
        return pipeline.execute(arguments, services, basicMessageCollector)
    }

    override val defaultPerformanceManager: PerformanceManager = K2MetadataCompilerPerformanceManager()

    override fun createArguments() = K2MetadataCompilerArguments()

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: K2MetadataCompilerArguments, services: Services
    ) {
        // No specific arguments yet
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2MetadataCompilerArguments) {}

    public override fun doExecute(
        arguments: K2MetadataCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val collector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val performanceManager = configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration, rootDisposable)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult
        MetadataConfigurationUpdater.fillConfiguration(configuration, arguments)

        val moduleName = configuration.moduleName
        val environment =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)

        val mode = if (arguments.metadataKlib) "KLib" else "metadata"

        val sourceFiles = environment.getSourceFiles()
        performanceManager.apply {
            targetDescription = "$mode mode for $moduleName module"
            addSourcesStats(sourceFiles.size, environment.countLinesOfCode(sourceFiles))
        }

        if (environment.getSourceFiles().isEmpty()) {
            if (arguments.version) {
                return ExitCode.OK
            }
            collector.report(ERROR, "No source files")
            return ExitCode.COMPILATION_ERROR
        }

        checkKotlinPackageUsageForPsi(environment.configuration, environment.getSourceFiles())

        try {
            val metadataSerializer = when (arguments.metadataKlib) {
                true -> K1MetadataKlibSerializer(configuration, environment)
                false -> K1LegacyMetadataSerializer(configuration, environment, dependOnOldBuiltIns = true)
            }
            metadataSerializer.analyzeAndSerialize()
        } catch (e: CompilationException) {
            collector.report(EXCEPTION, OutputMessageUtil.renderException(e), MessageUtil.psiElementToMessageLocation(e.element))
            return ExitCode.INTERNAL_ERROR
        }

        return ExitCode.OK
    }

    // TODO: update this once a launcher script for K2MetadataCompiler is available
    override fun executableScriptFileName(): String = "kotlinc"

    public override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = BuiltInsBinaryVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(KotlinMetadataCompiler(), args)
        }
    }

    protected class K2MetadataCompilerPerformanceManager : PerformanceManager("Kotlin to Metadata compiler")
}

@Deprecated("Use KotlinMetadataCompiler instead", level = DeprecationLevel.HIDDEN)
class K2MetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {
    private val delegate = KotlinMetadataCompiler()

    override val defaultPerformanceManager: PerformanceManager
        get() = delegate.defaultPerformanceManager

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return delegate.createMetadataVersion(versionArray)
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2MetadataCompilerArguments,
        services: Services,
    ) {
        delegate.defaultPerformanceManager
    }

    override fun doExecute(
        arguments: K2MetadataCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode {
        return delegate.doExecute(arguments, configuration, rootDisposable, paths)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2MetadataCompilerArguments) {}

    override fun createArguments(): K2MetadataCompilerArguments {
        return delegate.createArguments()
    }

    override fun executableScriptFileName(): String {
        return delegate.executableScriptFileName()
    }
}
