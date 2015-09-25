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

package org.jetbrains.kotlin.rmi.service

import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.rmi.*
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.concurrent.timer

val DAEMON_PERIODIC_CHECK_INTERVAL_MS = 1000L


class LogStream(name: String) : OutputStream() {

    val log by lazy { Logger.getLogger(name) }

    val lineBuf = StringBuilder()

    override fun write(byte: Int) {
        if (byte.toChar() == '\n') flush()
        else lineBuf.append(byte.toChar())
    }

    override fun flush() {
        log.info(lineBuf.toString())
        lineBuf.setLength(0)
    }
}


public object CompileDaemon {

    init {
        val logTime: String = SimpleDateFormat("yyyy-MM-dd.HH-mm-ss-SSS").format(Date())
        val (logPath: String, fileIsGiven: Boolean) =
                System.getProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY)?.trimQuotes()?.let { Pair(it, File(it).isFile) } ?: Pair("%t", false)
        val cfg: String =
                "handlers = java.util.logging.FileHandler\n" +
                "java.util.logging.FileHandler.level     = ALL\n" +
                "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
                "java.util.logging.FileHandler.encoding  = UTF-8\n" +
                "java.util.logging.FileHandler.limit     = ${if (fileIsGiven) 0 else (1 shl 20)}\n" + // if file is provided - disabled, else - 1Mb
                "java.util.logging.FileHandler.count     = ${if (fileIsGiven) 1 else 3}\n" +
                "java.util.logging.FileHandler.append    = $fileIsGiven\n" +
                "java.util.logging.FileHandler.pattern   = ${if (fileIsGiven) logPath else (logPath + File.separator + "$COMPILE_DAEMON_DEFAULT_FILES_PREFIX.$logTime.%u%g.log")}\n" +
                "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s\\n\n"

        LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
    }

    val log by lazy { Logger.getLogger("daemon") }

    private fun loadVersionFromResource(): String? {
        (CompileDaemon::class.java.classLoader as? URLClassLoader)
                ?.findResource("META-INF/MANIFEST.MF")
                ?.let {
                    try {
                        return Manifest(it.openStream()).mainAttributes.getValue("Implementation-Version") ?: null
                    }
                    catch (e: IOException) {}
                }
        return null
    }

    @JvmStatic
    public fun main(args: Array<String>) {

        log.info("Kotlin compiler daemon version " + (loadVersionFromResource() ?: "<unknown>"))
        log.info("daemon JVM args: " + ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" "))
        log.info("daemon args: " + args.joinToString(" "))

        val compilerId = CompilerId()
        val daemonOptions = DaemonOptions()

        try {
            val filteredArgs = args.asIterable().filterExtractProps(compilerId, daemonOptions, prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX)

            if (filteredArgs.any()) {
                val helpLine = "usage: <daemon> <compilerId options> <daemon options>"
                log.info(helpLine)
                println(helpLine)
                throw IllegalArgumentException("Unknown arguments: " + filteredArgs.joinToString(" "))
            }

            log.info("starting daemon")

            // TODO: find minimal set of permissions and restore security management
            // note: may be not needed anymore since (hopefully) server is now loopback-only
            //            if (System.getSecurityManager() == null)
            //                System.setSecurityManager (RMISecurityManager())
            //
            //            setDaemonPpermissions(daemonOptions.port)

            val (registry, port) = createRegistry(COMPILE_DAEMON_FIND_PORT_ATTEMPTS)
            val runFileDir = File(if (daemonOptions.runFilesPath.isBlank()) COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH else daemonOptions.runFilesPath)
            runFileDir.mkdirs()
            val runFile = File(runFileDir,
                               makeRunFilenameString(timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                                                     digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest(),
                                                     port = port.toString()))
            try {
                if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
            } catch (e: Exception) {
                throw IllegalStateException("Unable to create run file '${runFile.absolutePath}'", e)
            }
            runFile.deleteOnExit()

            val compiler = K2JVMCompiler()
            val compilerService = CompileServiceImpl(registry, compiler, compilerId, port)

            if (daemonOptions.runFilesPath.isNotEmpty())
                println(daemonOptions.runFilesPath)

            daemonOptions.clientAliveFlagPath?.let {
                if (!File(it).exists()) {
                    log.info("Client alive flag $it do not exist, disable watching it")
                    daemonOptions.clientAliveFlagPath = null
                }
            }

            // this supposed to stop redirected streams reader(s) on the client side and prevent some situations with hanging threads, but doesn't work reliably
            // TODO: implement more reliable scheme
            System.out.close()
            System.err.close()

            System.setErr(PrintStream(LogStream("stderr")))
            System.setOut(PrintStream(LogStream("stdout")))

            fun shutdownCondition(check: () -> Boolean, message: String): Boolean {
                val res = check()
                if (res) {
                    log.info(message)
                }
                return res
            }

            // stopping daemon if any shutdown condition is met
            timer(initialDelay = DAEMON_PERIODIC_CHECK_INTERVAL_MS, period = DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
                try {
                    val idleSeconds = nowSeconds() - compilerService.lastUsedSeconds
                    if (shutdownCondition({ !runFile.exists() }, "Run file removed, shutting down") ||
                        shutdownCondition({ daemonOptions.autoshutdownIdleSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && idleSeconds > daemonOptions.autoshutdownIdleSeconds },
                                          "Idle timeout exceeded ${daemonOptions.autoshutdownIdleSeconds}s, shutting down") ||
                        shutdownCondition({ daemonOptions.clientAliveFlagPath?.let { !File(it).exists() } ?: false },
                                          "Client alive flag ${daemonOptions.clientAliveFlagPath} removed, shutting down")) {
                        cancel()
                        compilerService.shutdown()
                    }
                }
                catch (e: Exception) {
                    System.err.println("Exception in timer thread: " + e.getMessage())
                    e.printStackTrace(System.err)
                }
            }
        }
        catch (e: Exception) {
            System.err.println("Exception: " + e.getMessage())
            e.printStackTrace(System.err)
            // repeating it to log for the cases when stderr is not redirected yet
            log.log(Level.INFO, "Exception: ", e)
            // TODO consider exiting without throwing
            throw e
        }
    }

    val random = Random()

    private fun createRegistry(attempts: Int) : Pair<Registry, Int> {
        var i = 0
        var lastException: RemoteException? = null

        while (i++ < attempts) {
            val port = random.nextInt(COMPILE_DAEMON_PORTS_RANGE_END - COMPILE_DAEMON_PORTS_RANGE_START) + COMPILE_DAEMON_PORTS_RANGE_START
            try {
                return Pair(LocateRegistry.createRegistry(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory), port)
            }
            catch (e: RemoteException) {
                // assuming that the port is already taken
                lastException = e
            }
        }
        throw IllegalStateException("Cannot find free port in $attempts attempts", lastException)
    }
}