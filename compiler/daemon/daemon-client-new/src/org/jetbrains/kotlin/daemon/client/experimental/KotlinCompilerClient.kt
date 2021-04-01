/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSessionAsync
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerDaemonClient
import org.jetbrains.kotlin.daemon.client.launchProcessWithFallback
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ServerSocketWrapper
import java.io.File
import java.io.Serializable
import java.net.SocketException
import java.nio.channels.ClosedChannelException
import java.nio.file.Files
import java.rmi.ConnectException
import java.rmi.ConnectIOException
import java.rmi.UnmarshalException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.concurrent.thread


class KotlinCompilerClient : KotlinCompilerDaemonClient {

    init {
        println("experimental KotlinCompilerClient is being instantiated")
    }

    val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = CompilerSystemProperties.COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY.value != null

    private val log = Logger.getLogger("KotlinCompilerClient")

    override fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File =
    // for jps property is passed from IDEA to JPS in KotlinBuildProcessParametersProvider
        CompilerSystemProperties.COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY.value
            ?.let(String::trimQuotes)
            ?.takeUnless(String::isBlank)
            ?.let(::File)
            ?.takeIf(File::exists)
            ?: makeAutodeletingFlagFile(baseDir = File(daemonOptions.runFilesPathOrDefault))

    override suspend fun connectToCompileService(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        checkId: Boolean
    ): CompileServiceAsync? {
        val flagFile = getOrCreateClientFlagFile(daemonOptions)
        return connectToCompileService(
            compilerId,
            flagFile,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart
        )
    }

    override suspend fun connectToCompileService(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean
    ): CompileServiceAsync? {
        return connectAndLease(
            compilerId,
            clientAliveFlagFile,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart,
            leaseSession = false,
            sessionAliveFlagFile = null
        )?.compileService
    }


