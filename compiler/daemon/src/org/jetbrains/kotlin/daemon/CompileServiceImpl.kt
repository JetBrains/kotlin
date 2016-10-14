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

package org.jetbrains.kotlin.daemon

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.BufferedOutputStream
import java.io.File
import java.io.PrintStream
import java.rmi.NoSuchObjectException
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.comparisons.compareByDescending
import kotlin.concurrent.read
import kotlin.concurrent.schedule
import kotlin.concurrent.write

const val REMOTE_STREAM_BUFFER_SIZE = 4096

fun nowSeconds() = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())

interface CompilerSelector {
    operator fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*>
}

interface EventManger {
    fun onCompilationFinished(f : () -> Unit)
}

private class EventMangerImpl : EventManger {
    private val onCompilationFinished = arrayListOf<() -> Unit>()

    override fun onCompilationFinished(f: () -> Unit) {
        onCompilationFinished.add(f)
    }

    fun fireCompilationFinished() {
        onCompilationFinished.forEach { it() }
    }
}

class CompileServiceImpl(
        val registry: Registry,
        val compiler: CompilerSelector,
        val compilerId: CompilerId,
        val daemonOptions: DaemonOptions,
        val daemonJVMOptions: DaemonJVMOptions,
        val port: Int,
        val timer: Timer,
        val onShutdown: () -> Unit
) : CompileService {

    init {
        System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")
    }

    // wrapped in a class to encapsulate alive check logic
    private class ClientOrSessionProxy<out T: Any>(val aliveFlagPath: String?, val data: T? = null, private var disposable: Disposable? = null) {
        val registered = nowSeconds()
        val secondsSinceRegistered: Long get() = nowSeconds() - registered
        val isAlive: Boolean get() = aliveFlagPath?.let { File(it).exists() } ?: true // assuming that if no file was given, the client is alive
        fun dispose() {
            disposable?.let {
                Disposer.dispose(it)
                disposable = null
            }
        }
    }

    private val sessionsIdCounter = AtomicInteger(0)
    private val compilationsCounter = AtomicInteger(0)
    private val internalRng = Random()

    private val classpathWatcher = LazyClasspathWatcher(compilerId.compilerClasspath)

    enum class Aliveness {
        // !!! ordering of values is used in state comparison
        Dying, LastSession, Alive
    }

    // TODO: encapsulate operations on state here
    private val state = object {

        val clientProxies: MutableSet<ClientOrSessionProxy<Any>> = hashSetOf()
        val sessions: MutableMap<Int, ClientOrSessionProxy<Any>> = hashMapOf()

        val delayedShutdownQueued = AtomicBoolean(false)

        var alive = AtomicInteger(Aliveness.Alive.ordinal)
    }

    @Volatile private var _lastUsedSeconds = nowSeconds()
    val lastUsedSeconds: Long get() = if (rwlock.isWriteLocked || rwlock.readLockCount - rwlock.readHoldCount > 0) nowSeconds() else _lastUsedSeconds

    private val log by lazy { Logger.getLogger("compiler") }

    private val rwlock = ReentrantReadWriteLock()

    private var runFile: File

    init {
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        runFile = File(runFileDir,
                       makeRunFilenameString(timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                                             digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                                             port = port.toString()))
        try {
            if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
        } catch (e: Exception) {
            throw IllegalStateException("Unable to create run file '${runFile.absolutePath}'", e)
        }
        runFile.deleteOnExit()
    }

    // RMI-exposed API

    override fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = ifAlive {
        CompileService.CallResult.Good(daemonOptions)
    }

    override fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = ifAlive {
        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> = ifAlive {
        synchronized(state.clientProxies) {
            state.clientProxies.add(ClientOrSessionProxy(aliveFlagPath))
        }
        CompileService.CallResult.Ok()
    }

    override fun getClients(): CompileService.CallResult<List<String>> = ifAlive {
        synchronized(state.clientProxies) {
            CompileService.CallResult.Good(state.clientProxies.mapNotNull { it.aliveFlagPath })
        }
    }

    // TODO: consider tying a session to a client and use this info to cleanup
    override fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        CompileService.CallResult.Good(
                leaseSessionImpl(ClientOrSessionProxy<Any>(aliveFlagPath)).apply {
                    log.info("leased a new session $this, client alive file: $aliveFlagPath")
                })
    }

    private fun<T: Any> leaseSessionImpl(session: ClientOrSessionProxy<T>): Int {
        // fighting hypothetical integer wrapping
        var newId = sessionsIdCounter.incrementAndGet()
        for (attempt in 1..100) {
            if (newId != CompileService.NO_SESSION) {
                synchronized(state.sessions) {
                    if (!state.sessions.containsKey(newId)) {
                        state.sessions.put(newId, session)
                        return newId
                    }
                }
            }
            // assuming wrap, jumping to random number to reduce probability of further clashes
            newId = sessionsIdCounter.addAndGet(internalRng.nextInt())
        }
        throw IllegalStateException("Invalid state or algorithm error")
    }

    override fun releaseCompileSession(sessionId: Int) = ifAlive(minAliveness = Aliveness.LastSession) {
        synchronized(state.sessions) {
            state.sessions[sessionId]?.dispose()
            state.sessions.remove(sessionId)
            log.info("cleaning after session $sessionId")
            clearJarCache()
            if (state.sessions.isEmpty()) {
                // TODO: and some goes here
            }
        }
        timer.schedule(0) {
            periodicAndAfterSessionCheck()
        }
        CompileService.CallResult.Ok()
    }

    override fun checkCompilerId(expectedCompilerId: CompilerId): Boolean =
            (compilerId.compilerVersion.isEmpty() || compilerId.compilerVersion == expectedCompilerId.compilerVersion) &&
            (compilerId.compilerClasspath.all { expectedCompilerId.compilerClasspath.contains(it) }) &&
            !classpathWatcher.isChanged

    override fun getUsedMemory(): CompileService.CallResult<Long> =
            ifAlive { CompileService.CallResult.Good(usedMemory(withGC = true)) }

    override fun shutdown(): CompileService.CallResult<Nothing> = ifAliveExclusive(minAliveness = Aliveness.LastSession, ignoreCompilerChanged = true) {
        shutdownImpl()
        CompileService.CallResult.Ok()
    }

    override fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> = ifAlive(minAliveness = Aliveness.Alive) {
        CompileService.CallResult.Good(
                if (!graceful || state.alive.compareAndSet(Aliveness.Alive.ordinal, Aliveness.LastSession.ordinal)) {
                    timer.schedule(0) {
                        ifAliveExclusive(minAliveness = Aliveness.LastSession, ignoreCompilerChanged = true) {
                            if (!graceful || state.sessions.isEmpty()) {
                                shutdownImpl()
                            }
                            else {
                                log.info("Some sessions are active, waiting for them to finish")
                            }
                            CompileService.CallResult.Ok()
                        }
                    }
                    true
                }
                else false)
    }

    override fun remoteCompile(sessionId: Int,
                               targetPlatform: CompileService.TargetPlatform,
                               args: Array<out String>,
                               servicesFacade: CompilerCallbackServicesFacade,
                               compilerOutputStream: RemoteOutputStream,
                               outputFormat: CompileService.OutputFormat,
                               serviceOutputStream: RemoteOutputStream,
                               operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> =
            doCompile(sessionId, args, compilerOutputStream, serviceOutputStream, operationsTracer) { printStream, eventManager, profiler ->
                when (outputFormat) {
                    CompileService.OutputFormat.PLAIN -> compiler[targetPlatform].exec(printStream, *args)
                    CompileService.OutputFormat.XML -> compiler[targetPlatform].execAndOutputXml(printStream, createCompileServices(servicesFacade, eventManager, profiler), *args)
                }
            }

    override fun remoteIncrementalCompile(sessionId: Int,
                                          targetPlatform: CompileService.TargetPlatform,
                                          args: Array<out String>,
                                          servicesFacade: CompilerCallbackServicesFacade,
                                          compilerOutputStream: RemoteOutputStream,
                                          compilerOutputFormat: CompileService.OutputFormat,
                                          serviceOutputStream: RemoteOutputStream,
                                          operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> =
            doCompile(sessionId, args, compilerOutputStream, serviceOutputStream, operationsTracer) { printStream, eventManager, profiler ->
                when (compilerOutputFormat) {
                    CompileService.OutputFormat.PLAIN -> throw NotImplementedError("Only XML output is supported in remote incremental compilation")
                    CompileService.OutputFormat.XML -> compiler[targetPlatform].execAndOutputXml(printStream, createCompileServices(servicesFacade, eventManager, profiler), *args)
                }
            }

    override fun leaseReplSession(
            aliveFlagPath: String?,
            targetPlatform: CompileService.TargetPlatform,
            servicesFacade: CompilerCallbackServicesFacade,
            templateClasspath: List<File>,
            templateClassName: String,
            scriptArgs: Array<Any?>?,
            scriptArgsTypes: Array<Class<*>>?,
            compilerMessagesOutputStream: RemoteOutputStream,
            evalOutputStream: RemoteOutputStream?,
            evalErrorStream: RemoteOutputStream?,
            evalInputStream: RemoteInputStream?,
            operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        if (targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val repl = KotlinJvmReplService(disposable, templateClasspath, templateClassName, scriptArgs, scriptArgsTypes, compilerMessagesOutputStream, evalOutputStream, evalErrorStream, evalInputStream, operationsTracer)
            val sessionId = leaseSessionImpl(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    // TODO: add more checks (e.g. is it a repl session)
    override fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = releaseCompileSession(sessionId)

    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine, history: List<ReplCodeLine>): CompileService.CallResult<ReplCheckResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                withValidRepl(sessionId) {
                    check(codeLine, history)
                }
            }

    override fun remoteReplLineCompile(sessionId: Int, codeLine: ReplCodeLine, history: List<ReplCodeLine>): CompileService.CallResult<ReplCompileResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                withValidRepl(sessionId) {
                    compile(codeLine, history)
                }
            }

    override fun remoteReplLineEval(
            sessionId: Int,
            codeLine: ReplCodeLine,
            history: List<ReplCodeLine>
    ): CompileService.CallResult<ReplEvalResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                withValidRepl(sessionId) {
                    eval(codeLine, history)
                }
            }

    // internal implementation stuff

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
        registry.rebind (COMPILER_SERVICE_RMI_NAME, stub)

        timer.schedule(0) {
            initiateElections()
        }
        timer.schedule(delay = DAEMON_PERIODIC_CHECK_INTERVAL_MS, period = DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
            try {
                periodicAndAfterSessionCheck()
            }
            catch (e: Exception) {
                System.err.println("Exception in timer thread: " + e.message)
                e.printStackTrace(System.err)
                log.log(Level.SEVERE, "Exception in timer thread", e)
            }
        }
    }


    private fun periodicAndAfterSessionCheck() {

        ifAlive(minAliveness = Aliveness.LastSession) {

            // 1. check if unused for a timeout - shutdown
            if (shutdownCondition({ daemonOptions.autoshutdownUnusedSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && compilationsCounter.get() == 0 && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownUnusedSeconds },
                                  "Unused timeout exceeded ${daemonOptions.autoshutdownUnusedSeconds}s, shutting down")) {
                shutdown()
            }
            else {
                var anyDead = false
                var shuttingDown = false

                synchronized(state.sessions) {
                    // 2. check if any session hanged - clean
                    state.sessions.filterValues { !it.isAlive }.forEach {
                        it.value.dispose()
                        state.sessions.remove(it.key)
                        anyDead = true
                    }
                }

                // 3. check if in graceful shutdown state and all sessions are closed
                if (shutdownCondition({ state.alive.get() == Aliveness.LastSession.ordinal && state.sessions.none()}, "All sessions finished, shutting down")) {
                    shutdown()
                    shuttingDown = true
                }

                // 4. clean dead clients, then check if any left - conditional shutdown (with small delay)
                synchronized(state.clientProxies) {
                    state.clientProxies.removeAll(
                            state.clientProxies.filter { !it.isAlive }.map {
                                it.dispose()
                                anyDead = true
                                it
                            })
                }
                if (state.clientProxies.isEmpty() && compilationsCounter.get() > 0 && !state.delayedShutdownQueued.get()) {
                    log.info("No more clients left, delayed shutdown in ${daemonOptions.shutdownDelayMilliseconds}ms")
                    shutdownWithDelay()
                }
                // 5. check idle timeout - shutdown
                if (shutdownCondition({ daemonOptions.autoshutdownIdleSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownIdleSeconds },
                                      "Idle timeout exceeded ${daemonOptions.autoshutdownIdleSeconds}s, shutting down") ||
                    // 6. discovery file removed - shutdown
                    shutdownCondition({ !runFile.exists() }, "Run file removed, shutting down") ||
                    // 7. compiler changed (seldom check) - shutdown
                    // TODO: could be too expensive anyway, consider removing this check
                    shutdownCondition({ classpathWatcher.isChanged }, "Compiler changed"))
                {
                    shutdown()
                    shuttingDown = true
                }

                if (anyDead && !shuttingDown) {
                    clearJarCache()
                }
            }
            CompileService.CallResult.Ok()
        }
    }


    private fun initiateElections() {

        ifAlive {

            val aliveWithOpts = walkDaemons(File(daemonOptions.runFilesPathOrDefault), compilerId, filter = { f, p -> p != port }, report = { lvl, msg -> log.info(msg) })
                    .map { Pair(it, it.getDaemonJVMOptions()) }
                    .filter { it.second.isGood }
                    .sortedWith(compareByDescending(DaemonJVMOptionsMemoryComparator(), { it.second.get() }))
            if (aliveWithOpts.any()) {
                val fattestOpts = aliveWithOpts.first().second.get()
                // second part of the condition means that we prefer other daemon if is "equal" to the current one
                if (fattestOpts memorywiseFitsInto daemonJVMOptions && !(daemonJVMOptions memorywiseFitsInto fattestOpts)) {
                    // all others are smaller that me, take overs' clients and shut them down
                    aliveWithOpts.forEach {
                        it.first.getClients().check { it.isGood }?.let {
                            it.get().forEach { registerClient(it) }
                        }
                        it.first.scheduleShutdown(true)
                    }
                }
                else if (daemonJVMOptions memorywiseFitsInto fattestOpts) {
                    // there is at least one bigger, handover my clients to it and shutdown
                    scheduleShutdown(true)
                    aliveWithOpts.first().first.let { fattest ->
                        getClients().check { it.isGood }?.let {
                            it.get().forEach { fattest.registerClient(it) }
                        }
                    }
                }
                // else - do nothing, all daemons are staying
                // TODO: implement some behaviour here, e.g.:
                //   - shutdown/takeover smaller daemon
                //   - run (or better persuade client to run) a bigger daemon (in fact may be even simple shutdown will do, because of client's daemon choosing logic)
            }
            CompileService.CallResult.Ok()
        }
    }

    private fun shutdownImpl() {
        log.info("Shutdown started")
        state.alive.set(Aliveness.Dying.ordinal)

        UnicastRemoteObject.unexportObject(this, true)
        log.info("Shutdown complete")
        onShutdown()
    }

    private fun shutdownWithDelay() {
        state.delayedShutdownQueued.set(true)
        val currentCompilationsCount = compilationsCounter.get()
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            state.delayedShutdownQueued.set(false)
            if (currentCompilationsCount == compilationsCounter.get()) {
                log.fine("Execute delayed shutdown")
                shutdown()
            }
            else {
                log.info("Cancel delayed shutdown due to new client")
            }
        }
    }

    private inline fun shutdownCondition(check: () -> Boolean, message: String): Boolean {
        val res = check()
        if (res) {
            log.info(message)
        }
        return res
    }

    private fun doCompile(sessionId: Int,
                          args: Array<out String>,
                          compilerMessagesStreamProxy: RemoteOutputStream,
                          serviceOutputStreamProxy: RemoteOutputStream,
                          operationsTracer: RemoteOperationsTracer?,
                          body: (PrintStream, EventManger, Profiler) -> ExitCode): CompileService.CallResult<Int> =
            ifAlive {
                withValidClientOrSessionProxy(sessionId) { session ->
                    operationsTracer?.before("compile")
                    val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
                    val eventManger = EventMangerImpl()
                    val compilerMessagesStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(compilerMessagesStreamProxy, rpcProfiler), REMOTE_STREAM_BUFFER_SIZE))
                    val serviceOutputStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(serviceOutputStreamProxy, rpcProfiler), REMOTE_STREAM_BUFFER_SIZE))
                    try {
                        CompileService.CallResult.Good(
                                checkedCompile(args, serviceOutputStream, rpcProfiler) {
                                    body(compilerMessagesStream, eventManger, rpcProfiler).code
                                })
                    }
                    finally {
                        serviceOutputStream.flush()
                        compilerMessagesStream.flush()
                        eventManger.fireCompilationFinished()
                        operationsTracer?.after("compile")
                    }
                }
            }

    private fun createCompileServices(facade: CompilerCallbackServicesFacade, eventManger: EventManger, rpcProfiler: Profiler): Services {
        val builder = Services.Builder()
        if (facade.hasIncrementalCaches() || facade.hasLookupTracker()) {
            builder.register(IncrementalCompilationComponents::class.java, RemoteIncrementalCompilationComponentsClient(facade, eventManger, rpcProfiler))
        }
        if (facade.hasCompilationCanceledStatus()) {
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        }
        return builder.build()
    }


    private fun<R> checkedCompile(args: Array<out String>, serviceOut: PrintStream, rpcProfiler: Profiler, body: () -> R): R {
        try {
            if (args.none())
                throw IllegalArgumentException("Error: empty arguments list.")
            log.info("Starting compilation with args: " + args.joinToString(" "))

            val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            val res = profiler.withMeasure(null, body)

            val endMem = if (daemonOptions.reportPerf) usedMemory(withGC = false) else 0L

            log.info("Done with result " + res.toString())

            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                fun Long.kb() = this / 1024
                val pc = profiler.getTotalCounters()
                val rpc = rpcProfiler.getTotalCounters()

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; rpc: ${rpc.count} calls, ${rpc.time.ms()} ms, thread ${rpc.threadTime.ms()} ms; memory: ${endMem.kb()} kb (${"%+d".format(pc.memory.kb())} kb)".let {
                    serviceOut.println(it)
                    log.info(it)
                }

                // this will only be reported if if appropriate (e.g. ByClass) profiler is used
                for ((obj, counters) in rpcProfiler.getCounters()) {
                    "PERF: rpc by $obj: ${counters.count} calls, ${counters.time.ms()} ms, thread ${counters.threadTime.ms()} ms".let {
                        serviceOut.println(it)
                        log.info(it)
                    }
                }
            }
            return res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Exception) {
            log.info("Error: $e")
            throw e
        }
    }

    private fun clearJarCache() {
        ZipHandler.clearFileAccessorCache()
        (KotlinCoreEnvironment.applicationEnvironment?.jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
    }

    private fun<R> ifAlive(minAliveness: Aliveness = Aliveness.Alive,
                           ignoreCompilerChanged: Boolean = false,
                           body: () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> =
            rwlock.read {
                ifAliveChecksImpl(minAliveness, ignoreCompilerChanged, body)
            }

    private fun<R> ifAliveExclusive(minAliveness: Aliveness = Aliveness.Alive,
                                    ignoreCompilerChanged: Boolean = false,
                                    body: () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> =
            rwlock.write {
                ifAliveChecksImpl(minAliveness, ignoreCompilerChanged, body)
            }

    inline private fun<R> ifAliveChecksImpl(minAliveness: Aliveness = Aliveness.Alive, ignoreCompilerChanged: Boolean = false, body: () -> CompileService.CallResult<R>): CompileService.CallResult<R> =
        when {
            state.alive.get() < minAliveness.ordinal -> CompileService.CallResult.Dying()
            !ignoreCompilerChanged && classpathWatcher.isChanged -> {
                log.info("Compiler changed, scheduling shutdown")
                timer.schedule(0) { shutdown() }
                CompileService.CallResult.Dying()
            }
            else -> {
                try {
                    body()
                }
                catch (e: Exception) {
                    log.log(Level.SEVERE, "Exception", e)
                    CompileService.CallResult.Error(e.message ?: "unknown")
                }
            }
        }

    private inline fun<R> withValidClientOrSessionProxy(sessionId: Int,
                                                        body: (ClientOrSessionProxy<Any>?) -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val session: ClientOrSessionProxy<Any>? =
                if (sessionId == CompileService.NO_SESSION) null
                else state.sessions[sessionId] ?: return CompileService.CallResult.Error("Unknown or invalid session $sessionId")
        try {
            compilationsCounter.incrementAndGet()
            return body(session)
        }
        finally {
            _lastUsedSeconds = nowSeconds()
        }

    }

    private inline fun<R> withValidRepl(sessionId: Int, body: KotlinJvmReplService.() -> R): CompileService.CallResult<R> =
            withValidClientOrSessionProxy(sessionId) { session ->
                (session?.data as? KotlinJvmReplService?)?.let {
                    CompileService.CallResult.Good(it.body())
                } ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
            }
}
