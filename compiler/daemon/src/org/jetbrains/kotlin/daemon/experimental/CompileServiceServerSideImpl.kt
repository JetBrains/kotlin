/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.kotlin.daemon.experimental

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.CompileServiceImplBase
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.EventManager
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import org.jetbrains.kotlin.daemon.experimental.CompileServiceTaskScheduler.*
import org.jetbrains.kotlin.daemon.nowSeconds
import org.jetbrains.kotlin.daemon.report.experimental.CompileServicesFacadeMessageCollector
import org.jetbrains.kotlin.daemon.report.experimental.DaemonMessageReporterAsync
import org.jetbrains.kotlin.daemon.report.experimental.getICReporterAsync
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.experimental.CompilationCanceledStatus
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.schedule

// TODO: this classes should replace their non-experimental versions eventually.

private class CompileServiceTaskScheduler(log: Logger) {
    interface CompileServiceTask
    interface CompileServiceTaskWithResult : CompileServiceTask

    open class ExclusiveTask(val completed: CompletableDeferred<Boolean>, val shutdownAction: suspend () -> Any) : CompileServiceTask
    open class ShutdownTaskWithResult(val result: CompletableDeferred<Any>, shutdownAction: suspend () -> Any) :
        ExclusiveTask(CompletableDeferred(), shutdownAction), CompileServiceTaskWithResult

    open class OrdinaryTask(val completed: CompletableDeferred<Boolean>, val action: suspend () -> Any) : CompileServiceTask
    class OrdinaryTaskWithResult(val result: CompletableDeferred<Any>, action: suspend () -> Any) :
        OrdinaryTask(CompletableDeferred(), action),
        CompileServiceTaskWithResult

    class TaskFinished(val taskId: Int) : CompileServiceTask
    class ExclusiveTaskFinished : CompileServiceTask

    private var shutdownActionInProgress = false
    private var readLocksCount = 0

    suspend fun scheduleTask(task: CompileServiceTask) = queriesActor.send(task)

    fun getReadLocksCnt() = readLocksCount

    fun isShutdownActionInProgress() = shutdownActionInProgress

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    private val queriesActor = GlobalScope.actor<CompileServiceTask>(capacity = Channel.UNLIMITED) {
        var currentTaskId = 0
        var shutdownTask: ExclusiveTask? = null
        val activeTaskIds = arrayListOf<Int>()
        val waitingTasks = arrayListOf<CompileServiceTask>()
        fun shutdownIfInactive(reason: String) {
            log.fine("invoked \'shutdownIfInactive\', reason : $reason")
            if (activeTaskIds.isEmpty()) {
                shutdownTask?.let { task ->
                    shutdownActionInProgress = true
                    GlobalScope.async {
                        val res = task.shutdownAction()
                        task.completed.complete(true)
                        if (task is ShutdownTaskWithResult) {
                            task.result.complete(res)
                        }
                        channel.send(ExclusiveTaskFinished())
                    }
                }
            }
        }
        consumeEach { task ->
            when (task) {
                is ExclusiveTask -> {
                    if (shutdownTask == null) {
                        shutdownTask = task
                        shutdownIfInactive("ExclusiveTask")
                    } else {
                        waitingTasks.add(task)
                    }
                }
                is OrdinaryTask -> {
                    if (shutdownTask == null) {
                        val id = currentTaskId++
                        activeTaskIds.add(id)
                        readLocksCount++
                        GlobalScope.async {
                            val res = task.action()
                            if (task is OrdinaryTaskWithResult) {
                                task.result.complete(res)
                            }
                            task.completed.complete(true)
                            channel.send(TaskFinished(id))
                        }
                    } else {
                        waitingTasks.add(task)
                    }
                }
                is TaskFinished -> {
                    activeTaskIds.remove(task.taskId)
                    readLocksCount--
                    shutdownIfInactive("TaskFinished")
                }
                is ExclusiveTaskFinished -> {
                    shutdownTask = null
                    shutdownActionInProgress = false
                    waitingTasks.forEach {
                        channel.send(it)
                    }
                    waitingTasks.clear()
                }
            }
        }
    }
}

