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
import java.rmi.ConnectException
import java.rmi.Remote
import java.rmi.registry.LocateRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Logger
import kotlin.concurrent.read
import kotlin.concurrent.thread
import kotlin.concurrent.write
import kotlin.platform.platformStatic

val DAEMON_STARTUP_TIMEOUT_MILIS = 10000L
val DAEMON_STARTUP_CHECH_INTERVAL_MILIS = 100L

public class KotlinCompilerClient {

    companion object {

        val log: Logger by lazy { Logger.getLogger("KotlinDaemonClient") }

        private fun connectToService(compilerId: CompilerId, daemonOptions: DaemonOptions, log: Logger): CompileService? {

            val compilerObj = connectToDaemon(compilerId, daemonOptions, log) ?: return null // no registry - no daemon running
            return compilerObj as? CompileService ?:
                throw ClassCastException("Unable to cast compiler service, actual class received: ${compilerObj.javaClass}")
        }

        private fun connectToDaemon(compilerId: CompilerId, daemonOptions: DaemonOptions, log: Logger): Remote? {
            try {
                val daemon = LocateRegistry.getRegistry("localhost", daemonOptions.port)
                        ?.lookup(COMPILER_SERVICE_RMI_NAME)
                if (daemon != null)
                    return daemon
                log.info("Failed to find compile daemon")
            }
            catch (e: ConnectException) {
                log.info("Error connecting registry: " + e.toString())
                // ignoring it - processing below
            }
            return null
        }


        fun Process.isAlive() =
            try {
                this.exitValue()
                false
            }
            catch (e: IllegalThreadStateException) {
                true
            }


        private fun startDaemon(compilerId: CompilerId, daemonOptions: DaemonOptions, log: Logger) {
            val javaExecutable = listOf(System.getProperty("java.home"), "bin", "java").joinToString(File.separator)
            // TODO: add some specific environment variables to the cp and may be command line, to allow some specific startup configs
            val args = listOf(javaExecutable,
                              "-cp", compilerId.compilerClasspath.joinToString(File.pathSeparator),
                              COMPILER_DAEMON_CLASS_FQN) +
                       daemonOptions.asParams +
                       compilerId.asParams
            log.info("Starting the daemon as: " + args.joinToString(" "))
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
                                  log.info("daemon: " + it)
                              }
                    }
            // trying to wait for process
            if (daemonOptions.startEcho.isNotEmpty()) {
                log.info("waiting for daemon to respond")
                var waitMillis: Long = DAEMON_STARTUP_TIMEOUT_MILIS / DAEMON_STARTUP_CHECH_INTERVAL_MILIS
                while (waitMillis-- > 0) {
                    Thread.sleep(DAEMON_STARTUP_CHECH_INTERVAL_MILIS)
                    if (!daemon.isAlive() || lock.read { isEchoRead } == true) break;
                }
                if (!daemon.isAlive())
                    throw Exception("Daemon terminated unexpectedly")
                if (lock.read { isEchoRead } == false)
                    throw Exception("Unable to get response from daemon in $DAEMON_STARTUP_TIMEOUT_MILIS ms")
            }
            else
                // without startEcho defined waiting for max timeout
                Thread.sleep(DAEMON_STARTUP_TIMEOUT_MILIS)
            // assuming that all important output is already done, the rest should be routed to the log by the daemon itself
            if (stdouThread.isAlive)
                // TODO: find better method to stop the thread, but seems it will require asynchronous consuming of the stream
                lock.write { stdouThread.stop() }
        }

        public fun checkCompilerId(compiler: CompileService, localId: CompilerId, log: Logger): Boolean {
            val remoteId = compiler.getCompilerId()
            log.info("remoteId = " + remoteId.toString())
            log.info("localId = " + localId.toString())
            return (localId.compilerVersion.isEmpty() || localId.compilerVersion == remoteId.compilerVersion) &&
                   (localId.compilerClasspath.all { remoteId.compilerClasspath.contains(it) })
        }

        public fun connectToCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions, log: Logger): CompileService? {
            val service = connectToService(compilerId, daemonOptions, log)
            if (service != null) {
                if (checkCompilerId(service, compilerId, log)) {
                    log.info("Found the suitable daemon")
                    return service
                }
                log.info("Compiler identity don't match: " + compilerId.asParams.joinToString(" "))
                log.info("Shutdown the daemon")
                service.shutdown()
                // TODO: find more reliable way
                Thread.sleep(1000)
                log.info("Daemon shut down correctly, restarting")
            }
            else
                log.info("cannot connect to Compile Daemon, trying to start")
            startDaemon(compilerId, daemonOptions, log)
            log.info("Daemon started, trying to connect")
            return connectToService(compilerId, daemonOptions, log)
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


        platformStatic public fun main(vararg args: String) {
            val compilerId = CompilerId()
            val daemonOptions = DaemonOptions()
            val filteredArgs = args.asIterable().propParseFilter(compilerId, daemonOptions)

            if (compilerId.compilerClasspath.none()) {
                // attempt to find compiler to use
                log.info("compiler wasn't explicitly specified, attempt to find appropriate jar")
                System.getProperty("java.class.path")
                        ?.split(File.pathSeparator)
                        ?.map { File(it).parent }
                        ?.distinct()
                        ?.map { it?.walk()
                                ?.firstOrNull { it.getName().equals(COMPILER_JAR_NAME, ignoreCase = true) } }
                        ?.filterNotNull()
                        ?.firstOrNull()
                        ?.let { compilerId.compilerClasspath = listOf(it.absolutePath)}
            }
            if (compilerId.compilerClasspath.none())
                throw IllegalArgumentException("Cannot find compiler jar")
            else
                log.info("desired compiler classpath: " + compilerId.compilerClasspath.joinToString(File.pathSeparator))

            connectToCompileService(compilerId, daemonOptions, log)?.let {
                    log.info("Executing daemon compilation with args: " + args.joinToString(" "))
                    val outStrm = RemoteOutputStreamServer(System.out)
                    try {
                        val memBefore = it.getUsedMemory() / 1024
                        val startTime = System.nanoTime()
                        val res = it.remoteCompile(args, outStrm, CompileService.OutputFormat.PLAIN)
                        val endTime = System.nanoTime()
                        log.info("Compilation result code: $res")
                        val memAfter = it.getUsedMemory() / 1024
                        log.info("Compilation time: " + TimeUnit.NANOSECONDS.toMillis(endTime - startTime) + " ms")
                        log.info("Used memory $memAfter (${"%+d".format(memAfter - memBefore)} kb)")
                    }
                    finally {
                        outStrm.disconnect()
                    }
                }
            ?: throw Exception("Unable to connect to daemon")
        }
    }
}

