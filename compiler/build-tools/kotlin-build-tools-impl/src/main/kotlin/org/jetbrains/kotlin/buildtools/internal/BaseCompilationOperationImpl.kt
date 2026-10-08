/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.reportPerformanceData
import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.arguments.*
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.internal.arguments.CommonToolArgumentsImpl.Companion.WERROR
import org.jetbrains.kotlin.buildtools.internal.serializability.BtaSerializable
import org.jetbrains.kotlin.buildtools.internal.serializability.CompilationResultSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.beforeSerialization
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.trackers.LookupTrackerAdapter
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId
import org.jetbrains.kotlin.cli.jvm.plugins.PluginsLoader
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Path

@kotlinx.serialization.Serializable
internal abstract class BaseCompilationOperationImpl<BtaCompilerArgs : CommonCompilerArgumentsImpl, CompilerArgs : CommonCompilerArguments>() :
    CancellableBuildOperationImpl<CompilationResult>(), BaseCompilationOperation, BaseCompilationOperation.Builder {

    abstract override val compilerArguments: BtaCompilerArgs

    override fun getResultSerializer(): KSerializer<CompilationResult> {
        return CompilationResultSerializer
    }

    override fun afterSerialization(messageReporter: (Message) -> Unit) {
        super.afterSerialization(messageReporter)
        if (hasLookupTracker) {
            lookupTracker = MessageReportingCompilerLookupTracker(messageReporter)
        }
        compilerMessageCollector = MessageReportingMessageCollectorWithDiagnosticId(messageReporter)
    }

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> {
        val messageVisitors = super.beforeSerialization(logger).toMutableList()
        lookupTracker?.let { messageVisitors.add(LookupMessageVisitor(it)) }
        messageVisitors.add(
            CompilerMessageVisitor(
                KotlinLoggerMessageCollectorAdapter(
                    logger,
                    compilerMessageRenderer,
                    compilerArguments[WERROR]
                )
            )
        )
        return messageVisitors
    }

    @Transient
    @SerialName("LOOKUP_TRACKER")
    var lookupTracker: CompilerLookupTracker? = null
        set(value) {
            hasLookupTracker = value != null
            field = value
        }

    @SerialName("HAS_LOOKUP_TRACKER")
    private var hasLookupTracker: Boolean = false

    @SerialName("COMPILER_ARGUMENTS_LOG_LEVEL")
    internal var compilerArgumentsLogLevel: CompilerArgumentsLogLevel = DEBUG

    @SerialName("COMPILER_MESSAGE_RENDERER")
    @Transient
    override var compilerMessageRenderer: CompilerMessageRenderer = DefaultCompilerMessageRenderer

    @SerialName("GENERATE_COMPILER_REF_INDEX")
    internal var generateCompilerRefIndex: Boolean = false

    override val warningsAsError: Boolean
        get() = compilerArguments[WERROR]

    @Transient
    private var compilerMessageCollector: MessageCollectorWithDiagnosticId? = null

    internal fun copyFrom(from: BaseCompilationOperationImpl<BtaCompilerArgs, CompilerArgs>) {
        super.copyFrom(from)
        lookupTracker = from.lookupTracker
        compilerArgumentsLogLevel = from.compilerArgumentsLogLevel
        compilerMessageRenderer = from.compilerMessageRenderer
        generateCompilerRefIndex = from.generateCompilerRefIndex
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseCompilationOperation.Option<V>): V =
        BaseCompilationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    // In-process compilation and linking run a CLICompiler, which creates and uses the shared application environment.
    override val usesApplicationEnvironment: Boolean
        get() = true

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseCompilationOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        BaseCompilationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    operator fun <V> get(key: Option<V>): V = BaseCompilationOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        BaseCompilationOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)


    override fun executeInDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        logger: KotlinLogger,
        executionContext: ExecutionContext
    ): CompilationResult {
        val messageCollector = createMessageCollector(logger)
        if (reportArgumentWarningsAndErrors(logger, messageCollector)) return COMPILATION_ERROR
        val hasArgumentParsingErrors = messageCollector.hasErrors()
        val result = super.executeInDaemon(projectId, executionPolicy, logger, executionContext)
        return if (hasArgumentParsingErrors && result == COMPILATION_SUCCESS) {
            COMPILATION_ERROR
        } else {
            result
        }
    }

    override fun executeCancellableImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): CompilationResult {
        val kotlinLogger = logger ?: DefaultKotlinLogger
        val messageCollector = compilerMessageCollector ?: createMessageCollector(kotlinLogger)

        if (reportArgumentWarningsAndErrors(kotlinLogger, messageCollector)) return COMPILATION_ERROR
        val hasArgumentParsingErrors = messageCollector.hasErrors()
        val result = when (executionPolicy) {
            InProcessExecutionPolicyImpl -> {
                compileInProcess(kotlinLogger, messageCollector, executionContext)
            }
            is DaemonExecutionPolicyImpl -> {
                compileWithDaemon(executionPolicy, kotlinLogger, messageCollector, executionContext)
            }
            else -> {
                CompilationResult.COMPILATION_ERROR.also {
                    kotlinLogger.error("Unknown execution mode: ${executionPolicy::class.qualifiedName}")
                }
            }
        }
        return if (hasArgumentParsingErrors && result == COMPILATION_SUCCESS) {
            COMPILATION_ERROR
        } else {
            result
        }
    }

    private fun createMessageCollector(kotlinLogger: KotlinLogger): KotlinLoggerMessageCollectorAdapter =
        KotlinLoggerMessageCollectorAdapter(kotlinLogger, compilerMessageRenderer, compilerArguments[WERROR])

    private fun reportArgumentWarningsAndErrors(
        kotlinLogger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
    ): Boolean {
        compilerArguments.reportRestrictedViolations(kotlinLogger)
        if (compilerArguments.hasValidationErrors()) {
            compilerArguments.reportValidationErrors(kotlinLogger)
            return true
        }
        compilerArguments.reportArgumentParseWarnings(messageCollector, createAndPrepareCompilerArguments())
        return false
    }

    abstract val targetPlatform: CompileService.TargetPlatform
    open val ktsExtensionsAsArray: Array<String>? = null

    abstract fun getIcOptionsOrNull(
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        arguments: CompilerArgs,
    ): CompilationOptions?

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
                compilerMode = NON_INCREMENTAL_COMPILER,
                targetPlatform = targetPlatform,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
                generateCompilerRefIndex = this[GENERATE_COMPILER_REF_INDEX],
            )
    }

    private fun compileWithDaemon(
        executionPolicy: DaemonExecutionPolicyImpl,
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
    ): CompilationResult {
        logger.debug("Compiling using the daemon strategy")
        (val daemon = compileService, val sessionId) = executionContext.daemonConnectionRegistry.getCompileServiceSession(
            executionPolicy,
            logger,
            messageCollector,
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult

        onCancel {
            daemon.cancelCompilation(sessionId, compilationId)
        }

        val arguments = createAndPrepareCompilerArguments()
        arguments.addSources()
        logCompilerArguments(logger, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))

        val rootProjectDir = getRootProjectDir()
        val daemonCompileOptions = toDaemonCompilationOptions(logger.isDebugEnabled, arguments)
        logger.info("Options for KOTLIN DAEMON: $daemonCompileOptions")

        val metricsReporter = getMetricsReporter()
        val memoryUsageBeforeBuild = daemon.getUsedMemory(withGC = false).takeIf { it.isGood }?.get()

        val exitCode = try {
            daemon.compile(
                sessionId,
                arguments.toArgumentStrings(allowArgFileInValues = false).toTypedArray(),
                daemonCompileOptions,
                createCompilerServicesFacade(logger, messageCollector),
                DaemonCompilationResults(
                    logger, rootProjectDir?.toFile(), metricsReporter
                ),
                compilationId
            ).get()
        } finally {
            val memoryUsageAfterBuild = runCatching { daemon.getUsedMemory(withGC = false).takeIf { it.isGood }?.get() }.getOrNull()

            if (memoryUsageAfterBuild == null || memoryUsageBeforeBuild == null) {
                logger.debug("Unable to calculate memory usage")
            } else {
                metricsReporter.addMetric(DAEMON_INCREASED_MEMORY, memoryUsageAfterBuild - memoryUsageBeforeBuild)
                metricsReporter.addMetric(DAEMON_MEMORY_USAGE, memoryUsageAfterBuild)
            }
        }

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult.also {
            populateMetricsCollector(metricsReporter)
        }
    }

    protected open fun createCompilerServicesFacade(
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
    ): CompilerServicesFacadeBase =
        BaseCompilerServicesWithResultsFacade(messageCollector, get(LOOKUP_TRACKER))

    protected fun populateMetricsCollector(metricsReporter: BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric>) {
        if (this[XX_KGP_METRICS_COLLECTOR] && metricsReporter is BuildMetricsReporterImpl) {
            this[XX_KGP_METRICS_COLLECTOR_OUT] = ByteArrayOutputStream().apply {
                ObjectOutputStream(this).writeObject(metricsReporter)
            }.toByteArray()
        }
    }

    abstract fun getRootProjectDir(): Path?

    abstract fun createAndPrepareCompilerArguments(): CompilerArgs

    abstract fun shouldCompileIncrementally(): Boolean

    protected open fun compileInProcess(
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
    ): CompilationResult {
        logger.debug("Compiling using the in-process strategy")
        val arguments = createAndPrepareCompilerArguments()

        return if (shouldCompileIncrementally()) {
            compileIncrementallyInProcess(arguments, logger, messageCollector, executionContext)
        } else {
            compileInProcessWithoutIc(arguments, logger, messageCollector, executionContext)
        }
    }

    abstract fun compileIncrementallyInProcess(
        arguments: CompilerArgs,
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
    ): CompilationResult

    abstract fun createCompiler(): CLICompiler<CompilerArgs>

    abstract fun CompilerArgs.addSources()

    private fun compileInProcessWithoutIc(
        arguments: CompilerArgs,
        logger: KotlinLogger,
        messageCollector: MessageCollectorWithDiagnosticId,
        executionContext: ExecutionContext,
    ): CompilationResult {
        val compiler = createCompiler()
        arguments.addSources()
        val services = Services.Builder().apply {
            register(CompilationCanceledStatus::class.java, cancellationHandle)
            get(LOOKUP_TRACKER)?.let { tracker: CompilerLookupTracker ->
                register(LookupTracker::class.java, LookupTrackerAdapter(tracker))
            }
            executionContext.classloadersCache?.let { register(PluginsLoader::class.java, it.asPluginsLoader()) }
            registerPlatformServices(logger)
        }.build()
        logCompilerArguments(logger, arguments, get(COMPILER_ARGUMENTS_LOG_LEVEL))
        val metricsReporter = getMetricsReporter()
        metricsReporter.startMeasureGc()
        val compilationResult = compiler.exec(messageCollector, services, arguments).asCompilationResult
        metricsReporter.reportPerformanceData(compiler.defaultPerformanceManager.unitStats)
        metricsReporter.addMetric(COMPILE_ITERATION, 1) // in non-IC case there's always 1 iteration
        metricsReporter.endMeasureGc()

        populateMetricsCollector(metricsReporter)

        return compilationResult
    }

    protected open fun Services.Builder.registerPlatformServices(logger: KotlinLogger) {}

    protected fun getLookupTrackerAdapter(): LookupTracker = this[LOOKUP_TRACKER]?.let { tracker ->
        LookupTrackerAdapter(tracker)
    } ?: DO_NOTHING

    protected fun logCompilerArguments(
        logger: KotlinLogger,
        arguments: CompilerArgs,
        argumentsLogLevel: CompilerArgumentsLogLevel,
    ) {
        with(logger) {
            val message = "Kotlin compiler args: ${arguments.toArgumentStrings().joinToString(" ")}"
            when (argumentsLogLevel) {
                ERROR -> error(message)
                WARNING -> warn(message)
                INFO -> info(message)
                DEBUG -> debug(message)
            }
        }
    }

    companion object {
        val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER")

        val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> =
            Option("COMPILER_ARGUMENTS_LOG_LEVEL")

        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER")

        val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX")

    }
}

private class MessageReportingCompilerLookupTracker(private val messageReporter: (Message) -> Unit) : CompilerLookupTracker {
    override fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: CompilerLookupTracker.ScopeKind,
        name: String,
    ) {
        messageReporter(
            Message.LookupMessage(filePath, scopeFqName, scopeKind, name)
        )
    }

    override fun clear() {
        messageReporter(
            Message.LookupClear
        )
    }
}

private class MessageReportingMessageCollectorWithDiagnosticId(private val messageReporter: (Message) -> Unit) :
    MessageCollectorWithDiagnosticId {
    override fun clear() {
        messageReporter(Message.CompilerMessageClear)
    }

    override fun hasErrors(): Boolean {
        return false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        messageReporter(Message.CompilerMessageWithDiagnosticId(severity, message, location?.beforeSerialization(), diagnosticId = null))
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ) {
        messageReporter(Message.CompilerMessageWithDiagnosticId(severity, message, location?.beforeSerialization(), diagnosticId))
    }
}

private class BaseCompilerServicesWithResultsFacade(
    messageCollector: MessageCollectorWithDiagnosticId,
    val lookupTracker: CompilerLookupTracker? = null,
) : BasicCompilerServicesWithResultsFacadeServer(messageCollector) {
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
