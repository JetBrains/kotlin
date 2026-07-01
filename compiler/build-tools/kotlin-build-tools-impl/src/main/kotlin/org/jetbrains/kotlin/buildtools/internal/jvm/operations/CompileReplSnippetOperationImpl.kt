/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.ReplSnippetCompilationResult
import org.jetbrains.kotlin.buildtools.api.jvm.operations.ReplSnippetDiagnostic
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplStatelessCompiler
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.rmi.RemoteException
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

/**
 * BTA op implementation that bridges the public [CompileReplSnippetOperation] API to the
 * stateless K2 REPL compile entry point.
 *
 * **This is an out-of-process operation.** The whole point of the stateless refactoring is that a
 * snippet is compiled in a *separate* process from the IDE/build-tool consumer — keeping no
 * server-side REPL state — so only [ExecutionPolicy.WithDaemon] is supported. There is
 * deliberately **no** [ExecutionPolicy.InProcess] variant: an in-process consumer that wants to
 * drive the stateless compiler directly should call [K2ReplStatelessCompiler] itself rather than
 * go through this transport op (and [executeImpl] rejects the in-process policy with a clear
 * [UnsupportedOperationException]).
 *
 * On [ExecutionPolicy.WithDaemon] the snippet is compiled on the **regular** Kotlin compile daemon
 * path (migration step 3, Q5d): the snippet rides a plain `CompileService.compile(...)` call
 * switched into snippet mode by scripting-plugin options (`repl-snippet-mode` /
 * `repl-snippet-name` / `repl-snippet-prior-artifact` / `repl-snippet-artifact-output`), exactly
 * as a CLI invocation would. No REPL-specific RMI is added to `CompileService` (which migration
 * step 4 strips) — priors and the produced artifact are exchanged through plain files, and the
 * daemon-reported compiler messages are captured back into the structured
 * [ReplSnippetCompilationResult].
 *
 * A plain compile failure is **not** signalled by a thrown exception — exceptions are reserved for
 * precondition/infra errors (unsupported execution policy, daemon connection failure).
 */