    override suspend fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File?
    ): CompileServiceSessionAsync? {
        return connectLoop(
            reportingTargets,
            autostart
        ) { isLastAttempt ->
            fun CompileServiceAsync.leaseImpl(): Deferred<CompileServiceSessionAsync?> =
                GlobalScope.async {
                    // the newJVMOptions could be checked here for additional parameters, if needed
                    try {
                        registerClient(clientAliveFlagFile.absolutePath)
                    } catch (e: Throwable) {
                        return@async null
                    }
                    reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
                    if (!leaseSession)
                        CompileServiceSessionAsync(this@leaseImpl, CompileService.NO_SESSION)
                    else
                        try {
                            leaseCompileSession(sessionAliveFlagFile?.absolutePath)
                        } catch (e: Throwable) {
                            return@async null
                        }
                            .takeUnless { it is CompileService.CallResult.Dying }
                            ?.let {
                                CompileServiceSessionAsync(this@leaseImpl, it.get())
                            }
                }

            ensureServerHostnameIsSetUp()
            val (service, newJVMOptions) =
                    tryFindSuitableDaemonOrNewOpts(File(daemonOptions.runFilesPath), compilerId, daemonJVMOptions) { cat, msg ->
                        GlobalScope.async { reportingTargets.report(cat, msg) }
                    }.await()
            if (service != null) {
                service.leaseImpl().await()
            } else {
                if (!isLastAttempt && autostart) {
                    if (startDaemon(compilerId, newJVMOptions, daemonOptions, reportingTargets)) {
                        reportingTargets.report(DaemonReportCategory.DEBUG, "new daemon started, trying to find it")
                    }
                }
                null
            }
        }
    }

    override suspend fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions) {
        connectToCompileService(
            compilerId,
            DaemonJVMOptions(),
            daemonOptions,
            DaemonReportingTargets(out = System.out),
            autostart = false,
            checkId = false
        )?.shutdown()
    }

    override suspend fun leaseCompileSession(compilerService: CompileServiceAsync, aliveFlagPath: String?): Int =
        compilerService.leaseCompileSession(aliveFlagPath).get()

    override suspend fun releaseCompileSession(compilerService: CompileServiceAsync, sessionId: Int) {
        compilerService.releaseCompileSession(sessionId)
    }

    override suspend fun compile(
        compilerService: CompileServiceAsync,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        outputsCollector: ((File, List<File>) -> Unit)?,
        compilerMode: CompilerMode,
        reportSeverity: ReportSeverity,
        port: Int,
        profiler: Profiler
    ): Int = profiler.withMeasure(this) {
        val services = BasicCompilerServicesWithResultsFacadeServerServerSide(
            messageCollector,
            outputsCollector,
            findCallbackServerSocket()
        )
        runBlocking {
            services.runServer()
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
                services.clientSide,
                createCompResults().clientSide
            ).get()
        }
    }

    data class ClientOptions(
        var stop: Boolean = false
    ) : OptionsGroup {
        override val mappers: List<PropMapper<*, *, *>>
            get() = listOf(BoolPropMapper(this, ClientOptions::stop))
    }

    private fun configureClientOptions(opts: ClientOptions): ClientOptions {
        CompilerSystemProperties.COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY.value?.let {
            val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
            if (unrecognized.any())
                throw IllegalArgumentException(
                    "Unrecognized client options passed via property ${CompilerSystemProperties.COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY.property}: " + unrecognized.joinToString(" ") +
                            "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() })
                )
        }
        return opts
    }

    private fun configureClientOptions(): ClientOptions =
        configureClientOptions(ClientOptions())

    override fun main(vararg args: String) {
        runBlocking {
            val compilerId = CompilerId()
            val daemonOptions = configureDaemonOptions()
            val daemonLaunchingOptions = configureDaemonJVMOptions(
                inheritMemoryLimits = true,
                inheritOtherJvmOptions = false,
                inheritAdditionalProperties = true
            )

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
                    log.info("desired compiler classpath: " + compilerId.compilerClasspath.joinToString(File.pathSeparator))
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
                    log.info("Shutdown the daemon")
                    daemon.shutdown()
                    log.info("Daemon shut down successfully")
                }
                filteredArgs.none() -> {
                    // so far used only in tests
                    log.info(
                        "Warning: empty arguments list, only daemon check is performed: checkCompilerId() returns ${
                        daemon.checkCompilerId(
                            compilerId
                        )}"
                    )
                }
                else -> {
                    log.info("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                    val servicesFacade =
                        CompilerCallbackServicesFacadeServerServerSide()
                    servicesFacade.runServer()
                    try {
                        val memBefore = daemon.getUsedMemory().get() / 1024
                        val startTime = System.nanoTime()

                        val compResults = createCompResults()
                        compResults.runServer()
                        val res = daemon.compile(
                            CompileService.NO_SESSION,
                            filteredArgs.toList().toTypedArray(),
                            CompilationOptions(
                                CompilerMode.NON_INCREMENTAL_COMPILER,
                                CompileService.TargetPlatform.JVM,
                                arrayOf(),  // TODO ???
                                ReportSeverity.INFO.code,  // TODO ???
                                arrayOf() // TODO ???
                            ),
                            servicesFacade.clientSide,
                            compResults.clientSide
                        )

                        val endTime = System.nanoTime()
                        log.info("Compilation ${if (res.isGood) "succeeded" else "failed"}, result code: ${res.get()}")
                        val memAfter = daemon.getUsedMemory().get() / 1024
                        log.info("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                        log.info("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                    } finally {
                        // TODO ??
                    }
                }
            }
        }
    }

    override fun createCompResults(): CompilationResultsServerSide = object : CompilationResultsServerSide {

        override val clients = hashMapOf<Socket, Server.ClientInfo>()

        override val serverSocketWithPort: ServerSocketWrapper
            get() = resultsPort

        private val resultsPort = findPortForSocket(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            RESULTS_SERVER_PORTS_RANGE_START,
            RESULTS_SERVER_PORTS_RANGE_END
        )

        private val resultsMap = hashMapOf<Int, MutableList<Serializable>>()

        override val clientSide: CompilationResultsClientSide
            get() = CompilationResultsClientSideImpl(resultsPort.port)

        override suspend fun add(compilationResultCategory: Int, value: Serializable) {
            synchronized(this) {
                resultsMap.putIfAbsent(compilationResultCategory, mutableListOf())
                resultsMap[compilationResultCategory]!!.add(value)
                // TODO logger?
            }
        }

    }

    private fun detectCompilerClasspath(): List<String>? =
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

    private inline fun <R> connectLoop(reportingTargets: DaemonReportingTargets, autostart: Boolean, body: (Boolean) -> R?): R? {
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
                } catch (e: ClosedChannelException) {
                    null to e
                }

                if (res != null) return res

                if (err != null) {
                    GlobalScope.async {
                        reportingTargets.report(
                            DaemonReportCategory.INFO,
                            (if (attempts >= DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) "no more retries on: " else "retrying($attempts) on: ")
                                    + err.toString()
                        )
                    }
                }

                if (attempts++ > DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) {
                    return null
                }
            }
        } catch (e: Throwable) {
            GlobalScope.async { reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString()) }
        }
        return null
    }

    private fun tryFindSuitableDaemonOrNewOpts(
        registryDir: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        report: (DaemonReportCategory, String) -> Unit
    ): Deferred<Pair<CompileServiceAsync?, DaemonJVMOptions>> = GlobalScope.async {
        registryDir.mkdirs()
        val timestampMarker = Files.createTempFile(registryDir.toPath(), "kotlin-daemon-client-tsmarker", null).toFile()
        val aliveWithMetadata = try {
            walkDaemonsAsync(registryDir, compilerId, timestampMarker, report = report)
        } finally {
            timestampMarker.delete()
        }
        val comparator = compareBy<DaemonWithMetadataAsync, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator(), { it.jvmOptions })
            .thenBy {
                when (it.daemon) {
                    is CompileServiceAsyncWrapper -> 0
                    else -> 1
                }
            }
            .thenBy(FileAgeComparator()) { it.runFile }
        val optsCopy = daemonJVMOptions.copy()
        // if required options fit into fattest running daemon - return the daemon and required options with memory params set to actual ones in the daemon
        aliveWithMetadata.maxWith(comparator)
            ?.takeIf { daemonJVMOptions memorywiseFitsInto it.jvmOptions }
            ?.let {
                Pair(it.daemon, optsCopy.updateMemoryUpperBounds(it.jvmOptions))
            }
        // else combine all options from running daemon to get fattest option for a new daemon to runServer
            ?: Pair(null, aliveWithMetadata.fold(optsCopy, { opts, d -> opts.updateMemoryUpperBounds(d.jvmOptions) }))
    }


    private suspend fun startDaemon(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets
    ): Boolean {
        val javaExecutable = File(File(CompilerSystemProperties.JAVA_HOME.safeValue, "bin"), "java")
        val serverHostname = CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.value ?: error("${CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.property} is not set!")
        val platformSpecificOptions = listOf(
            // hide daemon window
            "-Djava.awt.headless=true",
            "-D${CompilerSystemProperties.JAVA_RMI_SERVER_HOSTNAME.property}=$serverHostname"
        )
        val args = listOf(
            javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)
        ) +
                platformSpecificOptions +
                daemonJVMOptions.mappers.flatMap { it.toArgs("-") } +
                COMPILER_DAEMON_CLASS_FQN_EXPERIMENTAL +
                daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        reportingTargets.report(DaemonReportCategory.DEBUG, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        processBuilder.redirectErrorStream(true)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        val daemon =
            launchProcessWithFallback(processBuilder, reportingTargets, "daemon client")

        val isEchoRead = Semaphore(1)
        isEchoRead.acquire()

        val stdoutThread =
            thread {
                try {
                    daemon.inputStream
                        .reader()
                        .forEachLine {
                            log.info("daemon_process_report : $it")
                            if (it == COMPILE_DAEMON_IS_READY_MESSAGE) {
                                GlobalScope.async {
                                    reportingTargets.report(
                                        DaemonReportCategory.DEBUG,
                                        "Received the message signalling that the daemon is ready"
                                    )
                                }
                                isEchoRead.release()
                                //TODO return@forEachLine
                            } else {
                                GlobalScope.async { reportingTargets.report(DaemonReportCategory.INFO, it, "daemon") }
                            }
                        }
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
                            DaemonReportCategory.INFO,
                            "Daemon terminated unexpectedly with error code: ${daemon.exitValue()}"
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
                @Suppress("DEPRECATION")
                stdoutThread.stop()
            }
            reportingTargets.out?.flush()
        }
    }
}

internal suspend fun DaemonReportingTargets.report(category: DaemonReportCategory, message: String, source: String? = null) {
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
