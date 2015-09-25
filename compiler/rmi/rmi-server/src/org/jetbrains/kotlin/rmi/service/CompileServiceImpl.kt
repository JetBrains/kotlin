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
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.rmi.*
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.rmi.NoSuchObjectException
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Logger
import kotlin.concurrent.read
import kotlin.concurrent.write


fun nowSeconds() = System.nanoTime() / 1000000000L

class CompileServiceImpl<Compiler: CLICompiler<*>>(
        val registry: Registry,
        val compiler: Compiler,
        val selfCompilerId: CompilerId,
        port: Int
) : CompileService, UnicastRemoteObject() {

    // RMI-exposed API

    override fun getCompilerId(): CompilerId = ifAlive { selfCompilerId }

    override fun getUsedMemory(): Long = ifAlive { usedMemory() }

    override fun shutdown() {
        ifAliveExclusive {
            log.info("Shutdown started")
            alive = false
            UnicastRemoteObject.unexportObject(this, true)
            log.info("Shutdown complete")
        }
    }

    override fun remoteCompile(args: Array<out String>,
                               services: CompileService.RemoteCompilationServices,
                               compilerOutputStream: RemoteOutputStream,
                               outputFormat: CompileService.OutputFormat,
                               serviceOutputStream: RemoteOutputStream
    ): Int =
            doCompile(args, compilerOutputStream, serviceOutputStream) { printStream ->
                when (outputFormat) {
                    CompileService.OutputFormat.PLAIN -> compiler.exec(printStream, *args)
                    CompileService.OutputFormat.XML -> compiler.execAndOutputXml(printStream, Services.EMPTY, *args)
                }
            }

    override fun remoteIncrementalCompile(args: Array<out String>,
                                          services: CompileService.RemoteCompilationServices,
                                          compilerOutputStream: RemoteOutputStream,
                                          compilerOutputFormat: CompileService.OutputFormat,
                                          serviceOutputStream: RemoteOutputStream
    ): Int =
            doCompile(args, compilerOutputStream, serviceOutputStream) { printStream ->
                when (compilerOutputFormat) {
                    CompileService.OutputFormat.PLAIN -> throw NotImplementedError("Only XML output is supported in remote incremental compilation")
                    CompileService.OutputFormat.XML -> compiler.execAndOutputXml(printStream, createCompileServices(services), *args)
                }
            }

    // internal implementation stuff

    @Volatile private var _lastUsedSeconds = nowSeconds()
    public val lastUsedSeconds: Long get() = if (rwlock.isWriteLocked || rwlock.readLockCount - rwlock.readHoldCount > 0) nowSeconds() else _lastUsedSeconds

    val log by lazy { Logger.getLogger("compiler") }

    private val rwlock = ReentrantReadWriteLock()
    private var alive = false

    // TODO: consider matching compilerId coming from outside with actual one
    //    private val selfCompilerId by lazy {
    //        CompilerId(
    //                compilerClasspath = System.getProperty("java.class.path")
    //                                            ?.split(File.pathSeparator)
    //                                            ?.map { File(it) }
    //                                            ?.filter { it.exists() }
    //                                            ?.map { it.absolutePath }
    //                                    ?: listOf(),
    //                compilerVersion = loadKotlinVersionFromResource()
    //        )
    //    }

    init {
        // assuming logically synchronized
        try {
            // cleanup for the case of incorrect restart and many other situations
            UnicastRemoteObject.unexportObject(this, false)
        }
        catch (e: NoSuchObjectException) {
            // ignoring if object already exported
        }

        val stub = UnicastRemoteObject.exportObject(this, port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) as CompileService
        registry.rebind (COMPILER_SERVICE_RMI_NAME, stub);
        alive = true
    }

    private fun doCompile(args: Array<out String>, compilerMessagesStreamProxy: RemoteOutputStream, serviceOutputStreamProxy: RemoteOutputStream, body: (PrintStream) -> ExitCode): Int =
            ifAlive {
                val compilerMessagesStream = PrintStream(RemoteOutputStreamClient(compilerMessagesStreamProxy))
                val serviceOutputStream = PrintStream(RemoteOutputStreamClient(serviceOutputStreamProxy))
                checkedCompile(args, serviceOutputStream) {
                    val res = body( compilerMessagesStream).code
                    _lastUsedSeconds = nowSeconds()
                    res
                }
            }

    private fun createCompileServices(services: CompileService.RemoteCompilationServices): Services {
        val builder = Services.Builder()
        services.incrementalCompilationComponents?.let { builder.register(IncrementalCompilationComponents::class.java, RemoteIncrementalCompilationComponentsClient(it)) }
        services.compilationCanceledStatus?.let { builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(it)) }
        return builder.build()
    }


    fun usedMemory(): Long {
        System.gc()
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory())
    }

    fun ThreadMXBean.threadCpuTime() = if (isCurrentThreadCpuTimeSupported) currentThreadCpuTime else 0L
    fun ThreadMXBean.threadUserTime() = if (isCurrentThreadCpuTimeSupported) currentThreadUserTime else 0L

    fun<R> checkedCompile(args: Array<out String>, serviceOut: PrintStream, body: () -> R): R {
        try {
            if (args.none())
                throw IllegalArgumentException("Error: empty arguments list.")
            log.info("Starting compilation with args: " + args.joinToString(" "))
            val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
            val startMem = usedMemory() / 1024
            val startTime = System.nanoTime()
            val startThreadTime = threadMXBean.threadCpuTime()
            val startThreadUserTime = threadMXBean.threadUserTime()
            val res = body()
            val endTime = System.nanoTime()
            val endThreadTime = threadMXBean.threadCpuTime()
            val endThreadUserTime = threadMXBean.threadUserTime()
            val endMem = usedMemory() / 1024
            log.info("Done with result " + res.toString())
            val elapsed = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)
            val elapsedThread = TimeUnit.NANOSECONDS.toMillis(endThreadTime - startThreadTime)
            val elapsedThreadUser = TimeUnit.NANOSECONDS.toMillis(endThreadUserTime - startThreadUserTime)
            log.info("Elapsed time: $elapsed ms (thread user: $elapsedThreadUser ms sys: ${elapsedThread - elapsedThreadUser} ms)")
            log.info("Used memory: $endMem kb (${"%+d".format(endMem - startMem)} kb)")
            System.getProperty(COMPILE_DAEMON_REPORT_PERF_PROPERTY)?.let {
                serviceOut.println("PERF: Compile on daemon: $elapsed ms (thread user: $elapsedThreadUser ms sys: ${elapsedThread - elapsedThreadUser} ms); memory: $endMem kb (${"%+d".format(endMem - startMem)} kb)")
            }
            return res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Exception) {
            log.info("Error: $e")
            throw e
        }
    }

    fun<R> ifAlive(body: () -> R): R = rwlock.read {
        if (!alive) throw IllegalStateException("Kotlin Compiler Service is not in alive state")
        body()
    }

    fun<R> ifAliveExclusive(body: () -> R): R = rwlock.write {
        if (!alive) throw IllegalStateException("Kotlin Compiler Service is not in alive state")
        body()
    }

    // sometimes used for debugging
    fun<R> spy(msg: String, body: () -> R): R {
        val res = body()
        log.info(msg + " = " + res.toString())
        return res
    }

}