internal class CompileReplSnippetOperationImpl private constructor(
    override val options: Options = Options(CompileReplSnippetOperation::class),
    override val priorSnippets: List<ByteArray>,
    override val snippetSource: String,
    override val snippetName: String,
    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
) : BuildOperationImpl<ReplSnippetCompilationResult>(),
    CompileReplSnippetOperation,
    CompileReplSnippetOperation.Builder,
    DeepCopyable<CompileReplSnippetOperation> {

    constructor(
        priorSnippets: List<ByteArray>,
        snippetSource: String,
        snippetName: String,
        buildIdToSessionFlagFile: MutableMap<ProjectId, File>,
    ) : this(
        Options(CompileReplSnippetOperation::class),
        priorSnippets,
        snippetSource,
        snippetName,
        buildIdToSessionFlagFile,
    ) {
        initializeOptions(this::class, options)
    }

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
    ): ReplSnippetCompilationResult = when (executionPolicy) {
        is DaemonExecutionPolicyImpl -> executeWithDaemon(projectId, executionPolicy, logger)
        is InProcessExecutionPolicyImpl -> throw UnsupportedOperationException(
            "Stateless REPL snippet compilation is an out-of-process operation: only " +
                    "ExecutionPolicy.WithDaemon is supported. In-process execution is intentionally not " +
                    "provided — the stateless refactoring exists so a snippet can be compiled in a separate " +
                    "process (the daemon, or a regular compiler subprocess) without keeping server-side REPL " +
                    "state. An in-process consumer should call K2ReplStatelessCompiler directly instead of " +
                    "going through this transport operation."
        )
        else -> throw IllegalStateException("Unsupported execution policy: ${executionPolicy::class.qualifiedName}")
    }

    /**
     * Compiles the snippet through the Kotlin compile daemon on the regular compile path: the
     * priors are written to temp files and the snippet is delivered through `-expression`, switched
     * into snippet mode by the scripting-plugin `-P` options. The produced artifact is read back
     * from the configured output file, and the daemon's compiler messages are captured into the
     * structured result.
     */
    private fun executeWithDaemon(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        logger: KotlinLogger?,
    ): ReplSnippetCompilationResult {
        val kotlinLogger = logger ?: DefaultKotlinLogger
        val renderer = this[COMPILER_MESSAGE_RENDERER]
        val messageCollector = ReplSnippetDiagnosticCollector(kotlinLogger, renderer)

        val workDir = Files.createTempDirectory("bta-repl-snippet-").toFile()
        try {
            val priorFiles = priorSnippets.mapIndexed { index, bytes ->
                File(workDir, "prior-$index.artifact").also { it.writeBytes(bytes) }
            }
            val outputFile = File(workDir, "snippet-out.artifact")
            val extraClasspath: List<Path> = this[ADDITIONAL_CLASSPATH]
            // The shaded `kotlin-build-tools-impl` jar deliberately strips the scripting plugin's
            // `CompilerPluginRegistrar`/`CommandLineProcessor` service files (so it does not
            // auto-register on every regular compilation). Calling `K2ReplStatelessCompiler`
            // directly would sidestep this, but the daemon runs the *regular* compiler, which
            // discovers plugins via services — so we hand it a tiny `-Xplugin` jar that re-declares
            // the (relocated) scripting plugin (its classes are already on the daemon's compiler
            // classpath inside the shaded jar).
            val pluginServicesJar = createScriptingPluginServicesJar(workDir)

            val arguments = buildSnippetCompilerArguments(extraClasspath, priorFiles, outputFile, pluginServicesJar)

            val exitCode = runDaemonCompile(projectId, executionPolicy, kotlinLogger, messageCollector, arguments)

            val diagnostics = messageCollector.diagnostics
            return if (exitCode == ExitCode.OK.code && outputFile.exists()) {
                ReplSnippetCompilationResult.Success(outputFile.readBytes(), diagnostics)
            } else {
                ReplSnippetCompilationResult.Failure(diagnostics)
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    /**
     * Builds the CLI argument list that compiles [snippetSource] as a stateless REPL snippet on the
     * regular compile path. This is the same invocation shape a CLI `kotlinc` call would carry, so
     * the daemon needs no REPL-specific transport.
     */
    private fun buildSnippetCompilerArguments(
        extraClasspath: List<Path>,
        priorFiles: List<File>,
        outputFile: File,
        pluginServicesJar: File,
    ): List<String> {
        fun pluginOption(name: String, value: String) = "plugin:$KOTLIN_SCRIPTING_PLUGIN_ID:$name=$value"
        return buildList {
            if (extraClasspath.isNotEmpty()) {
                add("-cp")
                add(extraClasspath.joinToString(File.pathSeparator) { it.toAbsolutePath().toString() })
            }
            add("-Xplugin=${pluginServicesJar.absolutePath}")
            // `-expression` reliably enters the scripting eval path without needing a recognised
            // script file; the snippet's identifying name is supplied separately via
            // `repl-snippet-name` (an `-expression` source is otherwise always named `script.kts`,
            // which would collide across a multi-snippet sequence).
            add("-expression")
            add(snippetSource)
            add("-P")
            add(pluginOption("repl-snippet-mode", "true"))
            add("-P")
            add(pluginOption("repl-snippet-name", snippetName))
            for (priorFile in priorFiles) {
                add("-P")
                add(pluginOption("repl-snippet-prior-artifact", priorFile.absolutePath))
            }
            add("-P")
            add(pluginOption("repl-snippet-artifact-output", outputFile.absolutePath))
            add("-Xsuppress-version-warnings")
        }
    }

    /** Connects to (or starts) the compile daemon and runs a single non-incremental compile. */
    private fun runDaemonCompile(
        projectId: ProjectId,
        executionPolicy: DaemonExecutionPolicyImpl,
        kotlinLogger: KotlinLogger,
        messageCollector: MessageCollector,
        arguments: List<String>,
    ): Int {
        kotlinLogger.debug("Compiling a REPL snippet using the daemon strategy")
        val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
        val sessionIsAliveFlagFile = buildIdToSessionFlagFile.computeIfAbsent(projectId) {
            createSessionIsAliveFlagFile()
        }

        val logsPath = executionPolicy[DaemonExecutionPolicyImpl.LOGS_PATH]
        Files.createDirectories(logsPath)
        val daemonLogOptions = DaemonLogOptions(
            logsPath = logsPath.toAbsolutePath().toString(),
            logsFileSizeLimit = executionPolicy[DaemonExecutionPolicyImpl.LOGS_FILE_SIZE_LIMIT] ?: 0,
            logsFileCountLimit = executionPolicy[DaemonExecutionPolicyImpl.LOGS_FILE_COUNT_LIMIT] ?: Int.MAX_VALUE,
        )

        val additionalJvmArguments = mutableListOf<String>()
        val daemonOptions = configureDaemonOptions(
            DaemonOptions().apply {
                executionPolicy[DaemonExecutionPolicyImpl.SHUTDOWN_DELAY_MILLIS]?.let { shutdownDelay ->
                    shutdownDelayMilliseconds = shutdownDelay
                }
                runFilesPath = executionPolicy[DaemonExecutionPolicyImpl.DAEMON_RUN_DIR_PATH].toAbsolutePath().toString()
                additionalJvmArguments += "D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=$runFilesPath"
            }
        )

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        ).also { opts ->
            val effectiveJvmArguments = additionalJvmArguments + (executionPolicy[DaemonExecutionPolicyImpl.JVM_ARGUMENTS] ?: emptyList())
            if (effectiveJvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    effectiveJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }

        val connection = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFile,
            sessionIsAliveFlagFile,
            messageCollector,
            isDebugEnabled = kotlinLogger.isDebugEnabled,
            daemonJVMOptions = jvmOptions,
            daemonOptions = daemonOptions,
            daemonLogOptions = daemonLogOptions,
        ) ?: throw IllegalStateException("Could not connect to the Kotlin daemon for REPL snippet compilation")

        val daemon = connection.compileService
        val sessionId = connection.sessionId

        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code),
            reportSeverity = ReportSeverity.INFO.code,
            requestedCompilationResults = emptyArray(),
        )

        try {
            return daemon.compile(
                sessionId,
                arguments.toTypedArray(),
                compilationOptions,
                BasicCompilerServicesWithResultsFacadeServer(messageCollector),
                DaemonCompilationResults(kotlinLogger, rootProjectDir = null, DoNothingBuildMetricsReporter),
            ).get()
        } finally {
            try {
                daemon.releaseCompileSession(sessionId)
            } catch (e: RemoteException) {
                kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
            }
        }
    }

    private fun getCurrentClasspath(): List<File> =
        (CompileReplSnippetOperationImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }

    /**
     * Writes a minimal `-Xplugin` jar that re-declares the scripting plugin's K2
     * [CompilerPluginRegistrar] and [CommandLineProcessor] via `META-INF/services`. The class names
     * are the *relocated* ones (derived from this module's bundled, relocated
     * [K2ReplStatelessCompiler]), and the classes themselves live on the daemon's compiler classpath
     * inside the shaded jar — so the compiler's plugin classloader (whose parent is the compiler
     * classloader) can load them from the service declarations in this jar.
     */
    private fun createScriptingPluginServicesJar(workDir: File): File {
        // `K2ReplStatelessCompiler` lives in `<relocated>.compiler.plugin.impl`; the registrar and
        // command-line processor live one package up, in `<relocated>.compiler.plugin`.
        val pluginPackage = K2ReplStatelessCompiler::class.java.`package`.name.removeSuffix(".impl")
        val registrarFqName = "$pluginPackage.ScriptingK2CompilerPluginRegistrar"
        val commandLineProcessorFqName = "$pluginPackage.ScriptingCommandLineProcessor"
        val jar = File(workDir, "scripting-plugin-services.jar")
        JarOutputStream(jar.outputStream().buffered()).use { out ->
            fun serviceEntry(serviceName: String, implementationFqName: String) {
                out.putNextEntry(JarEntry("META-INF/services/$serviceName"))
                out.write("$implementationFqName\n".toByteArray())
                out.closeEntry()
            }
            serviceEntry("org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar", registrarFqName)
            serviceEntry("org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor", commandLineProcessorFqName)
        }
        return jar
    }

    override fun toBuilder(): CompileReplSnippetOperation.Builder = deepCopy()

    @UseFromImplModuleRestricted
    override fun <V> get(key: CompileReplSnippetOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: CompileReplSnippetOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    override fun build(): CompileReplSnippetOperation = deepCopy()

    override fun deepCopy(): CompileReplSnippetOperationImpl =
        CompileReplSnippetOperationImpl(options.deepCopy(), priorSnippets, snippetSource, snippetName, buildIdToSessionFlagFile)

    private operator fun <V> get(key: Option<V>): V = options[key]

    private operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        const val KOTLIN_SCRIPTING_PLUGIN_ID: String = "kotlin.scripting"

        val ADDITIONAL_CLASSPATH: Option<List<Path>> =
            Option("ADDITIONAL_CLASSPATH", default = emptyList())

        val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> =
            Option("COMPILER_MESSAGE_RENDERER", default = DefaultCompilerMessageRenderer)
    }
}

