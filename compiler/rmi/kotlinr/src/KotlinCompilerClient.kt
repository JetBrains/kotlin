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

import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.rmi.*
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.rmi.ConnectException
import java.rmi.Remote
import java.rmi.registry.LocateRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write
import kotlin.platform.platformStatic

fun Process.isAlive() =
        try {
            this.exitValue()
            false
        }
        catch (e: IllegalThreadStateException) {
            true
        }

public class KotlinCompilerClient {

    companion object {

        val DAEMON_STARTUP_TIMEOUT_MS = 10000L
        val DAEMON_STARTUP_CHECK_INTERVAL_MS = 100L

        private fun connectToService(compilerId: CompilerId, daemonOptions: DaemonOptions, errStream: PrintStream): CompileService? {

            val compilerObj = connectToDaemon(compilerId, daemonOptions, errStream) ?: return null // no registry - no daemon running
            return compilerObj as? CompileService ?:
                throw ClassCastException("Unable to cast compiler service, actual class received: ${compilerObj.javaClass}")
        }

        private fun connectToDaemon(compilerId: CompilerId, daemonOptions: DaemonOptions, errStream: PrintStream): Remote? {
            try {
                val daemon = LocateRegistry.getRegistry("localhost", daemonOptions.port)
                        ?.lookup(COMPILER_SERVICE_RMI_NAME)
                if (daemon != null)
                    return daemon
                errStream.println("[daemon client] daemon not found")
            }
            catch (e: ConnectException) {
                errStream.println("[daemon client] cannot connect to registry: " + (e.getCause()?.getMessage() ?: e.getMessage() ?: "unknown exception"))
                // ignoring it - processing below
            }
            return null
        }


        private fun startDaemon(compilerId: CompilerId, daemonLaunchingOptions: DaemonLaunchingOptions, daemonOptions: DaemonOptions, errStream: PrintStream) {
            val javaExecutable = listOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
            // TODO: add some specific environment variables to the cp and may be command line, to allow some specific startup configs
            val args = listOf(javaExecutable,
                              "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator)) +
                       daemonLaunchingOptions.jvmParams +
                       COMPILER_DAEMON_CLASS_FQN +
                       daemonOptions.asParams +
                       compilerId.asParams
            errStream.println("[daemon client] starting the daemon as: " + args.joinToString(" "))
            val processBuilder = ProcessBuilder(args).redirectErrorStream(true)
            // assuming daemon process is deaf and (mostly) silent, so do not handle streams
            val daemon = processBuilder.start()

            val lock = ReentrantReadWriteLock()
            var isEchoRead = false

            val stdouThread =
                    thread {
                        daemon.getInputStream()
                              .reader()
                              .forEachLine {
                                  if (daemonOptions.startEcho.isNotEmpty() && it.contains(daemonOptions.startEcho))
                                      lock.write { isEchoRead = true; return@forEachLine }
                                  errStream.println("[daemon] " + it)
                              }
                    }
            try {
                // trying to wait for process
                if (daemonOptions.startEcho.isNotEmpty()) {
                    errStream.println("[daemon client] waiting for daemon to respond")
                    var waitMillis: Long = DAEMON_STARTUP_TIMEOUT_MS / DAEMON_STARTUP_CHECK_INTERVAL_MS
                    while (waitMillis-- > 0) {
                        Thread.sleep(DAEMON_STARTUP_CHECK_INTERVAL_MS)
                        if (!daemon.isAlive() || lock.read { isEchoRead } == true) break;
                    }
                    if (!daemon.isAlive())
                        throw Exception("Daemon terminated unexpectedly")
                    if (lock.read { isEchoRead } == false)
                        throw Exception("Unable to get response from daemon in $DAEMON_STARTUP_TIMEOUT_MS ms")
                }
                else
                // without startEcho defined waiting for max timeout
                    Thread.sleep(DAEMON_STARTUP_TIMEOUT_MS)
            }
            finally {
                // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
                if (stdouThread.isAlive)
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                    lock.write { stdouThread.stop() }
            }
        }

        public fun checkCompilerId(compiler: CompileService, localId: CompilerId, errStream: PrintStream): Boolean {
            val remoteId = compiler.getCompilerId()
            errStream.println("[daemon client] remoteId = " + remoteId.toString())
            errStream.println("[daemon client] localId = " + localId.toString())
            return (localId.compilerVersion.isEmpty() || localId.compilerVersion == remoteId.compilerVersion) &&
                   (localId.compilerClasspath.all { remoteId.compilerClasspath.contains(it) }) &&
                   (localId.compilerDigest.isEmpty() || remoteId.compilerDigest.isEmpty() || localId.compilerDigest == remoteId.compilerDigest)
        }

        public fun connectToCompileService(compilerId: CompilerId, daemonLaunchingOptions: DaemonLaunchingOptions, daemonOptions: DaemonOptions, errStream: PrintStream, autostart: Boolean = true, checkId: Boolean = true): CompileService? {
            val service = connectToService(compilerId, daemonOptions, errStream)
            if (service != null) {
                if (!checkId || checkCompilerId(service, compilerId, errStream)) {
                    errStream.println("[daemon client] found the suitable daemon")
                    return service
                }
                errStream.println("[daemon client] compiler identity don't match: " + compilerId.asParams.joinToString(" "))
                if (!autostart) return null;
                errStream.println("[daemon client] shutdown the daemon")
                service.shutdown()
                // TODO: find more reliable way
                Thread.sleep(1000)
                errStream.println("[daemon client] daemon shut down correctly, restarting")
            }
            else {
                if (!autostart) return null;
                else errStream.println("[daemon client] cannot connect to Compile Daemon, trying to start")
            }

            startDaemon(compilerId, daemonLaunchingOptions, daemonOptions, errStream)
            errStream.println("[daemon client] daemon started, trying to connect")
            return connectToService(compilerId, daemonOptions, errStream)
        }

        public fun incrementalCompile(compiler: CompileService, args: Array<String>, caches: Map<String, IncrementalCache>, out: OutputStream): Int {

            val outStrm = RemoteOutputStreamServer(out)
            val cacheServers = hashMapOf<String, RemoteIncrementalCacheServer>()
            try {
                caches.forEach { cacheServers.put( it.getKey(), RemoteIncrementalCacheServer( it.getValue())) }
                val res = compiler.remoteIncrementalCompile(args, cacheServers, outStrm, CompileService.OutputFormat.XML)
                return res
            }
            finally {
                cacheServers.forEach { it.getValue().disconnect() }
                outStrm.disconnect()
            }
        }

        public fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null

        data class ClientOptions(
                public var stop: Boolean = false
        ) :CmdlineParams {
            override val asParams: Iterable<String>
                get() =
                    if (stop) listOf("stop") else listOf()

            override val parsers: List<PropParser<*,*,*>>
                get() = listOf( BoolPropParser(this, ::stop))
        }

        platformStatic public fun main(vararg args: String) {
            val compilerId = CompilerId()
            val daemonOptions = DaemonOptions()
            val daemonLaunchingOptions = DaemonLaunchingOptions()
            val clientOptions = ClientOptions()
            val filteredArgs = args.asIterable().propParseFilter(compilerId, daemonOptions, daemonLaunchingOptions, clientOptions)

            if (!clientOptions.stop) {
                if (compilerId.compilerClasspath.none()) {
                    // attempt to find compiler to use
                    println("compiler wasn't explicitly specified, attempt to find appropriate jar")
                    System.getProperty("java.class.path")
                            ?.split(File.pathSeparator)
                            ?.map { File(it).parent }
                            ?.distinct()
                            ?.map {
                                it?.walk()
                                        ?.firstOrNull { it.getName().equals(COMPILER_JAR_NAME, ignoreCase = true) }
                            }
                            ?.filterNotNull()
                            ?.firstOrNull()
                            ?.let { compilerId.compilerClasspath = listOf(it.absolutePath) }
                }
                if (compilerId.compilerClasspath.none())
                    throw IllegalArgumentException("Cannot find compiler jar")
                else
                    println("desired compiler classpath: " + compilerId.compilerClasspath.joinToString(File.pathSeparator))

                compilerId.updateDigest()
            }

            connectToCompileService(compilerId, daemonLaunchingOptions, daemonOptions, System.out, autostart = !clientOptions.stop, checkId = !clientOptions.stop)?.let {
                when {
                    clientOptions.stop -> {
                        println("Shutdown the daemon")
                        it.shutdown()
                        println("Daemon shut down successfully")
                    }
                    else -> {
                        println("Executing daemon compilation with args: " + filteredArgs.joinToString(" "))
                        val outStrm = RemoteOutputStreamServer(System.out)
                        try {
                            val memBefore = it.getUsedMemory() / 1024
                            val startTime = System.nanoTime()
                            val res = it.remoteCompile(filteredArgs.toArrayList().toTypedArray(), outStrm, CompileService.OutputFormat.PLAIN)
                            val endTime = System.nanoTime()
                            println("Compilation result code: $res")
                            val memAfter = it.getUsedMemory() / 1024
                            println("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                            println("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                        }
                        finally {
                            outStrm.disconnect()
                        }
                    }
                }
            }
            ?: if (clientOptions.stop) println("No daemon found to shut down")
               else throw Exception("Unable to connect to daemon")
        }
    }
}