class CompileServiceServerSideImpl(
    override val serverSocketWithPort: ServerSocketWrapper,
    val compiler: CompilerSelector,
    compilerId: CompilerId,
    daemonOptions: DaemonOptions,
    val daemonJVMOptions: DaemonJVMOptions,
    port: Int,
    timer: Timer,
    val onShutdown: () -> Unit
) : CompileServiceServerSide, CompileServiceImplBase(daemonOptions, compilerId, port, timer) {

    lateinit var rmiServer: CompileServiceRMIWrapper

    private inline fun <R> withValidRepl(
        sessionId: Int,
        body: KotlinJvmReplServiceAsync.() -> CompileService.CallResult<R>
    ) = withValidReplImpl(sessionId, body)

    override val serverPort: Int
        get() = serverSocketWithPort.port

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    object KeepAliveServer : Server<ServerBase> {
        override val serverSocketWithPort = findCallbackServerSocket()
        override val clients = hashMapOf<Socket, Server.ClientInfo>()

    }

    override suspend fun checkClientCanReadFile(clientInputChannel: ByteReadChannelWrapper): Boolean = runWithTimeout {
        getSignatureAndVerify(clientInputChannel, securityData.token, securityData.publicKey)
    } ?: false

    override suspend fun serverHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger): Boolean {
        return tryAcquireHandshakeMessage(input) && trySendHandshakeMessage(output)
    }

    private var scheduler: CompileServiceTaskScheduler

    constructor(
        serverSocket: ServerSocketWrapper,
        compilerId: CompilerId,
        daemonOptions: DaemonOptions,
        daemonJVMOptions: DaemonJVMOptions,
        port: Int,
        timer: Timer,
        onShutdown: () -> Unit
    ) : this(
        serverSocket,
        object : CompilerSelector {
            private val jvm by lazy { K2JVMCompiler() }
            private val js by lazy { K2JSCompiler() }
            private val metadata by lazy { K2MetadataCompiler() }
            override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = when (targetPlatform) {
                CompileService.TargetPlatform.JVM -> jvm
                CompileService.TargetPlatform.JS -> js
                CompileService.TargetPlatform.METADATA -> metadata
            }
        },
        compilerId,
        daemonOptions,
        daemonJVMOptions,
        port,
        timer,
        onShutdown
    )

    override val lastUsedSeconds: Long
        get() = (if (scheduler.getReadLocksCnt() > 1 || scheduler.isShutdownActionInProgress()) nowSeconds() else _lastUsedSeconds).also {
            log.fine(
                "lastUsedSeconds .. isReadLockedCNT : ${scheduler.getReadLocksCnt()} , " +
                        "shutdownActionInProgress : ${scheduler.isShutdownActionInProgress()}"
            )
        }

    private var securityData: SecurityData = generateKeysAndToken().also { sdata ->
        runFile.outputStream().use {
            sendTokenKeyPair(it, sdata.token, sdata.privateKey)
        }
    }

    // RMI-exposed API

    override suspend fun getDaemonInfo(): CompileService.CallResult<String> =
        ifAlive(minAliveness = Aliveness.Dying) {
            CompileService.CallResult.Good("Kotlin daemon on socketPort $port")
        }

    override suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = ifAlive {
        CompileService.CallResult.Good(daemonOptions)
    }

    override suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = ifAlive {
        log.info("getDaemonJVMOptions: $daemonJVMOptions")// + daemonJVMOptions.mappers.flatMap { it.toArgs("-") })
        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        log.fine("fun registerClient")
        return ifAlive(minAliveness = Aliveness.Alive) {
            registerClientImpl(aliveFlagPath)
        }
    }

    override suspend fun classesFqNamesByFiles(
        sessionId: Int, sourceFiles: Set<File>
    ): CompileService.CallResult<Set<String>> =
        ifAlive {
            withValidClientOrSessionProxy(sessionId) {
                CompileService.CallResult.Good(classesFqNames(sourceFiles))
            }
        }

    private fun registerClientImpl(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        state.addClient(aliveFlagPath)
        log.info("Registered a client alive file: $aliveFlagPath")
        return CompileService.CallResult.Ok()
    }

    override suspend fun getClients(): CompileService.CallResult<List<String>> = ifAlive {
        getClientsImpl()
    }

    private fun getClientsImpl() = CompileService.CallResult.Good(state.getClientsFlagPaths())

    // TODO: consider tying a session to a client and use this info to cleanup
    override suspend fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> =
        ifAlive(minAliveness = Aliveness.Alive) {
            CompileService.CallResult.Good(
                state.sessions.leaseSession(ClientOrSessionProxy<Any>(aliveFlagPath)).apply {
                    log.info("leased a new session $this, session alive file: $aliveFlagPath")
                })
        }

    override suspend fun releaseCompileSession(sessionId: Int) = ifAlive(minAliveness = Aliveness.LastSession) {
        state.sessions.remove(sessionId)
        log.info("cleaning after session $sessionId")
        val completed = CompletableDeferred<Boolean>()
        scheduler.scheduleTask(ExclusiveTask(completed, { clearJarCache() }))
        completed.await()
        postReleaseCompileSession()
    }


    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean =
        (compilerId.compilerVersion.isEmpty() || compilerId.compilerVersion == expectedCompilerId.compilerVersion) &&
                (compilerId.compilerClasspath.all { expectedCompilerId.compilerClasspath.contains(it) }) &&
                !classpathWatcher.isChanged

    override suspend fun getUsedMemory(): CompileService.CallResult<Long> =
        ifAlive { CompileService.CallResult.Good(usedMemory(withGC = true)) }

    override suspend fun shutdown(): CompileService.CallResult<Nothing> =
        ifAliveExclusive(minAliveness = Aliveness.LastSession) {
            shutdownWithDelay()
            CompileService.CallResult.Ok()
        }

    override suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> =
        ifAlive(minAliveness = Aliveness.LastSession) {
            scheduleShutdownImpl(graceful)
        }

    private fun scheduleShutdownImpl(graceful: Boolean): CompileService.CallResult<Boolean> {
        val res = when {
            graceful -> gracefulShutdown(true)
            else -> {
                shutdownWithDelay()
                true
            }
        }
        return CompileService.CallResult.Good(res)
    }

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        compilationResults: CompilationResultsAsync?
    ) = compileImpl(
        sessionId,
        compilerArguments,
        compilationOptions,
        servicesFacade,
        compilationResults,
        hasIncrementalCaches = { hasIncrementalCaches() },
        createMessageCollector = ::CompileServicesFacadeMessageCollector,
        createReporter = ::DaemonMessageReporterAsync,
        createServices = { facade: CompilerCallbackServicesFacadeClientSide, eventMgr, profiler ->
            createCompileServices(facade, eventMgr, profiler)
        },
        getICReporter = ::getICReporterAsync
    )

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        if (compilationOptions.targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val messageCollector =
                CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
            val repl = KotlinJvmReplServiceAsync(
                disposable, serverSocketWithPort, compilerId, templateClasspath, templateClassName,
                messageCollector
            )
            val sessionId = state.sessions.leaseSession(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    // TODO: add more checks (e.g. is it a repl session)
    override suspend fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = releaseCompileSession(sessionId)

    override suspend fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacadeClientSide> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                CompileService.CallResult.Good(
                    createRemoteState(findReplServerSocket()).clientSide
                )
            }
        }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult> = ifAlive(minAliveness = Aliveness.Alive) {
        withValidRepl(sessionId) {
            withValidReplState(replStateId) { state ->
                check(state, codeLine)
            }
        }
    }

    override suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCompileResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                withValidReplState(replStateId) { state ->
                    compile(state, codeLine)
                }
            }
        }

    init {

        scheduler = CompileServiceTaskScheduler(log)

        // assuming logically synchronized
        System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        // TODO UNCOMMENT THIS : this.toRMIServer(daemonOptions, compilerId) // also create RMI server in order to support old clients
//        rmiServer = this.toRMIServer(daemonOptions, compilerId)

        KeepAliveServer.runServer()
    }

    override fun periodicAndAfterSessionCheck() {
        if (state.delayedShutdownQueued.get()) return

        val anyDead = state.sessions.cleanDead() || state.cleanDeadClients()

        GlobalScope.async {
            ifAliveUnit(minAliveness = Aliveness.LastSession) {
                when {
                    // check if in graceful shutdown state and all sessions are closed
                    state.alive.get() == Aliveness.LastSession.ordinal && state.sessions.isEmpty() -> {
                        log.info("All sessions finished")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                    state.aliveClientsCount == 0 -> {
                        log.info("No more clients left")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                    // discovery file removed - shutdown
                    !runFile.exists() -> {
                        log.info("Run file removed")
                        shutdownWithDelay()
                        return@ifAliveUnit
                    }
                }
            }


            ifAliveUnit(minAliveness = Aliveness.Alive) {
                when {
                    daemonOptions.autoshutdownUnusedSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && compilationsCounter.get() == 0 && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownUnusedSeconds -> {
                        log.info("Unused timeout exceeded ${daemonOptions.autoshutdownUnusedSeconds}s")
                        gracefulShutdown(false)
                    }
                    daemonOptions.autoshutdownIdleSeconds != COMPILE_DAEMON_TIMEOUT_INFINITE_S && nowSeconds() - lastUsedSeconds > daemonOptions.autoshutdownIdleSeconds -> {
                        log.info("Idle timeout exceeded ${daemonOptions.autoshutdownIdleSeconds}s")
                        gracefulShutdown(false)
                    }
                    anyDead -> {
                        clearJarCache()
                    }
                }
            }
        }
    }

    override fun periodicSeldomCheck() {
        GlobalScope.async {
            ifAliveUnit(minAliveness = Aliveness.Alive) {
                // compiler changed (seldom check) - shutdown
                if (classpathWatcher.isChanged) {
                    log.info("Compiler changed.")
                    gracefulShutdown(false)
                }
            }
        }
    }


    // TODO: handover should include mechanism for client to switch to a new daemon then previous "handed over responsibilities" and shot down
    override fun initiateElections() {
        runBlocking(Dispatchers.Unconfined) {
            ifAliveUnit {
                log.info("initiate elections")
                val aliveWithOpts = walkDaemonsAsync(
                    File(daemonOptions.runFilesPathOrDefault),
                    compilerId,
                    runFile,
                    filter = { _, p -> p != port },
                    report = { _, msg -> log.info(msg) },
                    useRMI = false
                )
                log.fine("aliveWithOpts : ${aliveWithOpts.map { it.daemon.javaClass.name }}")
                val comparator = compareByDescending<DaemonWithMetadataAsync, DaemonJVMOptions>(
                    DaemonJVMOptionsMemoryComparator(),
                    { it.jvmOptions }
                )
                    .thenBy {
                        when (it.daemon) {
                            is CompileServiceAsyncWrapper -> 0
                            else -> 1
                        }
                    }
                    .thenBy(FileAgeComparator()) { it.runFile }
                    .thenBy { it.daemon.serverPort }
                aliveWithOpts.maxWithOrNull(comparator)?.let { bestDaemonWithMetadata ->
                    val fattestOpts = bestDaemonWithMetadata.jvmOptions
                    if (fattestOpts memorywiseFitsInto daemonJVMOptions && FileAgeComparator().compare(
                            bestDaemonWithMetadata.runFile,
                            runFile
                        ) < 0
                    ) {
                        // all others are smaller that me, take overs' clients and shut them down
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE lower prio, taking clients from them and schedule them to shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        aliveWithOpts.forEach { (daemon, runFile, _) ->
                            try {
                                log.fine("other : $daemon")
                                daemon.getClients().takeIf { it.isGood }?.let {
                                    it.get().forEach { clientAliveFile ->
                                        registerClientImpl(clientAliveFile)
                                    }
                                }
                                log.fine("other : CLIENTS_OK")
                                daemon.scheduleShutdown(true)
                                log.fine("other : SHUTDOWN_OK")
                            } catch (e: Throwable) {
                                log.info("Cannot connect to a daemon, assuming dying ('${runFile.canonicalPath}'): ${e.message}")
                            }
                        }
                    }
                    // TODO: seems that the second part of condition is incorrect, reconsider:
                    // the comment by @tsvtkv from review:
                    //    Algorithm in plain english:
                    //    (1) If the best daemon fits into me and the best daemon is younger than me, then I take over all other daemons clients.
                    //    (2) If I fit into the best daemon and the best daemon is older than me, then I give my clients to that daemon.
                    //
                    //    For example:
                    //
                    //    daemon A starts with params: maxMem=100, codeCache=50
                    //    daemon B starts with params: maxMem=200, codeCache=50
                    //    daemon C starts with params: maxMem=150, codeCache=100
                    //    A performs election: (1) is false because neither B nor C does not fit into A, (2) is false because both B and C are younger than A.
                    //    B performs election: (1) is false because neither A nor C does not fit into B, (2) is false because B does not fit into neither A nor C.
                    //    C performs election: (1) is false because B is better than A and B does not fit into C, (2) is false C does not fit into neither A nor B.
                    //    Result: all daemons are alive and well.
                    else if (daemonJVMOptions memorywiseFitsInto fattestOpts && FileAgeComparator().compare(
                            bestDaemonWithMetadata.runFile,
                            runFile
                        ) > 0
                    ) {
                        // there is at least one bigger, handover my clients to it and shutdown
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE higher prio, handover clients to it and schedule shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        getClientsImpl().takeIf { it.isGood }?.let {
                            it.get().forEach { bestDaemonWithMetadata.daemon.registerClient(it) }
                        }
                        scheduleShutdownImpl(true)
                    } else {
                        // undecided, do nothing
                        log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE equal prio, continue: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                        // TODO: implement some behaviour here, e.g.:
                        //   - shutdown/takeover smaller daemon
                        //   - runServer (or better persuade client to runServer) a bigger daemon (in fact may be even simple shutdown will do, because of client's daemon choosing logic)
                    }
                }

            }
        }
    }

    private fun shutdownNow() {
        log.info("Shutdown started")
        fun Long.mb() = this / (1024 * 1024)
        with(Runtime.getRuntime()) {
            log.info("Memory stats: total: ${totalMemory().mb()}mb, free: ${freeMemory().mb()}mb, max: ${maxMemory().mb()}mb")
        }
        state.alive.set(Aliveness.Dying.ordinal)
        shutdownServer()
        log.info("Shutdown complete")
        onShutdown()
        log.handlers.forEach { it.flush() }
    }

    private fun shutdownWithDelayImpl(currentClientsCount: Int, currentSessionId: Int, currentCompilationsCount: Int) {
        log.fine("${log.name} .......shutdowning........")
        log.fine("${log.name} currentCompilationsCount = $currentCompilationsCount, compilationsCounter.get(): ${compilationsCounter.get()}")
        state.delayedShutdownQueued.set(false)
        if (currentClientsCount == state.clientsCounter &&
            currentCompilationsCount == compilationsCounter.get() &&
            currentSessionId == state.sessions.lastSessionId
        ) {
            log.fine("currentCompilationsCount == compilationsCounter.get()")
            runBlocking(Dispatchers.Unconfined) {
                ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession) {
                    log.info("Execute delayed shutdown")
                    shutdownNow()
                }
            }
        } else {
            log.info("Cancel delayed shutdown due to a new activity")
        }
    }

    private fun shutdownWithDelay() {
        state.delayedShutdownQueued.set(true)
        val currentClientsCount = state.clientsCounter
        val currentSessionId = state.sessions.lastSessionId
        val currentCompilationsCount = compilationsCounter.get()
        log.info("Delayed shutdown in ${daemonOptions.shutdownDelayMilliseconds}ms")
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            shutdownWithDelayImpl(currentClientsCount, currentSessionId, currentCompilationsCount)
        }
    }

    private fun gracefulShutdown(onAnotherThread: Boolean): Boolean {

        if (!state.alive.compareAndSet(Aliveness.Alive.ordinal, Aliveness.LastSession.ordinal)) {
            log.info("Invalid state for graceful shutdown: ${state.alive.get().toAlivenessName()}")
            return false
        }
        log.info("Graceful shutdown signalled")

        if (!onAnotherThread) {
            shutdownIfIdle()
        } else {
            timer.schedule(1) {
                gracefulShutdownImpl()
            }

        }
        return true
    }

    private fun gracefulShutdownImpl() {
        runBlocking(Dispatchers.Unconfined) {
            ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession) {
                shutdownIfIdle()
            }
        }
    }

    private fun shutdownIfIdle() = when {
        state.sessions.isEmpty() -> shutdownWithDelay()
        else -> {
            daemonOptions.autoshutdownIdleSeconds =
                    TimeUnit.MILLISECONDS.toSeconds(daemonOptions.forceShutdownTimeoutMilliseconds).toInt()
            daemonOptions.autoshutdownUnusedSeconds = daemonOptions.autoshutdownIdleSeconds
            log.info("Some sessions are active, waiting for them to finish")
            log.info("Unused/idle timeouts are set to ${daemonOptions.autoshutdownUnusedSeconds}/${daemonOptions.autoshutdownIdleSeconds}s")
        }
    }

    private suspend fun createCompileServices(
        facade: CompilerCallbackServicesFacadeClientSide,
        eventManager: EventManager,
        rpcProfiler: Profiler
    ): Services {
        val builder = Services.Builder()
        if (facade.hasIncrementalCaches()) {
            builder.register(
                IncrementalCompilationComponents::class.java,
                RemoteIncrementalCompilationComponentsClient(facade, rpcProfiler)
            )
        }
        if (facade.hasLookupTracker()) {
            builder.register(LookupTracker::class.java, RemoteLookupTrackerClient(facade, eventManager, rpcProfiler))
        }
        if (facade.hasCompilationCanceledStatus()) {
            log.fine("facade.hasCompilationCanceledStatus() = true")
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        } else {
            log.fine("facade.hasCompilationCanceledStatus() = false")
        }
        return builder.build()
    }

    override suspend fun clearJarCache() {
        ZipHandler.clearFileAccessorCache()
        (KotlinCoreEnvironment.applicationEnvironment?.jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
    }

    private suspend fun <R> ifAlive(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val result = CompletableDeferred<Any>()
        scheduler.scheduleTask(OrdinaryTaskWithResult(result) {
            ifAliveChecksImplSuspend(minAliveness, body)
        })
        return result.await() as CompileService.CallResult<R>
    }

    private suspend fun ifAliveUnit(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: suspend () -> Unit
    ) {
        val completed = CompletableDeferred<Boolean>()
        scheduler.scheduleTask(
            OrdinaryTask(completed) {
                ifAliveChecksImplSuspend(minAliveness) {
                    body()
                    CompileService.CallResult.Ok()
                }
            }
        )
        completed.await()
    }

    private suspend fun <R> ifAliveExclusive(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val result = CompletableDeferred<Any>()
        scheduler.scheduleTask(ShutdownTaskWithResult(result) {
            ifAliveChecksImplSuspend(minAliveness, body)
        })
        return result.await() as CompileService.CallResult<R>
    }

    private suspend fun ifAliveExclusiveUnit(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: suspend () -> Unit
    ): CompileService.CallResult<Unit> {
        val result = CompletableDeferred<Any>()
        scheduler.scheduleTask(ShutdownTaskWithResult(result) {
            ifAliveChecksImplSuspend(minAliveness) {
                body()
                CompileService.CallResult.Ok()
            }
        })
        return result.await() as CompileService.CallResult<Unit>
    }

    private suspend fun <R> ifAliveChecksImplSuspend(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: suspend () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val curState = state.alive.get()
        return when {
            curState < minAliveness.ordinal -> {
                log.info("Cannot perform operation, requested state: ${minAliveness.name} > actual: ${curState.toAlivenessName()}")
                CompileService.CallResult.Dying()
            }
            else -> {
                try {
                    body()
                } catch (e: Throwable) {
                    log.log(Level.SEVERE, "Exception", e)
                    CompileService.CallResult.Error(e.message ?: "unknown")
                }
            }
        }
    }

}
