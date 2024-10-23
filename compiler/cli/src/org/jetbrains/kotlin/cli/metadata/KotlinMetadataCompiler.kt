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
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.K2MetadataConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.File

/**
 * This class is the entry-point for compiling Kotlin code into a metadata KLib.
 *
 * **Note: `2` in the name stands for Kotlin `TO` metadata compiler.
 * This entry-point used by both K1 and K2.**
 *
 * Please see `/docs/fir/k2_kmp.md` for more info on the K2/FIR implementation.
 */
class KotlinMetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {

    override val defaultPerformanceManager: CommonCompilerPerformanceManager = K2MetadataCompilerPerformanceManager()

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

        val commonSources = arguments.commonSources?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        if (hmppCliModuleStructure != null) {
            collector.report(ERROR, "HMPP module structure should not be passed during metadata compilation. Please remove `-Xfragments` and related flags")
            return ExitCode.COMPILATION_ERROR
        }
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources, hmppModuleName = null)
        }
        if (arguments.classpath != null) {
            configuration.addJvmClasspathRoots(arguments.classpath!!.split(File.pathSeparatorChar).map(::File))
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        configuration.putIfNotNull(K2MetadataConfigurationKeys.FRIEND_PATHS, arguments.friendPaths?.toList())
        configuration.putIfNotNull(K2MetadataConfigurationKeys.REFINES_PATHS, arguments.refinesPaths?.toList())


        val destination = arguments.destination
        if (destination != null) {
            if (destination.endsWith(".jar")) {
                // TODO: support .jar destination
                collector.report(
                    STRONG_WARNING,
                    ".jar destination is not yet supported, results will be written to the directory with the given name"
                )
            }
            configuration.put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, File(destination))
        }

        val environment =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)

        val mode = if (arguments.metadataKlib) "KLib" else "metadata"

        val sourceFiles = environment.getSourceFiles()
        performanceManager.notifyCompilerInitialized(sourceFiles.size, environment.countLinesOfCode(sourceFiles), "$mode mode for $moduleName module")

        if (environment.getSourceFiles().isEmpty()) {
            if (arguments.version) {
                return ExitCode.OK
            }
            collector.report(ERROR, "No source files")
            return ExitCode.COMPILATION_ERROR
        }

        checkKotlinPackageUsageForPsi(environment.configuration, environment.getSourceFiles())

        try {
            val useFir = configuration.getBoolean(CommonConfigurationKeys.USE_FIR)
            val metadataSerializer = when (useFir) {
                false -> when (arguments.metadataKlib) {
                    true -> K1MetadataKlibSerializer(configuration, environment)
                    false -> K1LegacyMetadataSerializer(configuration, environment, dependOnOldBuiltIns = true)
                }
                true -> when (arguments.legacyMetadataJar) {
                    true -> FirLegacyMetadataSerializer(configuration, environment)
                    false -> FirKlibMetadataSerializer(configuration, environment)
                }
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

    protected class K2MetadataCompilerPerformanceManager : CommonCompilerPerformanceManager("Kotlin to Metadata compiler")
}

@Deprecated("Use KotlinMetadataCompiler instead", level = DeprecationLevel.HIDDEN)
class K2MetadataCompiler : CLICompiler<K2MetadataCompilerArguments>() {
    private val delegate = KotlinMetadataCompiler()

    override val defaultPerformanceManager: CommonCompilerPerformanceManager
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