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

package org.jetbrains.kotlin.rmi.kotlinr

import net.rubygrapefruit.platform.ProcessLauncher
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.rmi.*
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


public class CompilationServices(
        val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
        val compilationCanceledStatus: CompilationCanceledStatus? = null
)


public object KotlinCompilerClient {

    val DAEMON_DEFAULT_STARTUP_TIMEOUT_MS = 10000L
    val DAEMON_CONNECT_CYCLE_ATTEMPTS = 3

    val verboseReporting = System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY) != null


    public fun connectToCompileService(compilerId: CompilerId,
                                       daemonJVMOptions: DaemonJVMOptions,
                                       daemonOptions: DaemonOptions,
                                       reportingTargets: DaemonReportingTargets,
                                       autostart: Boolean = true,
                                       checkId: Boolean = true
    ): CompileService? {
        fun newFlagFile(): File {
            val flagFile = File.createTempFile("kotlin-compiler-client-", "-is-running")
            flagFile.deleteOnExit()
            return flagFile
        }

        val flagFile = System.getProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)
                     ?.let { it.trimQuotes() }
                     ?.ifOrNull { !isBlank() }
                     ?.let { File(it) }
                     ?.ifOrNull { exists() }
                     ?: newFlagFile()
        return connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, reportingTargets, autostart)
    }

    public fun connectToCompileService(compilerId: CompilerId,
                                       clientAliveFlagFile: File,
                                       daemonJVMOptions: DaemonJVMOptions,
                                       daemonOptions: DaemonOptions,
                                       reportingTargets: DaemonReportingTargets,
                                       autostart: Boolean = true
    ): CompileService? {

        var attempts = 0
        try {
            while (attempts++ < DAEMON_CONNECT_CYCLE_ATTEMPTS) {
                val (service, newJVMOptions) = tryFindSuitableDaemonOrNewOpts(File(daemonOptions.runFilesPath), compilerId, daemonJVMOptions, { cat, msg -> reportingTargets.report(cat, msg) })
                if (service != null) {
                    // the newJVMOptions could be checked here for additional parameters, if needed
                    service.registerClient(clientAliveFlagFile.absolutePath)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
                    return service
                }
                else {
                    if (!autostart) return null
                    reportingTargets.report(DaemonReportCategory.DEBUG, "no suitable daemon found, starting a new one")
                }

                startDaemon(compilerId, newJVMOptions, daemonOptions, reportingTargets)
                reportingTargets.report(DaemonReportCategory.DEBUG, "daemon started, trying to find it")
            }
        }
        catch (e: Exception) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        return null
    }


    public fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions): Unit {
        KotlinCompilerClient.connectToCompileService(compilerId, DaemonJVMOptions(), daemonOptions, DaemonReportingTargets(out = System.out), autostart = false, checkId = false)
                ?.shutdown()
    }


    public fun shutdownCompileService(compilerId: CompilerId): Unit {
        shutdownCompileService(compilerId, DaemonOptions())
    }


    public fun compile(compilerService: CompileService,
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


    public fun incrementalCompile(compileService: CompileService,
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
                                                         compilationCancelledStatus = callbackServices.compilationCanceledStatus,
                                                         port = port),
                    RemoteOutputStreamServer(compilerOut, port),
                    CompileService.OutputFormat.XML,
                    RemoteOutputStreamServer(daemonOut, port),
                    operationsTracer).get()
    }

    public val COMPILE_DAEMON_CLIENT_OPTIONS_PROPERTY: String = "kotlin.daemon.client.options"
    data class ClientOptions(
            public var stop: Boolean = false
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
    public fun main(vararg args: String) {
        val compilerId = CompilerId()
        val daemonOptions = configureDaemonOptions()
        val daemonLaunchingOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)
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

                    val res = daemon.remoteCompile(CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, filteredArgs.toArrayList().toTypedArray(), servicesFacade, outStrm, CompileService.OutputFormat.PLAIN, outStrm, null)

                    val endTime = System.nanoTime()
                    println("Compilation result code: $res")
                    val memAfter = daemon.getUsedMemory().get() / 1024
                    println("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                    println("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                }
                finally {
                    // forcing RMI to unregister all objects and stop
                    java.rmi.server.UnicastRemoteObject.unexportObject(servicesFacade, true)
                    java.rmi.server.UnicastRemoteObject.unexportObject(outStrm, true)
                }
            }
        }
    }

    public fun detectCompilerClasspath(): List<String>? =
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

    private fun DaemonReportingTargets.report(category: DaemonReportCategory, message: String, source: String = "daemon client") {
        if (category == DaemonReportCategory.DEBUG && !verboseReporting) return
        out?.println("[$source] ${category.name}: $message")
        messages?.add(DaemonReportMessage(category, "[$source] $message"))
    }


    private fun tryFindSuitableDaemonOrNewOpts(registryDir: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, report: (DaemonReportCategory, String) -> Unit): Pair<CompileService?, DaemonJVMOptions> {
        val aliveWithOpts = walkDaemons(registryDir, compilerId, report)
                .map { Pair(it, it.getDaemonJVMOptions()) }
                .filter { it.second.isGood }
        // TODO: consider to sort the found daemons by memory settings and take the largest (rather that the first found), but carefully analyze possible situations first
        val opts = daemonJVMOptions.copy()
        return aliveWithOpts.firstOrNull { daemonJVMOptions memorywiseFitsInto it.second.get() }
                    ?.let { Pair(it.first, opts.updateMemoryUpperBounds(it.second.get())) }
               ?: Pair(null, aliveWithOpts.fold(daemonJVMOptions, { opts, d -> opts.updateMemoryUpperBounds(d.second.get()) }))
    }


    private fun startDaemon(compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, reportingTargets: DaemonReportingTargets) {
        val javaExecutable = File(File(System.getProperty("java.home"), "bin"), "java")
        val platformSpecificOptions = listOf("-Djava.awt.headless=true") // hide daemon window
        val args = listOf(
                   javaExecutable.absolutePath, "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)) +
                   platformSpecificOptions +
                   daemonJVMOptions.mappers.flatMap { it.toArgs("-") } +
                   COMPILER_DAEMON_CLASS_FQN +
                   daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        reportingTargets.report(DaemonReportCategory.DEBUG, "starting the daemon as: " + args.joinToString(" "))
        val processBuilder = ProcessBuilder(args)
        processBuilder.redirectErrorStream(true)
        // assuming daemon process is deaf and (mostly) silent, so do not handle streams
        val daemonLauncher = net.rubygrapefruit.platform.Native.get(ProcessLauncher::class.java)
        val daemon = daemonLauncher.start(processBuilder)

        var isEchoRead = Semaphore(1)
        isEchoRead.acquire()

        val stdoutThread =
                thread {
                    try {
                        daemon.inputStream
                                .reader()
                                .forEachLine {
                                    if (daemonOptions.runFilesPath.isNotEmpty() && it.contains(daemonOptions.runFilesPath)) {
                                        isEchoRead.release()
                                        return@forEachLine
                                    }
                                    reportingTargets.report(DaemonReportCategory.DEBUG, it, "daemon")
                                }
                    }
                    finally {
                        daemon.inputStream.close()
                        daemon.outputStream.close()
                        daemon.errorStream.close()
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
                    throw Exception("Daemon terminated unexpectedly")
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
        }
    }
}


public data class DaemonReportMessage(public val category: DaemonReportCategory, public val message: String)

public class DaemonReportingTargets(public val out: PrintStream? = null, public val messages: MutableCollection<DaemonReportMessage>? = null)


internal fun isProcessAlive(process: Process) =
        try {
            process.exitValue()
            false
        }
        catch (e: IllegalThreadStateException) {
            true
        }

internal fun<T> T.ifOrNull(pred: T.() -> Boolean): T? = if (this.pred()) this else null
