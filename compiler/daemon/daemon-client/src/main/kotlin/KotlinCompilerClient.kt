/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.io.PrintStream
import java.net.SocketException
import java.nio.file.Files
import java.rmi.ConnectException
import java.rmi.ConnectIOException
import java.rmi.UnmarshalException
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class CompilationServices(
    val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
    val lookupTracker: LookupTracker? = null,
    val compilationCanceledStatus: CompilationCanceledStatus? = null,
)

data class CompileServiceSession(val compileService: CompileService, val sessionId: Int)

object KotlinCompilerClient {

    private const val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    private const val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = CompilerSystemProperties.COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY.value != null

    fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File =
        // for jps property is passed from IDEA to JPS in KotlinBuildProcessParametersProvider
        CompilerSystemProperties.COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY.value
            ?.let(String::trimQuotes)
            ?.takeUnless(String::isBlank)
            ?.let(::File)
            ?.takeIf(File::exists)
            ?: makeAutodeletingFlagFile(baseDir = File(daemonOptions.runFilesPathOrDefault))

    fun connectToCompileService(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean = true,
        @Suppress("UNUSED_PARAMETER") checkId: Boolean = true,
    ): CompileService? {
        val flagFile = getOrCreateClientFlagFile(daemonOptions)
        return connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, reportingTargets, autostart)
    }

    fun connectToCompileService(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean = true,
    ): CompileService? =
        connectAndLease(
            compilerId,
            clientAliveFlagFile,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart,
            leaseSession = false,
            sessionAliveFlagFile = null
        )?.compileService


    fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File? = null,
    ): CompileServiceSession? {
        val ignoredDaemonSessionFiles = mutableSetOf<File>()
        var daemonStartupAttemptsCount = 0
        return connectLoop(reportingTargets, autostart) { isLastAttempt ->

            fun CompileService.tryToLeaseSession(): CompileServiceSession? {
                // the newJVMOptions could be checked here for additional parameters, if needed
                registerClient(clientAliveFlagFile.absolutePath)
                reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")

                if (!leaseSession) return CompileServiceSession(this, CompileService.NO_SESSION)

                return when (val leaseSessionResult = leaseCompileSession(sessionAliveFlagFile?.absolutePath)) {
                    is CompileService.CallResult.Dying -> {
                        reportingTargets.report(DaemonReportCategory.DEBUG, "the daemon is already dying, skipping it")
                        null
                    }
                    is CompileService.CallResult.Good -> {
                        val sessionId = leaseSessionResult.get()
                        reportingTargets.report(DaemonReportCategory.DEBUG, "successfully leased a compile session (id = $sessionId)")
                        CompileServiceSession(this, sessionId)
                    }
                    else -> {
                        reportingTargets.report(DaemonReportCategory.DEBUG, "got an expected result on attempt to lease a compile session")
                        // the call to get() below shall lead to an exception throwing
                        // if it does happen, it indicates real problems,
                        // so no special handling is required, and it's ok to fail-fast
                        CompileServiceSession(this, leaseSessionResult.get())
                    }
                }
            }

            ensureServerHostnameIsSetUp()
            val result = tryFindSuitableDaemonOrNewOpts(
                File(daemonOptions.runFilesPath),
                compilerId,
                daemonJVMOptions,
                ignoredDaemonSessionFiles,
            ) { cat, msg -> reportingTargets.report(cat, msg) }

            when (result) {
                is DaemonSearchResult.Found -> result.compileService.tryToLeaseSession().also {
                    // the null value here means that the daemon is already dying,
                    // so we shall query other daemons or start a new one
                    if (it == null) ignoredDaemonSessionFiles.add(result.runFileMarker)
                }
                is DaemonSearchResult.NotFound -> {
                    if (!isLastAttempt && autostart) {
                        reportingTargets.report(DaemonReportCategory.DEBUG, "trying to start a new compiler daemon")
                        if (startDaemon(compilerId, result.requiredJvmOptions, daemonOptions, reportingTargets, daemonStartupAttemptsCount++)) {
                            reportingTargets.report(DaemonReportCategory.DEBUG, "new compiler daemon started, trying to find it")
                        }
                    }
                    null
                }
            }
        }
    }

    fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions): Unit {
        connectToCompileService(
            compilerId,
            DaemonJVMOptions(),
            daemonOptions,
            DaemonReportingTargets(out = System.out),
            autostart = false,
            checkId = false
        )
            ?.shutdown()
    }


    fun shutdownCompileService(compilerId: CompilerId): Unit {
        shutdownCompileService(compilerId, DaemonOptions())
    }


    fun leaseCompileSession(compilerService: CompileService, aliveFlagPath: String?): Int =
        compilerService.leaseCompileSession(aliveFlagPath).get()

    fun releaseCompileSession(compilerService: CompileService, sessionId: Int): Unit {
        compilerService.releaseCompileSession(sessionId)
    }

    fun compile(
        compilerService: CompileService,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        outputsCollector: ((File, List<File>) -> Unit)? = null,
        compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        reportSeverity: ReportSeverity = ReportSeverity.INFO,
        port: Int = SOCKET_ANY_FREE_PORT,
        profiler: Profiler = DummyProfiler(),
    ): Int = profiler.withMeasure(this) {
        val services = BasicCompilerServicesWithResultsFacadeServer(messageCollector, outputsCollector, port)
        compilerService.compile(
            sessionId,
            args,
            CompilationOptions(
                compilerMode,
                targetPlatform,
                arrayOf(
                    ReportCategory.COMPILER_MESSAGE.code,
                    ReportCategory.DAEMON_MESSAGE.code,
                    ReportCategory.EXCEPTION.code,
                    ReportCategory.OUTPUT_MESSAGE.code
                ),
                reportSeverity.code,
                emptyArray()
            ),
            services,
            null
        ).get()
    }

    data class ClientOptions(
        var stop: Boolean = false,
    ) : OptionsGroup {
        override val mappers: List<PropMapper<*, *, *>>
            get() = listOf(BoolPropMapper(this, ClientOptions::stop))
    }

    private fun configureClientOptions(opts: ClientOptions): ClientOptions {
        CompilerSystemProperties.COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY.value?.let {
            val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
            if (unrecognized.any())
                throw IllegalArgumentException(
                    "Unrecognized client options passed via property ${CompilerSystemProperties.COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY.property}: " + unrecognized.joinToString(
                        " "
                    ) +
                            "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() })
                )
        }
        return opts
    }

    private fun configureClientOptions(): ClientOptions = configureClientOptions(ClientOptions())


    @JvmStatic
    fun main(vararg args: String) {
        val compilerId = CompilerId()
        val daemonOptions = configureDaemonOptions()
        val daemonLaunchingOptions =
            configureDaemonJVMOptions(inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true)
        val clientOptions = configureClientOptions()
        val filteredArgs = args.asIterable().filterExtractProps(
            compilerId,
            daemonOptions,
            daemonLaunchingOptions,
            clientOptions,
            prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX
        )

        if (!clientOptions.stop) {
            if (compilerId.compilerClasspath.none()) {
                // attempt to find compiler to use
                System.err.println("compiler wasn't explicitly specified, attempt to find appropriate jar")
                detectCompilerClasspath()
                    ?.let { compilerId.compilerClasspath = it }
            }
            if (compilerId.compilerClasspath.none())
                throw IllegalArgumentException("Cannot find compiler jar")
            else
                println("desired compiler classpath: " + compilerId.compilerClasspath.joinToString(File.pathSeparator))
        }

        val daemon = connectToCompileService(
            compilerId,
            daemonLaunchingOptions,
            daemonOptions,
            DaemonReportingTargets(out = System.out),
            autostart = !clientOptions.stop,
            checkId = !clientOptions.stop
        )

        if (daemon == null) {
            if (clientOptions.stop) {
                System.err.println("No daemon found to shut down")
            } else throw Exception("Unable to connect to daemon")
        } else when {
            clientOptions.stop -> {
                println("Shutdown the daemon")
                daemon.shutdown()
                println("Daemon shut down successfully")
            }
            filteredArgs.none() -> {
                // so far used only in tests
                println(
                    "Warning: empty arguments list, only daemon check is performed: checkCompilerId() returns ${
                        daemon.checkCompilerId(
                            compilerId
                        )
                    }"
                )
            }
            else -> {
                println("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                val messageCollector = object : MessageCollector {
                    var hasErrors = false
                    override fun clear() {}

                    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                        if (severity.isError) {
                            hasErrors = true
                        }
                        println("${severity.name}\t${location?.path ?: ""}:${location?.line ?: ""} \t$message")
                    }

                    override fun hasErrors() = hasErrors
                }

                val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
                val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(messageCollector, outputsCollector)
                try {
                    val memBefore = daemon.getUsedMemory().get() / 1024
                    val startTime = System.nanoTime()

                    val compilationOptions = CompilationOptions(
                        CompilerMode.NON_INCREMENTAL_COMPILER,
                        CompileService.TargetPlatform.JVM,
                        arrayOf(
                            ReportCategory.COMPILER_MESSAGE.code,
                            ReportCategory.DAEMON_MESSAGE.code,
                            ReportCategory.EXCEPTION.code,
                            ReportCategory.OUTPUT_MESSAGE.code
                        ),
                        ReportSeverity.INFO.code,
                        emptyArray()
                    )


                    val res = daemon.compile(
                        CompileService.NO_SESSION,
                        filteredArgs.toList().toTypedArray(),
                        compilationOptions,
                        servicesFacade,
                        null
                    )

                    val endTime = System.nanoTime()
                    println("Compilation ${if (res.isGood) "succeeded" else "failed"}, result code: ${res.get()}")
                    val memAfter = daemon.getUsedMemory().get() / 1024
                    println("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                    println("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                } finally {
                    // forcing RMI to unregister all objects and stop
                    UnicastRemoteObject.unexportObject(servicesFacade, true)
                }
            }
        }
    }

    fun detectCompilerClasspath(): List<String>? =
        CompilerSystemProperties.JAVA_CLASS_PATH.value
            ?.split(File.pathSeparator)
            ?.map { File(it).parentFile }
            ?.distinct()
            ?.mapNotNull {
                it?.walk()
                    ?.firstOrNull { it.name.equals(COMPILER_JAR_NAME, ignoreCase = true) }
            }
            ?.firstOrNull()
            ?.let { listOf(it.absolutePath) }

    // --- Implementation ---------------------------------------

    private inline fun <R> connectLoop(
        reportingTargets: DaemonReportingTargets, autostart: Boolean, body: (Boolean) -> R?,
    ): R? = synchronized(this) {
        try {
            var attempts = 1
            while (true) {
                val (res, err) = try {
                    body(attempts >= DAEMON_CONNECT_CYCLE_ATTEMPTS) to null
                } catch (e: SocketException) {
                    null to e
                } catch (e: ConnectException) {
                    null to e
                } catch (e: ConnectIOException) {
                    null to e
                } catch (e: UnmarshalException) {
                    null to e
                } catch (e: RuntimeException) {
                    null to e
                }

                if (res != null) return res

                val errorDetails = err?.let { ", error: ${it.stackTraceToString()}" } ?: ""
                if (attempts >= DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) {
                    reportingTargets.report(
                        DaemonReportCategory.EXCEPTION,
                        "Failed connecting to the daemon in $attempts retries$errorDetails"
                    )
                } else {
                    reportingTargets.report(DaemonReportCategory.INFO, "#$attempts retrying connecting to the daemon $errorDetails")
                }

                if (++attempts > DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) {
                    return null
                }
            }
        } catch (e: Throwable) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        return null
    }

    private sealed interface DaemonSearchResult {
        class Found(
            val compileService: CompileService,
            val runFileMarker: File,
        ) : DaemonSearchResult

        class NotFound(
            val requiredJvmOptions: DaemonJVMOptions,
        ) : DaemonSearchResult
    }

    private fun tryFindSuitableDaemonOrNewOpts(
        registryDir: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        ignoredDaemonSessionFiles: Set<File>,
        report: (DaemonReportCategory, String) -> Unit,
    ): DaemonSearchResult {
        registryDir.mkdirs()
        val timestampMarker = Files.createTempFile(registryDir.toPath(), "kotlin-daemon-client-tsmarker", null).toFile()
        val aliveWithMetadata = try {
            walkDaemons(registryDir, compilerId, timestampMarker, report = report, filter = { file, _ -> file !in ignoredDaemonSessionFiles }).toList()
        } finally {
            timestampMarker.delete()
        }
        val comparator =
            compareBy<DaemonWithMetadata, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator()) { it.jvmOptions }
                .thenBy(FileAgeComparator()) { it.runFile }
        val optsCopy = daemonJVMOptions.copy()
        // if required options fit into fattest running daemon - return the daemon and required options with memory params set to actual ones in the daemon
        return aliveWithMetadata.maxWithOrNull(comparator)?.takeIf { daemonJVMOptions memorywiseFitsInto it.jvmOptions }?.let {
            DaemonSearchResult.Found(it.daemon, it.runFile)
        }
        // else combine all options from running daemon to get fattest option for a new daemon to run
            ?: DaemonSearchResult.NotFound(aliveWithMetadata.fold(optsCopy) { opts, d -> opts.updateMemoryUpperBounds(d.jvmOptions) })
    }


    private fun startDaemon(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        startupAttempt: Int,
    ): Boolean {
        val javaExecutable = File(File(CompilerSystemProperties.JAVA_HOME.safeValue, "bin"), "java")
        val serverHostname = CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.value
            ?: error("${CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.property} is not set!")
        val platformSpecificOptions = listOf(
            // hide daemon window
            "-Djava.awt.headless=true",
            "-D${CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.property}=$serverHostname"
        )
        val javaVersion = CompilerSystemProperties.JAVA_VERSION.value?.toIntOrNull()
        val javaIllegalAccessWorkaround =
            if (javaVersion != null && javaVersion >= 16)
                listOf("--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED")
            else emptyList()
        val jvmArguments = daemonJVMOptions.mappers.flatMap { it.toArgs("-") }
        val additionalOptimizationOptions = listOfNotNull(
            "-XX:+UseCodeCacheFlushing",
            // enable parallel gc only if it's not explicitly disabled and no other GC is selected
            "-XX:+UseParallelGC".takeIf { jvmArguments.none { it == "-XX:-UseParallelGC" || (it.startsWith("-XX:+Use") && it.endsWith("GC")) } },
        )
        val args = listOf(
            javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)
        ) +
                platformSpecificOptions +
                jvmArguments +
                additionalOptimizationOptions +
                javaIllegalAccessWorkaround +
                COMPILER_DAEMON_CLASS_FQN +
                daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        reportingTargets.report(DaemonReportCategory.INFO, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        processBuilder.redirectErrorStream(true)
        val workingDir = File(daemonOptions.runFilesPath).apply { mkdirs() }
        processBuilder.directory(workingDir)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        val daemon = launchProcessWithFallback(processBuilder, reportingTargets, "daemon client")

        val isEchoRead = Semaphore(1)
        isEchoRead.acquire()

        val lastDaemonCliOutputs = LastDaemonCliOutputs()

        val stdoutThread =
            thread {
                try {
                    daemon.inputStream
                        .reader()
                        .forEachLine {
                            if (Thread.currentThread().isInterrupted) return@forEachLine
                            lastDaemonCliOutputs.add(it)
                            if (it == COMPILE_DAEMON_IS_READY_MESSAGE) {
                                reportingTargets.report(
                                    DaemonReportCategory.DEBUG,
                                    "Received the message signalling that the daemon is ready"
                                )
                                isEchoRead.release()
                                return@forEachLine
                            } else {
                                reportingTargets.report(DaemonReportCategory.INFO, it, "daemon")
                            }
                        }
                } catch (_: Throwable) {
                    // Ignore, assuming all exceptions as interrupt exceptions
                } finally {
                    daemon.inputStream.close()
                    daemon.outputStream.close()
                    daemon.errorStream.close()
                    isEchoRead.release()
                }
            }
        try {
            // trying to wait for process
            val daemonStartupTimeout = CompilerSystemProperties.COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY.value?.let {
                try {
                    it.toLong()
                } catch (e: Exception) {
                    reportingTargets.report(
                        DaemonReportCategory.INFO,
                        "unable to interpret ${CompilerSystemProperties.COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY.property} property ('$it'); using default timeout $DAEMON_DEFAULT_STARTUP_TIMEOUT_MS ms"
                    )
                    null
                }
            } ?: DAEMON_DEFAULT_STARTUP_TIMEOUT_MS
            if (daemonOptions.runFilesPath.isNotEmpty()) {
                val succeeded = isEchoRead.tryAcquire(daemonStartupTimeout, TimeUnit.MILLISECONDS)
                return when {
                    !isProcessAlive(daemon) -> {
                        reportingTargets.report(
                            DaemonReportCategory.EXCEPTION,
                            "The daemon has terminated unexpectedly on startup attempt #${startupAttempt + 1} with error code: ${daemon.exitValue()}. ${lastDaemonCliOutputs.getAsSingleString()}"
                        )
                        false
                    }
                    !succeeded -> {
                        reportingTargets.report(DaemonReportCategory.INFO, "Unable to get response from daemon in $daemonStartupTimeout ms")
                        false
                    }
                    else -> true
                }
            } else
            // without startEcho defined waiting for max timeout
                Thread.sleep(daemonStartupTimeout)
            return true
        } finally {
            // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
            if (stdoutThread.isAlive) {
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                stdoutThread.interrupt()
            }
            reportingTargets.out?.flush()
        }
    }
}


