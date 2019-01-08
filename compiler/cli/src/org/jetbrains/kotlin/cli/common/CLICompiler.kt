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
import kotlin.collections.*
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.KotlinPathsFromHomeDir
import org.jetbrains.kotlin.utils.PathUtil

import java.io.File
import java.io.PrintStream

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.INTERNAL_ERROR
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*

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

        val groupingCollector = GroupingMessageCollector(messageCollector, arguments.allWarningsAsErrors)

        val configuration = CompilerConfiguration()
        configuration.put(MESSAGE_COLLECTOR_KEY, groupingCollector)
        configuration.put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
        try {
            setupCommonArguments(configuration, arguments)
            setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
            val paths = computeKotlinPaths(groupingCollector, arguments)
            if (groupingCollector.hasErrors()) {
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

                return if (groupingCollector.hasErrors()) COMPILATION_ERROR else code
            } catch (e: CompilationCanceledException) {
                messageCollector.report(INFO, "Compilation was canceled", null)
                return ExitCode.OK
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is CompilationCanceledException) {
                    messageCollector.report(INFO, "Compilation was canceled", null)
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
            MessageCollectorUtil.reportException(groupingCollector, t)
            return INTERNAL_ERROR
        } finally {
            groupingCollector.flush()
        }
    }

    private fun setupCommonArguments(configuration: CompilerConfiguration, arguments: A) {
        if (arguments.noInline) {
            configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true)
        }
        if (arguments.intellijPluginRoot != null) {
            configuration.put(CLIConfigurationKeys.INTELLIJ_PLUGIN_ROOT, arguments.intellijPluginRoot!!)
        }
        if (arguments.reportOutputFiles) {
            configuration.put(CommonConfigurationKeys.REPORT_OUTPUT_FILES, true)
        }

        val metadataVersionString = arguments.metadataVersion
        if (metadataVersionString != null) {
            val versionArray = BinaryVersion.parseVersionArray(metadataVersionString)
            if (versionArray == null) {
                configuration.getNotNull(MESSAGE_COLLECTOR_KEY).report(ERROR, "Invalid metadata version: $metadataVersionString", null)
            } else {
                configuration.put(CommonConfigurationKeys.METADATA_VERSION, createMetadataVersion(versionArray))
            }
        }

        setupLanguageVersionSettings(configuration, arguments)

        configuration.put(CommonConfigurationKeys.LIST_PHASES, arguments.listPhases)
        if (arguments.disablePhases != null) {
            configuration.put(CommonConfigurationKeys.DISABLED_PHASES, setOf(*arguments.disablePhases!!))
        }
        if (arguments.verbosePhases != null) {
            configuration.put(CommonConfigurationKeys.VERBOSE_PHASES, setOf(*arguments.verbosePhases!!))
        }
        if (arguments.phasesToDumpBefore != null) {
            configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_BEFORE, setOf(*arguments.phasesToDumpBefore!!))
        }
        if (arguments.phasesToDumpAfter != null) {
            configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_AFTER, setOf(*arguments.phasesToDumpAfter!!))
        }
        if (arguments.phasesToDump != null) {
            configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE, setOf(*arguments.phasesToDump!!))
        }
        configuration.put(CommonConfigurationKeys.PROFILE_PHASES, arguments.profilePhases)
    }

    protected abstract fun createMetadataVersion(versionArray: IntArray): BinaryVersion

    private fun setupLanguageVersionSettings(configuration: CompilerConfiguration, arguments: A) {
        configuration.languageVersionSettings = arguments.configureLanguageVersionSettings(configuration.getNotNull(MESSAGE_COLLECTOR_KEY))
    }

    protected abstract fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration, arguments: A, services: Services
    )

    protected abstract fun doExecute(
        arguments: A,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode

    companion object {

        var KOTLIN_HOME_PROPERTY = "kotlin.home"

        private fun computeKotlinPaths(messageCollector: MessageCollector, arguments: CommonCompilerArguments): KotlinPaths? {
            val paths: KotlinPaths?
            val kotlinHomeProperty = System.getProperty(KOTLIN_HOME_PROPERTY)
            val kotlinHome = if (arguments.kotlinHome != null)
                File(arguments.kotlinHome!!)
            else if (kotlinHomeProperty != null)
                File(kotlinHomeProperty)
            else
                null
            if (kotlinHome != null) {
                if (kotlinHome.isDirectory) {
                    paths = KotlinPathsFromHomeDir(kotlinHome)
                } else {
                    messageCollector.report(ERROR, "Kotlin home does not exist or is not a directory: $kotlinHome", null)
                    paths = null
                }
            } else {
                paths = PathUtil.kotlinPathsForCompiler
            }

            if (paths != null) {
                messageCollector.report(LOGGING, "Using Kotlin home directory " + paths.homePath, null)
            }

            return paths
        }

        fun getLibraryFromHome(
            paths: KotlinPaths?,
            getLibrary: Function1<KotlinPaths, File>,
            libraryName: String,
            messageCollector: MessageCollector,
            noLibraryArgument: String
        ): File? {
            if (paths != null) {
                val stdlibJar = getLibrary.invoke(paths)
                if (stdlibJar.exists()) {
                    return stdlibJar
                }
            }

            messageCollector.report(
                STRONG_WARNING, "Unable to find " + libraryName + " in the Kotlin home directory. " +
                        "Pass either " + noLibraryArgument + " to prevent adding it to the classpath, " +
                        "or the correct '-kotlin-home'", null
            )
            return null
        }
    }
}
