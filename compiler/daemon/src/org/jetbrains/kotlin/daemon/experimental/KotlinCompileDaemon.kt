/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket
import org.jetbrains.kotlin.daemon.common.impls.ensureServerHostnameIsSetUp
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.net.URLClassLoader
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.concurrent.schedule


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

object KotlinCompileDaemon {

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
                    "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s%n\n"

        LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
    }

    val log by lazy { Logger.getLogger("daemon") }

    private fun loadVersionFromResource(): String? {
        (KotlinCompileDaemon::class.java.classLoader as? URLClassLoader)
            ?.findResource("META-INF/MANIFEST.MF")
            ?.let {
                try {
                    return Manifest(it.openStream()).mainAttributes.getValue("Implementation-Version") ?: null
                } catch (e: IOException) {
                }
            }
        return null
    }

    @JvmStatic
    fun main(args: Array<String>) {

        log.info("main")

        ensureServerHostnameIsSetUp()

        val jvmArguments = ManagementFactory.getRuntimeMXBean().inputArguments

        log.info("Kotlin compiler daemon version " + (loadVersionFromResource() ?: "<unknown>"))
        log.info("daemon JVM args: " + jvmArguments.joinToString(" "))
        log.info("daemon args: " + args.joinToString(" "))

        setIdeaIoUseFallback()

        val compilerId = CompilerId()

        log.info("compilerId: " + compilerId)

        val daemonOptions = DaemonOptions()

        log.info("daemonOptions: " + daemonOptions)

        runBlocking {

            var serverRun: Deferred<Unit>?

            try {

                log.info("in try")

                val daemonJVMOptions = configureDaemonJVMOptions(
                    inheritMemoryLimits = true,
                    inheritOtherJvmOptions = true,
                    inheritAdditionalProperties = true
                )

                log.info("daemonJVMOptions: " + daemonJVMOptions)

                val filteredArgs = args.asIterable()
                    .filterExtractProps(
                        compilerId,
                        daemonOptions,
                        prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX
                    )

                log.info("filteredArgs: " + filteredArgs)

                if (filteredArgs.any()) {
                    val helpLine = "usage: <daemon> <compilerId options> <daemon options>"

                    log.info(helpLine)
                    println(helpLine)
                    throw IllegalArgumentException("Unknown arguments: " + filteredArgs.joinToString(" "))
                }

                log.info("starting_daemon")

                // TODO: find minimal set of permissions and restore security management
                // note: may be not needed anymore since (hopefully) server is now loopback-only
                //            if (System.getSecurityManager() == null)
                //                System.setSecurityManager (RMISecurityManager())
                //
                //            setDaemonPermissions(daemonOptions.socketPort)

                val port = findPortForSocket(
                    COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
                    COMPILE_DAEMON_PORTS_RANGE_START,
                    COMPILE_DAEMON_PORTS_RANGE_END
                )
                log.info("findPortForSocket() returned port= $port")


                val compilerSelector = object : CompilerSelector {
                    private val jvm by lazy { K2JVMCompiler() }
                    private val js by lazy { K2JSCompiler() }
                    private val metadata by lazy { K2MetadataCompiler() }
                    override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = when (targetPlatform) {
                        CompileService.TargetPlatform.JVM -> jvm
                        CompileService.TargetPlatform.JS -> js
                        CompileService.TargetPlatform.METADATA -> metadata
                    }
                }
                log.info("compilerSelector_ok")

                // timer with a daemon thread, meaning it should not prevent JVM to exit normally
                val timer = Timer(true)
                log.info("_STARTING_COMPILE_SERVICE")
                val compilerService = CompileServiceServerSideImpl(
                    port,
                    compilerSelector,
                    compilerId,
                    daemonOptions,
                    daemonJVMOptions,
                    port.port,
                    timer,
                    {
                        if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
                            // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
                            timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                                cancel()
                                log.info("force JVM shutdown")
                                System.exit(0)
                            }
                        } else {
                            timer.cancel()
                        }
                    })
                log.info("_COMPILE_SERVICE_STARTED")
                log.info("_compile_service_RUNNING_SEERVER")
                serverRun = compilerService.runServer()
                log.info("_compile_service_SEERVER_IS_RUNNING")


                println(COMPILE_DAEMON_IS_READY_MESSAGE)
                log.info("daemon is listening on port: ${port.port}")

                // this supposed to stop redirected streams reader(s) on the client side and prevent some situations with hanging threads, but doesn't work reliably
                // TODO: implement more reliable scheme
                System.out.close()
                System.err.close()

                System.setErr(PrintStream(LogStream("stderr")))
                System.setOut(PrintStream(LogStream("stdout")))
            } catch (e: Exception) {
                log.log(Level.ALL, "Exception: " + e.message)
                e.printStackTrace(System.err)
                // repeating it to log for the cases when stderr is not redirected yet
                log.log(Level.INFO, "Exception: ", e)
                // TODO consider exiting without throwing
                throw e
            }
            log.info("awaiting")
            serverRun.await()
            log.info("downing")
        }
    }

}