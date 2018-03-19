/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.io.PrintStream
import java.io.Serializable
import java.net.SocketException
import java.rmi.ConnectException
import java.rmi.ConnectIOException
import java.rmi.UnmarshalException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.concurrent.thread

class CompilationServices(
    val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
    val lookupTracker: LookupTracker? = null,
    val compilationCanceledStatus: CompilationCanceledStatus? = null
)

data class CompileServiceSession(val compileService: CompileServiceClientSide, val sessionId: Int)


object KotlinCompilerClient {

    val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY) != null

    private val log = Logger.getLogger("KotlinCompilerClient")

    fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File =
            // for jps property is passed from IDEA to JPS in KotlinBuildProcessParametersProvider
        System.getProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)
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
        checkId: Boolean = true
    ): CompileServiceClientSide? {
        log.info("in connectToCompileService")
        val flagFile = getOrCreateClientFlagFile(daemonOptions)
        return connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, reportingTargets, autostart)
    }

    fun connectToCompileService(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean = true
    ): CompileServiceClientSide? =
        log.info("connectToCompileService")
            .let {
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
            }


    fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File? = null
    ): CompileServiceSession? = connectLoop(reportingTargets, autostart) { isLastAttempt ->

        log.info("connectAndLease")

        fun CompileServiceClientSide.leaseImpl(): CompileServiceSession? = runBlocking(Unconfined) {
            // the newJVMOptions could be checked here for additional parameters, if needed
            registerClient(clientAliveFlagFile.absolutePath)
            reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
            if (!leaseSession)
                CompileServiceSession(this@leaseImpl, CompileService.NO_SESSION)
            else
                leaseCompileSession(sessionAliveFlagFile?.absolutePath)
                    .takeUnless { it is CompileService.CallResult.Dying }
                    ?.let {
                        CompileServiceSession(this@leaseImpl, it.get())
                    }
        }

        ensureServerHostnameIsSetUp()
        val (service, newJVMOptions) = runBlocking(Unconfined) {
            tryFindSuitableDaemonOrNewOpts(
                File(daemonOptions.runFilesPath),
                compilerId,
                daemonJVMOptions,
                { cat, msg -> reportingTargets.report(cat, msg) })
        }
        if (service != null) {
            log.info("service != null => service.connectToServer()")
            service.connectToServer()
            service.leaseImpl()
        } else {
            log.info("service == null <==> no suitable daemons found")
            if (!isLastAttempt && autostart) {
                log.info("starting daemon...")
                if (startDaemon(compilerId, newJVMOptions, daemonOptions, reportingTargets)) {
                    log.info("daemon successfully started!!!")
                    reportingTargets.report(DaemonReportCategory.DEBUG, "new daemon started, trying to find it")
                }
            }
            null
        }
    }

    suspend fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions): Unit {
        connectToCompileService(
            compilerId,
            DaemonJVMOptions(),
            daemonOptions,
            DaemonReportingTargets(out = System.out),
            autostart = false,
            checkId = false
        )?.shutdown()
    }


    suspend fun shutdownCompileService(compilerId: CompilerId): Unit {
        shutdownCompileService(compilerId, DaemonOptions())
    }


    fun leaseCompileSession(compilerService: CompileServiceClientSide, aliveFlagPath: String?): Int =
        runBlocking(Unconfined) { compilerService.leaseCompileSession(aliveFlagPath) }.get()

    fun releaseCompileSession(compilerService: CompileServiceClientSide, sessionId: Int): Unit {
        runBlocking(Unconfined) { compilerService.releaseCompileSession(sessionId) }
    }

    fun compile(
        compilerService: CompileServiceClientSide,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        outputsCollector: ((File, List<File>) -> Unit)? = null,
        compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        reportSeverity: ReportSeverity = ReportSeverity.INFO,
        port: Int = findCallbackServerSocket(),
        profiler: Profiler = DummyProfiler()
    ): Int = profiler.withMeasure(this) {
        runBlocking(Unconfined) {
            val services = BasicCompilerServicesWithResultsFacadeServerServerSide(messageCollector, outputsCollector, port)
            log.info("[BasicCompilerServicesWithResultsFacadeServerServerSide] services.runServer()")
            val serverRun = services.runServer()
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
                null
            )
        }.get().also { log.info("CODE = $it") }
    }

    val COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY: String = "kotlin.daemon.client.options"

    data class ClientOptions(
        var stop: Boolean = false
    ) : OptionsGroup {
        override val mappers: List<PropMapper<*, *, *>>
            get() = listOf(BoolPropMapper(this, ClientOptions::stop))
    }

    private fun configureClientOptions(opts: ClientOptions): ClientOptions {
        System.getProperty(COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY)?.let {
            val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
            if (unrecognized.any())
                throw IllegalArgumentException(
                    "Unrecognized client options passed via property $COMPILE_DAEMON_OPTIONS_PROPERTY: " + unrecognized.joinToString(" ") +
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
                runBlocking(Unconfined) { daemon.shutdown() }
                log.info("Daemon shut down successfully")
            }
            filteredArgs.none() -> {
                // so far used only in tests
                log.info(
                    "Warning: empty arguments list, only daemon check is performed: checkCompilerId() returns ${runBlocking(Unconfined) {
                        daemon.checkCompilerId(
                            compilerId
                        )
                    }}"
                )
            }
            else -> runBlocking(Unconfined) {
                log.info("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                val servicesFacade = CompilerCallbackServicesFacadeServerSide()
                val serverRun = servicesFacade.runServer()
                try {
                    val memBefore = runBlocking(Unconfined) { daemon.getUsedMemory().get() } / 1024
                    val startTime = System.nanoTime()

                    val compResults = object : CompilationResultsServerSide {

                        override val serverPort: Int
                            get() = resultsPort

                        private val resultsPort = findPortForSocket(
                            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
                            RESULTS_SERVER_PORTS_RANGE_START,
                            RESULTS_SERVER_PORTS_RANGE_END
                        )

                        private val resultsMap = hashMapOf<Int, MutableList<Serializable>>()

                        override val clientSide: CompilationResultsClientSide
                            get() = CompilationResultsClientSideImpl(resultsPort)

                        override suspend fun add(compilationResultCategory: Int, value: Serializable) {
                            resultsMap.putIfAbsent(compilationResultCategory, mutableListOf())
                            resultsMap[compilationResultCategory]!!.add(value)
                            // TODO logger?
                        }

                    }
                    val compResultsServerRun = compResults.runServer()
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
//                    serverRun.await()
                } finally {
                    // TODO ??
                }
            }
        }
    }

    fun detectCompilerClasspath(): List<String>? =
        System.getProperty("java.class.path")
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

    @Synchronized
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
                }

                if (res != null) return res

                if (err != null) {
                    reportingTargets.report(
                        DaemonReportCategory.INFO,
                        (if (attempts >= DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) "no more retries on: " else "retrying($attempts) on: ")
                                + err.toString()
                    )
                }

                if (attempts++ > DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) {
                    return null
                }
            }
        } catch (e: Throwable) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        return null
    }

    private suspend fun tryFindSuitableDaemonOrNewOpts(
        registryDir: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        report: (DaemonReportCategory, String) -> Unit
    ): Pair<CompileServiceClientSide?, DaemonJVMOptions> {
        log.info("tryFindSuitableDaemonOrNewOpts")

        registryDir.mkdirs()
        val timestampMarker = createTempFile("kotlin-daemon-client-tsmarker", directory = registryDir)
        val aliveWithMetadata = try {
            walkDaemonsAsync(registryDir, compilerId, timestampMarker, report = report).also {
                log.info(
                    "daemons (${it.size}): ${it.map { "daemon(params : " + it.jvmOptions.jvmParams.joinToString(", ") + ")" }.joinToString(
                        ", ", "[", "]"
                    )}"
                )
            }
        } finally {
            timestampMarker.delete()
        }
        val comparator = compareBy<DaemonWithMetadataAsync, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator(), { it.jvmOptions })
            .thenBy(FileAgeComparator()) { it.runFile }
        val optsCopy = daemonJVMOptions.copy()
        // if required options fit into fattest running daemon - return the daemon and required options with memory params set to actual ones in the daemon
        return aliveWithMetadata.maxWith(comparator)
            ?.takeIf { daemonJVMOptions memorywiseFitsInto it.jvmOptions }
            ?.let {
                Pair(it.daemon, optsCopy.updateMemoryUpperBounds(it.jvmOptions))
            }
        // else combine all options from running daemon to get fattest option for a new daemon to runServer
                ?: Pair(null, aliveWithMetadata.fold(optsCopy, { opts, d -> opts.updateMemoryUpperBounds(d.jvmOptions) }))
    }


    private fun startDaemon(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets
    ): Boolean {
        log.info("in startDaemon() - 0")
        val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
        log.info("in startDaemon() - 0.1")
        val serverHostname = System.getProperty(JAVA_RMI_SERVER_HOSTNAME) ?: error("$JAVA_RMI_SERVER_HOSTNAME is not set!")
        log.info("in startDaemon() - 0.2")
        val platformSpecificOptions = listOf(
            // hide daemon window
            "-Djava.awt.headless=true",
            "-D$JAVA_RMI_SERVER_HOSTNAME=$serverHostname"
        )
        log.info("in startDaemon() - 0.3")
        val args = listOf(
            javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)
        ) +
                platformSpecificOptions +
                daemonJVMOptions.mappers.flatMap { it.toArgs("-") } +
                COMPILER_DAEMON_CLASS_FQN_EXPERIMENTAL +
                daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        log.info("in startDaemon() - 1")
        reportingTargets.report(DaemonReportCategory.DEBUG, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        log.info("in startDaemon() - 2")
        processBuilder.redirectErrorStream(true)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        log.info("daemon = launchProcessWithFallback")
        val daemon = launchProcessWithFallback(processBuilder, reportingTargets, "daemon client")

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
                                reportingTargets.report(
                                    DaemonReportCategory.DEBUG,
                                    "Received the message signalling that the daemon is ready"
                                )
                                isEchoRead.release()
                                //TODO return@forEachLine
                            } else {
                                reportingTargets.report(DaemonReportCategory.INFO, it, "daemon")
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
            val daemonStartupTimeout = System.getProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY)?.let {
                try {
                    it.toLong()
                } catch (e: Exception) {
                    reportingTargets.report(
                        DaemonReportCategory.INFO,
                        "unable to interpret $COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY property ('$it'); using default timeout $DAEMON_DEFAULT_STARTUP_TIMEOUT_MS ms"
                    )
                    null
                }
            } ?: DAEMON_DEFAULT_STARTUP_TIMEOUT_MS
            if (daemonOptions.runFilesPath.isNotEmpty()) {
                log.info("daemonOptions.runFilesPath.isNotEmpty")
                val succeeded = isEchoRead.tryAcquire(daemonStartupTimeout, TimeUnit.MILLISECONDS)
                log.info("succeeded : $succeeded")
                return when {
                    !isProcessAlive(daemon) -> {
                        log.info("!isProcessAlive(daemon)")
                        reportingTargets.report(
                            DaemonReportCategory.INFO,
                            "Daemon terminated unexpectedly with error code: ${daemon.exitValue()}"
                        )
                        false
                    }
                    !succeeded -> {
                        log.info("isProcessAlive!")
                        reportingTargets.report(DaemonReportCategory.INFO, "Unable to get response from daemon in $daemonStartupTimeout ms")
                        false
                    }
                    else -> true
                }
            } else
                log.info("!daemonOptions.runFilesPath.isNotEmpty")
            // without startEcho defined waiting for max timeout
            Thread.sleep(daemonStartupTimeout)
            return true
        } finally {
            // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
            if (stdoutThread.isAlive) {
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                stdoutThread.stop()
            }
            reportingTargets.out?.flush()
        }
    }
}

class DaemonReportingTargets(
    val out: PrintStream? = null,
    val messages: MutableCollection<DaemonReportMessage>? = null,
    val messageCollector: MessageCollector? = null,
    val compilerServices: CompilerServicesFacadeBaseAsync? = null
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
            DaemonReportCategory.DEBUG -> async { it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.DEBUG, message, source) }
            DaemonReportCategory.INFO -> async { it.report(ReportCategory.DAEMON_MESSAGE, ReportSeverity.INFO, message, source) }
            DaemonReportCategory.EXCEPTION -> async { it.report(ReportCategory.EXCEPTION, ReportSeverity.ERROR, message, source) }
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
