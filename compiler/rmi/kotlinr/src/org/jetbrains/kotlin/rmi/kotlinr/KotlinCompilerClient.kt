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
import java.io.*
import java.rmi.ConnectException
import java.rmi.registry.LocateRegistry
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


    // TODO: remove jvmStatic after all use sites will switch to kotlin
    @JvmStatic
    public fun connectToCompileService(compilerId: CompilerId,
                                       daemonJVMOptions: DaemonJVMOptions,
                                       daemonOptions: DaemonOptions,
                                       reportingTargets: DaemonReportingTargets,
                                       autostart: Boolean = true,
                                       checkId: Boolean = true
    ): CompileService? {

        var attempts = 0
        var fileLock: FileBasedLock? = null
        var shutdonwnPerformed = false
        try {
            while (attempts++ < DAEMON_CONNECT_CYCLE_ATTEMPTS) {
                val service = tryFindDaemon(File(daemonOptions.runFilesPath), compilerId, reportingTargets)
                if (service != null) {
                    if (!checkId || service.checkCompilerId(compilerId)) {
                        reportingTargets.report(DaemonReportCategory.DEBUG, "connected to the daemon")
                        return service
                    }
                    reportingTargets.report(DaemonReportCategory.DEBUG, "compiler identity don't match: " + compilerId.mappers.flatMap { it.toArgs("") }.joinToString(" "))
                    if (!autostart) return null
                    reportingTargets.report(DaemonReportCategory.DEBUG, "shutdown the daemon")
                    service.shutdown()
                    // TODO: find more reliable way
                    Thread.sleep(1000)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "daemon shut down correctly")
                    shutdonwnPerformed = true
                }
                else {
                    if (!autostart) return null
                    reportingTargets.report(DaemonReportCategory.DEBUG, if (shutdonwnPerformed) "starting a new daemon" else "no suitable daemon found, starting a new one")
                }

                if (fileLock == null || !fileLock.isLocked()) {
                    File(daemonOptions.runFilesPath).mkdirs()
                    fileLock = FileBasedLock(compilerId, daemonOptions)
                    // need to check the daemons again here, because of possible racing conditions
                    // note: the algorithm could be simpler if we'll acquire lock right from the beginning, but it may be costly
                    attempts--
                }
                else {
                    startDaemon(compilerId, daemonJVMOptions, daemonOptions, reportingTargets)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "daemon started, trying to find it")
                }
            }
        }
        catch (e: Exception) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, e.toString())
        }
        finally {
            fileLock?.release()
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
                       targetPlatform: CompileService.TargetPlatform,
                       args: Array<out String>,
                       out: OutputStream,
                       port: Int = SOCKET_ANY_FREE_PORT,
                       operationsTracer: RemoteOperationsTracer? = null
    ): Int {
        val outStrm = RemoteOutputStreamServer(out, port = port)
        return compilerService.remoteCompile(targetPlatform, args, CompilerCallbackServicesFacadeServer(port = port), outStrm, CompileService.OutputFormat.PLAIN, outStrm, operationsTracer)
    }


    public fun incrementalCompile(compileService: CompileService,
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
                    targetPlatform,
                    args,
                    CompilerCallbackServicesFacadeServer(incrementalCompilationComponents = callbackServices.incrementalCompilationComponents,
                                                         compilationCancelledStatus = callbackServices.compilationCanceledStatus,
                                                         port = port),
                    RemoteOutputStreamServer(compilerOut, port),
                    CompileService.OutputFormat.XML,
                    RemoteOutputStreamServer(daemonOut, port),
                    operationsTracer)
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
                    val memBefore = daemon.getUsedMemory() / 1024
                    val startTime = System.nanoTime()

                    val res = daemon.remoteCompile(CompileService.TargetPlatform.JVM, filteredArgs.toArrayList().toTypedArray(), servicesFacade, outStrm, CompileService.OutputFormat.PLAIN, outStrm, null)

                    val endTime = System.nanoTime()
                    println("Compilation result code: $res")
                    val memAfter = daemon.getUsedMemory() / 1024
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
            ?.map {
                it?.walk()
                        ?.firstOrNull { it.name.equals(COMPILER_JAR_NAME, ignoreCase = true) }
            }
            ?.filterNotNull()
            ?.firstOrNull()
            ?.let { listOf(it.absolutePath) }

    // --- Implementation ---------------------------------------

    fun DaemonReportingTargets.report(category: DaemonReportCategory, message: String, source: String = "daemon client") {
        if (category == DaemonReportCategory.DEBUG && !verboseReporting) return
        out?.println("[$source] ${category.name()}: $message")
        messages?.add(DaemonReportMessage(category, "[$source] $message"))
    }


    private fun String.extractPortFromRunFilename(digest: String): Int =
            makeRunFilenameString(timestamp = "[0-9TZ:\\.\\+-]+", digest = digest, port = "(\\d+)", escapeSequence = "\\").toRegex()
                    .find(this)
            ?.groups?.get(1)
            ?.value?.toInt()
            ?: 0


    private fun tryFindDaemon(registryDir: File, compilerId: CompilerId, reportingTargets: DaemonReportingTargets): CompileService? {
        val classPathDigest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString()
        val daemons = registryDir.walk()
                .map { Pair(it, it.name.extractPortFromRunFilename(classPathDigest)) }
                .filter { it.second != 0 }
                .map {
                    assert(it.second > 0 && it.second < 0xffff)
                    reportingTargets.report(DaemonReportCategory.DEBUG, "found suitable daemon on port ${it.second}, trying to connect")
                    val daemon = tryConnectToDaemon(it.second, reportingTargets)
                    // cleaning orphaned file; note: daemon should shut itself down if it detects that the run file is deleted
                    if (daemon == null && !it.first.delete()) {
                        reportingTargets.report(DaemonReportCategory.INFO, "WARNING: unable to delete seemingly orphaned file '${it.first.absolutePath}', cleanup recommended")
                    }
                    daemon
                }
                .filterNotNull()
                .toList()
        return when (daemons.size) {
            0 -> null
            1 -> daemons.first()
            else -> throw IllegalStateException("Multiple daemons serving the same compiler, reset with the cleanup required")
        // TODO: consider implementing automatic recovery instead, e.g. getting the youngest or least used daemon and shut down others
        }
    }


    private fun tryConnectToDaemon(port: Int, reportingTargets: DaemonReportingTargets): CompileService? {
        try {
            val daemon = LocateRegistry.getRegistry(LoopbackNetworkInterface.loopbackInetAddressName, port)
                    ?.lookup(COMPILER_SERVICE_RMI_NAME)
            if (daemon != null)
                return daemon as? CompileService ?:
                       throw ClassCastException("Unable to cast compiler service, actual class received: ${daemon.javaClass}")
            reportingTargets.report(DaemonReportCategory.EXCEPTION, "daemon not found")
        }
        catch (e: ConnectException) {
            reportingTargets.report(DaemonReportCategory.EXCEPTION, "cannot connect to registry: " + (e.getCause()?.getMessage() ?: e.getMessage() ?: "unknown exception"))
            // ignoring it - processing below
        }
        return null
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


    class FileBasedLock(compilerId: CompilerId, daemonOptions: DaemonOptions) {

        private val lockFile: File =
                File(daemonOptions.runFilesPath,
                     makeRunFilenameString(timestamp = "lock",
                                           digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                                           port = "0"))
        @Volatile private var locked = acquireLockFile(lockFile)
        private val channel = if (locked) RandomAccessFile(lockFile, "rw").channel else null
        private val lock = channel?.lock()

        public fun isLocked(): Boolean = locked

        @Synchronized public fun release(): Unit {
            if (locked) {
                lock?.release()
                channel?.close()
                lockFile.delete()
                locked = false
            }
        }

        @Synchronized private fun acquireLockFile(lockFile: File): Boolean {
            val maxAttempts = COMPILE_DAEMON_STARTUP_LOCK_TIMEOUT_MS / COMPILE_DAEMON_STARTUP_LOCK_TIMEOUT_CHECK_MS + 1
            var attempt = 0L
            while (true) {
                if (lockFile.createNewFile()) break
                // attempt to delete if file is orphaned
                if (lockFile.delete() && lockFile.createNewFile()) break
                if (lockFile.exists() && ++attempt >= maxAttempts)
                    throw IOException("Timeout waiting the release of the lock file '${lockFile.absolutePath}")
                Thread.sleep(COMPILE_DAEMON_STARTUP_LOCK_TIMEOUT_CHECK_MS)
            }
            return true
        }
    }
}


public enum class DaemonReportCategory {
    DEBUG, INFO, EXCEPTION;
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

