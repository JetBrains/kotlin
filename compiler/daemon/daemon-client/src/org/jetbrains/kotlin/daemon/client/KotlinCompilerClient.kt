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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.net.SocketException
import java.rmi.ConnectException
import java.rmi.ConnectIOException
import java.rmi.UnmarshalException
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class CompilationServices(
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val compilationCanceledStatus: CompilationCanceledStatus? = null
)

data class CompileServiceSession(val compileService: CompileService, val sessionId: Int)

object KotlinCompilerClient {

    val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY) != null

    val java9RestrictionsWorkaroundOptions =
            if (System.getProperty("java.specification.version") == "9") listOf(
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "--add-opens", "java.base/java.util=ALL-UNNAMED",
                    "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
                    "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED"
            )
            else emptyList()

    fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File =
            // for jps property is passed from IDEA to JPS in KotlinBuildProcessParametersProvider
            System.getProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)
                ?.let(String::trimQuotes)
                ?.takeUnless(String::isBlank)
                ?.let(::File)
                ?.takeIf(File::exists)
                ?: makeAutodeletingFlagFile(baseDir = File(daemonOptions.runFilesPathOrDefault))

    fun connectToCompileService(compilerId: CompilerId,
                                daemonJVMOptions: DaemonJVMOptions,
                                daemonOptions: DaemonOptions,
                                reportingTargets: DaemonReportingTargets,
                                autostart: Boolean = true,
                                checkId: Boolean = true
    ): CompileService? {
        val flagFile = getOrCreateClientFlagFile(daemonOptions)
        return connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, reportingTargets, autostart)
    }

    fun connectToCompileService(compilerId: CompilerId,
                                clientAliveFlagFile: File,
                                daemonJVMOptions: DaemonJVMOptions,
                                daemonOptions: DaemonOptions,
                                reportingTargets: DaemonReportingTargets,
                                autostart: Boolean = true
    ): CompileService? =
            connectAndLease(compilerId,
                            clientAliveFlagFile,
                            daemonJVMOptions,
                            daemonOptions,
                            reportingTargets,
                            autostart,
                            leaseSession = false,
                            sessionAliveFlagFile = null)?.compileService


    fun connectAndLease(compilerId: CompilerId,
                        clientAliveFlagFile: File,
                        daemonJVMOptions: DaemonJVMOptions,
                        daemonOptions: DaemonOptions,
                        reportingTargets: DaemonReportingTargets,
                        autostart: Boolean,
                        leaseSession: Boolean,
                        sessionAliveFlagFile: File? = null
    ): CompileServiceSession? = connectLoop(reportingTargets, autostart) { isLastAttempt ->
        ensureServerHostnameIsSetUp()
        val (service, newJVMOptions) = tryFindSuitableDaemonOrNewOpts(File(daemonOptions.runFilesPath), compilerId, daemonJVMOptions, { cat, msg -> reportingTargets.report(cat, msg) })
        if (service != null) {
            // the newJVMOptions could be checked here for additional parameters, if needed
            service.registerClient(clientAliveFlagFile.absolutePath)
            reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
            if (!leaseSession) CompileServiceSession(service, CompileService.NO_SESSION)
            else {
                val sessionId = service.leaseCompileSession(sessionAliveFlagFile?.absolutePath)
                if (sessionId is CompileService.CallResult.Dying)
                    null
                else
                    CompileServiceSession(service, sessionId.get())
            }
        } else {
            reportingTargets.report(DaemonReportCategory.DEBUG, "no suitable daemon found")
            if (!isLastAttempt && autostart) {
                startDaemon(compilerId, newJVMOptions, daemonOptions, reportingTargets)
                reportingTargets.report(DaemonReportCategory.DEBUG, "new daemon started, trying to find it")
            }
            null
        }
    }

    fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions): Unit {
        connectToCompileService(compilerId, DaemonJVMOptions(), daemonOptions, DaemonReportingTargets(out = System.out), autostart = false, checkId = false)
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

    @Deprecated("Use other compile method", ReplaceWith("compile"))
    fun compile(compilerService: CompileService,
                sessionId: Int,
                targetPlatform: CompileService.TargetPlatform,
                args: Array<out String>,
                out: OutputStream,
                port: Int = SOCKET_ANY_FREE_PORT,
                operationsTracer: RemoteOperationsTracer? = null
    ): Int {
        val outStrm = RemoteOutputStreamServer(out, port = port)
        return compilerService.remoteCompile(sessionId, targetPlatform, args, CompilerCallbackServicesFacadeServer(port = port), outStrm, CompileService.OutputFormat.PLAIN, outStrm, operationsTracer).get()
    }


    @Deprecated("Use non-deprecated compile method", ReplaceWith("compile"))
    fun incrementalCompile(compileService: CompileService,
                           sessionId: Int,
                           targetPlatform: CompileService.TargetPlatform,
                           args: Array<out String>,
                           callbackServices: CompilationServices,
                           compilerOut: OutputStream,
                           daemonOut: OutputStream,
                           port: Int = SOCKET_ANY_FREE_PORT,
                           profiler: Profiler = DummyProfiler(),
                           operationsTracer: RemoteOperationsTracer? = null
    ): Int = profiler.withMeasure(this) {
            compileService.remoteIncrementalCompile(
                    sessionId,
                    targetPlatform,
                    args,
                    CompilerCallbackServicesFacadeServer(incrementalCompilationComponents = callbackServices.incrementalCompilationComponents,
                                                         compilationCanceledStatus = callbackServices.compilationCanceledStatus,
                                                         port = port),
                    RemoteOutputStreamServer(compilerOut, port),
                    CompileService.OutputFormat.XML,
                    RemoteOutputStreamServer(daemonOut, port),
                    operationsTracer).get()
    }

    fun compile(compilerService: CompileService,
                sessionId: Int,
                targetPlatform: CompileService.TargetPlatform,
                args: Array<out String>,
                messageCollector: MessageCollector,
                outputsCollector: ((File, List<File>) -> Unit)? = null,
                compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                reportSeverity: ReportSeverity = ReportSeverity.INFO,
                port: Int = SOCKET_ANY_FREE_PORT,
                profiler: Profiler = DummyProfiler()
    ): Int = profiler.withMeasure(this) {
        val services = BasicCompilerServicesWithResultsFacadeServer(messageCollector, outputsCollector, port)
        compilerService.compile(
                sessionId,
                args,
                CompilationOptions(
                        compilerMode,
                        targetPlatform,
                        arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.DAEMON_MESSAGE.code, ReportCategory.EXCEPTION.code, ReportCategory.OUTPUT_MESSAGE.code),
                        reportSeverity.code,
                        emptyArray()),
                services,
                null
        ).get()
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
                        "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() }))
        }
        return opts
    }

    private fun configureClientOptions(): ClientOptions = configureClientOptions(ClientOptions())


    @JvmStatic
    fun main(vararg args: String) {
        val compilerId = CompilerId()
        val daemonOptions = configureDaemonOptions()
        val daemonLaunchingOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true)
        val clientOptions = configureClientOptions()
        val filteredArgs = args.asIterable().filterExtractProps(compilerId, daemonOptions, daemonLaunchingOptions, clientOptions, prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX)

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

        val daemon = connectToCompileService(compilerId, daemonLaunchingOptions, daemonOptions, DaemonReportingTargets(out = System.out), autostart = !clientOptions.stop, checkId = !clientOptions.stop)

        if (daemon == null) {
            if (clientOptions.stop) {
                System.err.println("No daemon found to shut down")
            }
            else throw Exception("Unable to connect to daemon")
        }
        else when {
            clientOptions.stop -> {
                println("Shutdown the daemon")
                daemon.shutdown()
                println("Daemon shut down successfully")
            }
            filteredArgs.none() -> {
                // so far used only in tests
                println("Warning: empty arguments list, only daemon check is performed: checkCompilerId() returns ${daemon.checkCompilerId(compilerId)}")
            }
            else -> {
                println("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                val outStrm = RemoteOutputStreamServer(System.out)
                val servicesFacade = CompilerCallbackServicesFacadeServer()
                try {
                    val memBefore = daemon.getUsedMemory().get() / 1024
                    val startTime = System.nanoTime()

                    val res = daemon.remoteCompile(CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, filteredArgs.toList().toTypedArray(), servicesFacade, outStrm, CompileService.OutputFormat.PLAIN, outStrm, null)

                    val endTime = System.nanoTime()
                    println("Compilation ${if (res.isGood) "succeeded" else "failed"}, result code: ${res.get()}")
                    val memAfter = daemon.getUsedMemory().get() / 1024
                    println("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                    println("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                }
                finally {
                    // forcing RMI to unregister all objects and stop
                    UnicastRemoteObject.unexportObject(servicesFacade, true)
                    UnicastRemoteObject.unexportObject(outStrm, true)
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
                }
                catch (e: SocketException) { null to e }
                catch (e: ConnectException) { null to e }
                catch (e: ConnectIOException) { null to e }
                catch (e: UnmarshalException) { null to e }

                if (res != null) return res

                reportingTargets.report(DaemonReportCategory.INFO,
                                        (if (attempts >= DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) "no more retries on: " else "retrying($attempts) on: ")
                                        + err?.toString())

                if (attempts++ > DAEMON_CONNECT_CYCLE_ATTEMPTS || !autostart) {
                    return null
                }
            }
        }
        catch (e: Throwable) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        return null
    }

    private fun tryFindSuitableDaemonOrNewOpts(registryDir: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, report: (DaemonReportCategory, String) -> Unit): Pair<CompileService?, DaemonJVMOptions> {
        registryDir.mkdirs()
        val timestampMarker = createTempFile("kotlin-daemon-client-tsmarker", directory = registryDir)
        val aliveWithMetadata = try {
            walkDaemons(registryDir, compilerId, timestampMarker, report = report).toList()
        }
        finally {
            timestampMarker.delete()
        }
        val comparator = compareBy<DaemonWithMetadata, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator(), { it.jvmOptions })
                .thenBy(FileAgeComparator()) { it.runFile }
        val optsCopy = daemonJVMOptions.copy()
        // if required options fit into fattest running daemon - return the daemon and required options with memory params set to actual ones in the daemon
        return aliveWithMetadata.maxWith(comparator)?.takeIf { daemonJVMOptions memorywiseFitsInto it.jvmOptions }?.let {
                Pair(it.daemon, optsCopy.updateMemoryUpperBounds(it.jvmOptions))
            }
            // else combine all options from running daemon to get fattest option for a new daemon to run
            ?: Pair(null, aliveWithMetadata.fold(optsCopy, { opts, d -> opts.updateMemoryUpperBounds(d.jvmOptions) }))
    }


    private fun startDaemon(compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, reportingTargets: DaemonReportingTargets) {
        val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
        val serverHostname = System.getProperty(JAVA_RMI_SERVER_HOSTNAME) ?: error("$JAVA_RMI_SERVER_HOSTNAME is not set!")
        val platformSpecificOptions = listOf(
                // hide daemon window
                "-Djava.awt.headless=true",
                "-D$JAVA_RMI_SERVER_HOSTNAME=$serverHostname")
        val args = listOf(
                   javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)) +
                   platformSpecificOptions +
                   daemonJVMOptions.mappers.flatMap { it.toArgs("-") } +
                   java9RestrictionsWorkaroundOptions +
                   COMPILER_DAEMON_CLASS_FQN +
                   daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        reportingTargets.report(DaemonReportCategory.DEBUG, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        processBuilder.redirectErrorStream(true)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        val daemon = launchProcessWithFallback(processBuilder, reportingTargets, "daemon client")

        val isEchoRead = Semaphore(1)
        isEchoRead.acquire()

        val stdoutThread =
                thread {
                    try {
                        daemon.inputStream
                                .reader()
                                .forEachLine {
                                    if (it == COMPILE_DAEMON_IS_READY_MESSAGE) {
                                        reportingTargets.report(DaemonReportCategory.DEBUG, "Received the message signalling that the daemon is ready")
                                        isEchoRead.release()
                                        return@forEachLine
                                    }
                                    else {
                                        reportingTargets.report(DaemonReportCategory.INFO, it, "daemon")
                                    }
                                }
                    }
                    finally {
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
                }
                catch (e: Exception) {
                    reportingTargets.report(DaemonReportCategory.INFO, "unable to interpret $COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY property ('$it'); using default timeout $DAEMON_DEFAULT_STARTUP_TIMEOUT_MS ms")
                    null
                }
            } ?: DAEMON_DEFAULT_STARTUP_TIMEOUT_MS
            if (daemonOptions.runFilesPath.isNotEmpty()) {
                val succeeded = isEchoRead.tryAcquire(daemonStartupTimeout, TimeUnit.MILLISECONDS)
                if (!isProcessAlive(daemon))
                    throw Exception("Daemon terminated unexpectedly with error code: ${daemon.exitValue()}")
                if (!succeeded)
                    throw Exception("Unable to get response from daemon in $daemonStartupTimeout ms")
            }
            else
            // without startEcho defined waiting for max timeout
                Thread.sleep(daemonStartupTimeout)
        }
        finally {
            // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
            if (stdoutThread.isAlive) {
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                stdoutThread.stop()
            }
            reportingTargets.out?.flush()
        }
    }
}


data class DaemonReportMessage(val category: DaemonReportCategory, val message: String)

class DaemonReportingTargets(val out: PrintStream? = null,
                             val messages: MutableCollection<DaemonReportMessage>? = null,
                             val messageCollector: MessageCollector? = null,
                             val compilerServices: CompilerServicesFacadeBase? = null)

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
        }
        catch (e: IllegalThreadStateException) {
            true
        }