data class DaemonReportMessage(val category: DaemonReportCategory, val message: String)

class DaemonReportingTargets(
    val out: PrintStream? = null,
    val messages: MutableCollection<DaemonReportMessage>? = null,
    val messageCollector: MessageCollector? = null,
    val compilerServices: CompilerServicesFacadeBase? = null,
)

internal fun DaemonReportingTargets.report(category: DaemonReportCategory, message: String, source: String? = null) {
    val sourceMessage: String by lazy { source?.let { "[$it] $message" } ?: message }
    out?.println("${category.name}: $sourceMessage")
    messages?.add(DaemonReportMessage(category, sourceMessage))
    messageCollector?.let {
        when (category) {
            DaemonReportCategory.DEBUG -> it.report(CompilerMessageSeverity.LOGGING, sourceMessage)
            DaemonReportCategory.INFO -> it.report(CompilerMessageSeverity.INFO, sourceMessage)
            DaemonReportCategory.EXCEPTION -> it.report(CompilerMessageSeverity.EXCEPTION, sourceMessage)
        }
    }
    compilerServices?.let {
        when (category) {
            DaemonReportCategory.DEBUG -> it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.DEBUG, message, source)
            DaemonReportCategory.INFO -> it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.INFO, message, source)
            DaemonReportCategory.EXCEPTION -> it.report(ReportCategory.EXCEPTION, ReportSeverity.ERROR, message, source)
        }
    }
}

internal fun isProcessAlive(process: Process) =
    try {
        process.exitValue()
        false
    } catch (e: IllegalThreadStateException) {
        true
    }
