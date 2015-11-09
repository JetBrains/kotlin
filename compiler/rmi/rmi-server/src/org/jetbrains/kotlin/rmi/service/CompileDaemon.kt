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

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.rmi.*
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
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)

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
            //            setDaemonPermissions(daemonOptions.port)

            val (registry, port) = findPortAndCreateRegistry(COMPILE_DAEMON_FIND_PORT_ATTEMPTS, COMPILE_DAEMON_PORTS_RANGE_START, COMPILE_DAEMON_PORTS_RANGE_END)

            val compilerSelector = object : CompilerSelector {
                private val jvm by lazy { K2JVMCompiler() }
                private val js by lazy { K2JSCompiler() }
                override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = when (targetPlatform) {
                    CompileService.TargetPlatform.JVM -> jvm
                    CompileService.TargetPlatform.JS -> js
                }
            }
            // timer with a daemon thread, meaning it should not prevent JVM to exit normally
            val timer = Timer(true)
            val compilerService = CompileServiceImpl(registry = registry,
                                                     compiler = compilerSelector,
                                                     compilerId = compilerId,
                                                     daemonOptions = daemonOptions,
                                                     daemonJVMOptions = daemonJVMOptions,
                                                     port = port,
                                                     timer = timer,
                                                     onShutdown = {
                                                         if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
                                                             // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
                                                             timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                                                                 cancel()
                                                                 log.info("force JVM shutdown")
                                                                 System.exit(0)
                                                             }
                                                         }
                                                         else {
                                                             timer.cancel()
                                                         }
                                                     })

            if (daemonOptions.runFilesPath.isNotEmpty())
                println(daemonOptions.runFilesPath)

            // this supposed to stop redirected streams reader(s) on the client side and prevent some situations with hanging threads, but doesn't work reliably
            // TODO: implement more reliable scheme
            System.out.close()
            System.err.close()

            System.setErr(PrintStream(LogStream("stderr")))
            System.setOut(PrintStream(LogStream("stdout")))
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
}