/**
 * A [MessageCollector] that captures the daemon-reported compiler messages as
 * [ReplSnippetDiagnostic]s — so the daemon execution path can return the same structured
 * [ReplSnippetCompilationResult] as the in-process path — while also streaming them to [logger]
 * through [renderer]. Pure logging/output noise is forwarded to the logger but not recorded as a
 * snippet diagnostic.
 */
private class ReplSnippetDiagnosticCollector(
    private val logger: KotlinLogger,
    private val renderer: CompilerMessageRenderer,
) : MessageCollector {
    private val collected = mutableListOf<ReplSnippetDiagnostic>()
    private var sawErrors = false

    val diagnostics: List<ReplSnippetDiagnostic> get() = collected

    override fun clear() {
        collected.clear()
        sawErrors = false
    }

    override fun hasErrors(): Boolean = sawErrors

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity == CompilerMessageSeverity.ERROR || severity == CompilerMessageSeverity.EXCEPTION) {
            sawErrors = true
        }
        val mappedSeverity = when (severity) {
            CompilerMessageSeverity.ERROR, CompilerMessageSeverity.EXCEPTION -> CompilerMessageRenderer.Severity.ERROR
            CompilerMessageSeverity.STRONG_WARNING, CompilerMessageSeverity.WARNING, CompilerMessageSeverity.FIXED_WARNING ->
                CompilerMessageRenderer.Severity.WARNING
            CompilerMessageSeverity.INFO -> CompilerMessageRenderer.Severity.INFO
            CompilerMessageSeverity.LOGGING, CompilerMessageSeverity.OUTPUT -> {
                logger.debug(message)
                return
            }
        }
        val mappedLocation = location?.let {
            CompilerMessageRenderer.SourceLocation(
                path = it.path,
                line = it.line,
                column = it.column,
                lineEnd = it.lineEnd,
                columnEnd = it.columnEnd,
                lineContent = it.lineContent,
            )
        }
        collected.add(ReplSnippetDiagnostic(mappedSeverity, message, mappedLocation))
        val rendered = renderer.render(mappedSeverity, message, mappedLocation)
        when (mappedSeverity) {
            CompilerMessageRenderer.Severity.ERROR -> logger.error(rendered)
            CompilerMessageRenderer.Severity.WARNING -> logger.warn(rendered)
            CompilerMessageRenderer.Severity.INFO -> logger.info(rendered)
            CompilerMessageRenderer.Severity.DEBUG -> logger.debug(rendered)
        }
    }
}
