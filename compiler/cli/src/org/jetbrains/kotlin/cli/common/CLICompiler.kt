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

package org.jetbrains.kotlin.cli.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.INTERNAL_ERROR
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.PrintStream

abstract class CLICompiler<A : CommonCompilerArguments> : CLITool<A>() {

    protected abstract val performanceManager: CommonCompilerPerformanceManager

    // Used in CompilerRunnerUtil#invokeExecMethod, in Eclipse plugin (KotlinCLICompiler) and in kotlin-gradle-plugin (GradleCompilerRunner)
    fun execAndOutputXml(errStream: PrintStream, services: Services, vararg args: String): ExitCode {
        return exec(errStream, services, MessageRenderer.XML, args)
    }

    // Used via reflection in KotlinCompilerBaseTask
    fun execFullPathsInMessages(errStream: PrintStream, args: Array<String>): ExitCode {
        return exec(errStream, Services.EMPTY, MessageRenderer.PLAIN_FULL_PATHS, args)
    }

    public override fun execImpl(messageCollector: MessageCollector, services: Services, arguments: A): ExitCode {
        val performanceManager = performanceManager
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableCollectingPerformanceStatistics()
        }

        val configuration = CompilerConfiguration()

        val collector = GroupingMessageCollector(messageCollector, arguments.allWarningsAsErrors).also {
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, it)
        }

        configuration.put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
        try {
            setupCommonArguments(configuration, arguments)
            setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
            val paths = computeKotlinPaths(collector, arguments)
            if (collector.hasErrors()) {
                return ExitCode.COMPILATION_ERROR
            }

            val canceledStatus = services[CompilationCanceledStatus::class.java]
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)

            val rootDisposable = Disposer.newDisposable()
            try {
                setIdeaIoUseFallback()

                val code = doExecute(arguments, configuration, rootDisposable, paths)

                performanceManager.notifyCompilationFinished()
                if (arguments.reportPerf) {
                    performanceManager.getMeasurementResults()
                        .forEach { it -> configuration.get(MESSAGE_COLLECTOR_KEY)!!.report(INFO, "PERF: " + it.render(), null) }
                }

                if (arguments.dumpPerf != null) {
                    performanceManager.dumpPerformanceReport(File(arguments.dumpPerf!!))
                }

                return if (collector.hasErrors()) COMPILATION_ERROR else code
            } catch (e: CompilationCanceledException) {
                collector.report(INFO, "Compilation was canceled", null)
                return ExitCode.OK
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is CompilationCanceledException) {
                    collector.report(INFO, "Compilation was canceled", null)
                    return ExitCode.OK
                } else {
                    throw e
                }
            } finally {
                Disposer.dispose(rootDisposable)
            }
        } catch (e: AnalysisResult.CompilationErrorException) {
            return COMPILATION_ERROR
        } catch (t: Throwable) {
            MessageCollectorUtil.reportException(collector, t)
            return INTERNAL_ERROR
        } finally {
            collector.flush()
        }
    }

    private fun setupCommonArguments(configuration: CompilerConfiguration, arguments: A) {
        configuration.setupCommonArguments(arguments, this::createMetadataVersion)
    }

    protected abstract fun createMetadataVersion(versionArray: IntArray): BinaryVersion

    protected abstract fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: A, services: Services
    )

    protected abstract fun doExecute(
        arguments: A,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode

    protected fun commonLoadPlugins(
        configuration: CompilerConfiguration,
        paths: KotlinPaths? = null,
        passedPluginClasspaths: Iterable<String> = emptyList()
    ): Iterable<String> {
        var pluginClasspaths: Iterable<String> = passedPluginClasspaths
        val explicitOrLoadedScriptingPlugin =
            pluginClasspaths.any { File(it).name.startsWith(PathUtil.KOTLIN_SCRIPTING_COMPILER_PLUGIN_NAME) } ||
                    try {
                        PluginCliParser::class.java.classLoader.loadClass("org.jetbrains.kotlin.extensions.ScriptingCompilerConfigurationExtension")
                        true
                    } catch (_: Throwable) {
                        false
                    }
        // if scripting plugin is not enabled explicitly (probably from another path) and not in the classpath already,
        // try to find and enable it implicitly
        if (!explicitOrLoadedScriptingPlugin) {
            val kotlinPaths = paths ?: PathUtil.kotlinPathsForCompiler
            val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
            val (jars, missingJars) =
                PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.mapNotNull { File(libPath, it) }.partition { it.exists() }
            if (missingJars.isEmpty()) {
                pluginClasspaths = jars.map { it.canonicalPath } + pluginClasspaths
            } else {
                val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                messageCollector.report(
                    CompilerMessageSeverity.LOGGING,
                    "Scripting plugin will not be loaded: not all required jars are present in the classpath (missing files: $missingJars)"
                )
            }
        }

        return pluginClasspaths
    }
}

