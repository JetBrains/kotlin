/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.DAEMON_RUN_DIR_PATH
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_FILE_COUNT_LIMIT
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_FILE_SIZE_LIMIT
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.LOGS_PATH
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl.Companion.SHUTDOWN_DELAY_MILLIS
import org.jetbrains.kotlin.buildtools.internal.arguments.*
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl.Companion.WERROR
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.trackers.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.internal.trackers.ImportTrackerAdapter
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.rmi.RemoteException

internal abstract class BaseCompilationOperationImpl<BtaCompilerArgs : CommonCompilerArgumentsImpl, CompilerArgs : CommonCompilerArguments>(
    override val compilerArguments: BtaCompilerArgs,
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : CancellableBuildOperationImpl<CompilationResult>(), BaseCompilationOperation, BaseCompilationOperation.Builder {

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseCompilationOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    override fun executeCancellableImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): CompilationResult {
        val compilerMessageRenderer = this[COMPILER_MESSAGE_RENDERER]
        val kotlinLogger = logger ?: DefaultKotlinLogger
        compilerArguments.reportRestrictedViolations(kotlinLogger)
        if (compilerArguments.hasValidationErrors()) {
            compilerArguments.reportValidationErrors(kotlinLogger)
            return CompilationResult.COMPILATION_ERROR
        }
        val loggerAdapter = KotlinLoggerMessageCollectorAdapter(kotlinLogger, compilerMessageRenderer, compilerArguments[WERROR])

        return when (executionPolicy) {
            InProcessExecutionPolicyImpl -> {
                compileInProcess(loggerAdapter)
            }
            is DaemonExecutionPolicyImpl -> {
                compileWithDaemon(projectId, executionPolicy, loggerAdapter)
            }
            else -> {
                CompilationResult.COMPILATION_ERROR.also {
                    loggerAdapter.kotlinLogger.error("Unknown execution mode: ${executionPolicy::class.qualifiedName}")
                }
            }
        }
    }

    abstract val targetPlatform: CompileService.TargetPlatform
    open val ktsExtensionsAsArray: Array<String>? = null

    abstract fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: CompilerArgs,
    ): IncrementalCompilationOptions?

    private fun toDaemonCompilationOptions(isDebugLoggingEnabled: Boolean, arguments: CompilerArgs): CompilationOptions {
        // TODO: KT-79976 automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportCategories = buildList {
            add(ReportCategory.COMPILER_MESSAGE.code)
            if (get(LOOKUP_TRACKER) != null) {
                add(ReportCategory.COMPILER_LOOKUP.code)
            }
        }.toTypedArray()

        val reportSeverity = if (VERBOSE in compilerArguments && compilerArguments[VERBOSE]) {
            ReportSeverity.DEBUG.code
        } else {
            ReportSeverity.INFO.code
        }

        val requestedCompilationResults = listOfNotNull(
            CompilationResultCategory.IC_COMPILE_ITERATION.code,
            CompilationResultCategory.BUILD_METRICS.code.takeIf { this[METRICS_COLLECTOR] != null || this[XX_KGP_METRICS_COLLECTOR] },
            // Daemon would report log lines only if debug logging is enabled or metrics are requested
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code.takeIf { this[METRICS_COLLECTOR] != null || this[XX_KGP_METRICS_COLLECTOR] || isDebugLoggingEnabled },
        ).toTypedArray()

        return getIcOptionsOrNull(reportCategories, reportSeverity, requestedCompilationResults, arguments)
            ?: CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = targetPlatform,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
                generateCompilerRefIndex = this[GENERATE_COMPILER_REF_INDEX],
            )
    }

    private fun compileWithDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the daemon strategy")
        val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
        val sessionIsAliveFlagFile = buildIdToSessionFlagFile.computeIfAbsent(projectId) {
            createSessionIsAliveFlagFile()
        }

        val daemonLogOptions = DaemonLogOptions(
            logsPath = executionPolicy[LOGS_PATH].absolutePathStringOrThrow(),
            logsFileSizeLimit = executionPolicy[LOGS_FILE_SIZE_LIMIT] ?: 0,
            logsFileCountLimit = executionPolicy[LOGS_FILE_COUNT_LIMIT] ?: Int.MAX_VALUE,
        )
        Files.createDirectories(executionPolicy[LOGS_PATH])

        val additionalJvmArguments = mutableListOf<String>()
        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                executionPolicy[SHUTDOWN_DELAY_MILLIS]?.let { shutdownDelay ->
                    shutdownDelayMilliseconds = shutdownDelay
                }

                runFilesPath = executionPolicy[DAEMON_RUN_DIR_PATH].absolutePathStringOrThrow()
                additionalJvmArguments += "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=$runFilesPath"
            })

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        ).also { opts ->
            val effectiveJvmArguments = additionalJvmArguments + (executionPolicy[JVM_ARGUMENTS] ?: emptyList())
            if (effectiveJvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    effectiveJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }

        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            loggerAdapter,
            loggerAdapter.kotlinLogger.isDebugEnabled || System.getProperty("kotlin.daemon.debug.log")?.toBooleanStrictOrNull() ?: true,
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions,
            daemonLogOptions = daemonLogOptions,
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult
        onCancel {
            daemon.cancelCompilation(sessionId, compilationId)
        }
        if (loggerAdapter.kotlinLogger.isDebugEnabled) {
            daemon.getDaemonJVMOptions().takeIf { it.isGood }?.let { jvmOpts ->
                loggerAdapter.kotlinLogger.debug("Kotlin compile daemon JVM options: ${jvmOpts.get().mappers.flatMap { it.toArgs("-") }}")
            }
        }

        val arguments = createAndPrepareCompilerArguments()
        arguments.addSources()
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val rootProjectDir = getRootProjectDir()
        val daemonCompileOptions = toDaemonCompilationOptions(loggerAdapter.kotlinLogger.isDebugEnabled, arguments)
        loggerAdapter.kotlinLogger.info("Options for KOTLIN DAEMON: $daemonCompileOptions")

        val metricsReporter = getMetricsReporter()
        val exitCode = daemon.compile(
            sessionId,
            arguments.toArgumentStrings(allowArgFileInValues = false).toTypedArray(),
            daemonCompileOptions,
            BtaCompilerServicesWithResultsFacade(loggerAdapter, get(LOOKUP_TRACKER)),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger, rootProjectDir?.toFile(), metricsReporter
            ),
            compilationId
        ).get()

        try {
            daemon.releaseCompileSession(sessionId)
        } catch (e: RemoteException) {
            loggerAdapter.kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
        }

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult.also {
            populateMetricsCollector(metricsReporter)
        }
    }

    protected fun populateMetricsCollector(metricsReporter: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>) {
        if (this[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
            this[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                ObjectOutputStream(this).writeObject(metricsReporter)
            }.toByteArray()
        }
    }

    abstract fun getRootProjectDir(): Path?

    abstract fun createAndPrepareCompilerArguments(): CompilerArgs

    private fun getCurrentClasspath() =
        (JvmCompilationOperationImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

    abstract fun shouldCompileIncrementally(): Boolean

    protected open fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        val arguments = createAndPrepareCompilerArguments()

        return if (shouldCompileIncrementally()) {
            compileIncrementallyInProcess(arguments, loggerAdapter)
        } else {
            compileInProcessWithoutIc(arguments, loggerAdapter)
        }
    }

    abstract fun compileIncrementallyInProcess(
        arguments: CompilerArgs,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult

    abstract fun createCompiler(): CLICompiler<CompilerArgs>

    abstract fun CompilerArgs.addSources()

    private fun compileInProcessWithoutIc(
        arguments: CompilerArgs,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        val compiler = createCompiler()
        arguments.addSources()
        val services = Services.Builder().apply {
            register(CompilationCanceledStatus::class.java, cancellationHandle)
            get(LOOKUP_TRACKER)?.let { tracker: CompilerLookupTracker ->
                register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
            }
            get(IMPORT_TRACKER)?.let { tracker: CompilerImportTracker ->
                register(ImportTracker::class.java, ImportTrackerAdapter(tracker))
            }
        }.build()
        logCompilerArguments(loggerAdapter, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val compilationResult = compiler.exec(loggerAdapter, services, arguments).asCompilationResult
        metricsReporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats)
        metricsReporter.addMetric(COMPILE_ITERATION, 1) // in non-IC case there's always 1 iteration
        metricsReporter.endMeasureGc()

        populateMetricsCollector(metricsReporter)

        return compilationResult
    }

    protected fun getLookupTrackerAdapter(): LookupTracker = this[LOOKUP_TRACKER]?.let { tracker ->
        LookupTrackerAdapter(tracker)
    } ?: LookupTracker.DO_NOTHING

    protected fun logCompilerArguments(
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
        arguments: CompilerArgs,
        argumentsLogLevel: CompilerArgumentsLogLevel,
    ) {
        with(loggerAdapter.kotlinLogger) {
            val message = "Kotlin compiler args: ${arguments.toArgumentStrings().joinToString(" ")}"
            when (argumentsLogLevel) {
                CompilerArgumentsLogLevel.ERROR -> error(message)
                CompilerArgumentsLogLevel.WARNING -> warn(message)
                CompilerArgumentsLogLevel.INFO -> info(message)
                CompilerArgumentsLogLevel.DEBUG -> debug(message)
            }
        }
    }

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", null)

        /*
        * Tracks imports during compilation.
        * This option partially addresses [KT-84450](https://youtrack.jetbrains.com/issue/KT-84450)
        * and is not intended to work in all cases for now.
        * */
        val IMPORT_TRACKER: Option<CompilerImportTracker?> = Option("IMPORT_TRACKER", null)

        val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> =
            Option("COMPILER_ARGUMENTS_LOG_LEVEL", default = CompilerArgumentsLogLevel.DEBUG)

        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER", default = DefaultCompilerMessageRenderer)

        val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX", false)

    }
}

private class BtaCompilerServicesWithResultsFacade(
    loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    val lookupTracker: CompilerLookupTracker? = null,
) :
    BasicCompilerServicesWithResultsFacadeServer(loggerAdapter) {
    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        when (category) {
            ReportCategory.COMPILER_LOOKUP.code -> {
                attachment as LookupInfo?
                if (attachment == null) {
                    lookupTracker?.clear()
                } else {
                    lookupTracker?.recordLookup(
                        attachment.filePath,
                        attachment.scopeFqName,
                        CompilerLookupTracker.ScopeKind.valueOf(attachment.scopeKind.name),
                        attachment.name
                    )
                }
            }
            else -> super.report(category, severity, message, attachment)
        }
    }
}
