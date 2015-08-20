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
import org.jetbrains.kotlin.rmi.CompilerId
import org.jetbrains.kotlin.rmi.DaemonOptions
import org.jetbrains.kotlin.rmi.propParseFilter
import org.jetbrains.kotlin.service.CompileServiceImpl
import java.io.OutputStream
import java.io.PrintStream
import java.rmi.RMISecurityManager
import java.rmi.registry.LocateRegistry
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.platform.platformStatic

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

        platformStatic public fun main(args: Array<String>) {

            val compilerId = CompilerId()
            val daemonOptions = DaemonOptions()
            val filteredArgs = args.asIterable().propParseFilter(compilerId, daemonOptions)

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

            val registry = LocateRegistry.createRegistry(daemonOptions.port);
            val compiler = K2JVMCompiler()

            val server = CompileServiceImpl(registry, compiler, compilerId, daemonOptions)

            if (daemonOptions.startEcho.isNotEmpty())
                println(daemonOptions.startEcho)

            // this stops redirected streams reader(s) on the client side and prevent some situations with hanging threads
            System.out.close()
            System.err.close()

            System.setErr(PrintStream(LogStream("stderr")))
            System.setOut(PrintStream(LogStream("stdout")))
        }
    }
}