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
import org.jetbrains.kotlin.service.CompileServiceImpl
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
import java.util.logging.LogManager
import java.util.logging.Logger

class LogStream(name: String) : OutputStream() {

    val log by lazy { Logger.getLogger(name) }

    val lineBuf = StringBuilder()

    override fun write(byte: Int) {
        if (byte.toChar() == '\n') flush()
        else lineBuf.append(byte.toChar())
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        var ofs = offset
        var lineStart = ofs
        while (ofs < length) {
            if (data[ofs].toChar() == '\n') {
                flush(data, lineStart, ofs - lineStart)
                lineStart = ofs + 1
            }
            ofs++
        }
        if (lineStart < length)
            lineBuf.append(data, lineStart, length - lineStart)
    }

    fun flush(data: ByteArray, offset: Int, length: Int) {
        log.info(lineBuf.toString() + data.toString().substring(offset, length))
        lineBuf.setLength(0)
    }

    override fun flush() {
        log.info(lineBuf.toString())
        lineBuf.setLength(0)
    }
}


public class CompileDaemon {

    companion object {

        init {
            val logPath: String = System.getProperty("kotlin.daemon.log.path")?.trimEnd('/','\\') ?: "%t"
            val logTime: String = SimpleDateFormat("yyyy-MM-dd.HH-mm-ss-SSS").format(Date())
            val cfg: String =
                "handlers = java.util.logging.FileHandler\n" +
                "java.util.logging.FileHandler.level     = ALL\n" +
                "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
                "java.util.logging.FileHandler.encoding  = UTF-8\n" +
                "java.util.logging.FileHandler.limit     = 1073741824\n" + // 1Mb
                "java.util.logging.FileHandler.count     = 3\n" +
                "java.util.logging.FileHandler.append    = false\n" +
                "java.util.logging.FileHandler.pattern   = $logPath/kotlin-daemon.$logTime.%g.log\n" +
                "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s\\n\n"

            LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
        }

        val log by lazy { Logger.getLogger("daemon") }

        private fun loadVersionFromResource(): String? {
            (javaClass.classLoader as? URLClassLoader)
                    ?.findResource("META-INF/MANIFEST.MF")
                    ?.let {
                        try {
                            return Manifest(it.openStream()).mainAttributes.getValue("Implementation-Version") ?: ""
                        }
                        catch (e: IOException) {}
                    }
            return null
        }

        jvmStatic public fun main(args: Array<String>) {

            log.info("Kotlin compiler daemon version " + (loadVersionFromResource() ?: "<unknown>"))
            log.info("daemon JVM args: " + ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" "))
            log.info("daemon args: " + args.joinToString(" "))

            val compilerId = CompilerId()
            val daemonOptions = DaemonOptions()
            val filteredArgs = args.asIterable().filterExtractProps(compilerId, daemonOptions, prefix = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX)

            if (filteredArgs.any()) {
                val helpLine = "usage: <daemon> <compilerId options> <daemon options>"
                log.info(helpLine)
                println(helpLine)
                throw IllegalArgumentException("Unknown arguments")
            }

            log.info("starting daemon")

            // TODO: find minimal set of permissions and restore security management
//            if (System.getSecurityManager() == null)
//                System.setSecurityManager (RMISecurityManager())
//
//            setDaemonPpermissions(daemonOptions.port)

            val (registry, port) = createRegistry(COMPILE_DAEMON_FIND_PORT_ATTEMPTS)
            val runFileDir = File(daemonOptions.runFilesPath)
            runFileDir.mkdirs()
            val runFile = File(runFileDir,
                 makeRunFilenameString(ts = "%tFT%<tRZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                                       digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest(),
                                       port = port.toString()))
            if (!runFile.createNewFile()) {
                throw IllegalStateException("Unable to create run file '${runFile.absolutePath}'")
            }
            runFile.deleteOnExit()

            val compiler = K2JVMCompiler()
            CompileServiceImpl(registry, compiler, compilerId, daemonOptions)

            if (daemonOptions.runFilesPath.isNotEmpty())
                println(runFile.name)

            // this stops redirected streams reader(s) on the client side and prevent some situations with hanging threads
            System.out.close()
            System.err.close()

            System.setErr(PrintStream(LogStream("stderr")))
            System.setOut(PrintStream(LogStream("stdout")))
        }

        val random = Random()

        private fun createRegistry(attempts: Int) : Pair<Registry, Int> {
            var i = 0
            var lastException: RemoteException? = null
            while (i++ < attempts) {
                val port = random.nextInt(COMPILE_DAEMON_PORTS_RANGE_END - COMPILE_DAEMON_PORTS_RANGE_START) + COMPILE_DAEMON_PORTS_RANGE_START
                try {
                    return Pair(LocateRegistry.createRegistry(port), port)
                }
                catch (e: RemoteException) {
                    // assuming that the port is already taken
                    lastException = e
                }
            }
            throw IllegalStateException("Cannot find free port in $attempts attempts", lastException)
        }
    }
}