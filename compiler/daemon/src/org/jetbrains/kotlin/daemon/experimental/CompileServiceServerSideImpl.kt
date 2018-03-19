/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.KotlinJvmReplService
import org.jetbrains.kotlin.daemon.LazyClasspathWatcher
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Report
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.incremental.experimental.RemoteAnnotationsFileUpdaterAsync
import org.jetbrains.kotlin.daemon.incremental.experimental.RemoteArtifactChangesProviderAsync
import org.jetbrains.kotlin.daemon.incremental.experimental.RemoteChangesRegistryAsync
import org.jetbrains.kotlin.daemon.report.experimental.CompileServicesFacadeMessageCollector
import org.jetbrains.kotlin.daemon.report.experimental.DaemonMessageReporterAsync
import org.jetbrains.kotlin.daemon.report.experimental.RemoteICReporterAsync
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.rmi.RemoteException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.read
import kotlin.concurrent.schedule
import kotlin.concurrent.write

fun nowSeconds() = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())


interface EventManager {
    fun onCompilationFinished(f: () -> Unit)
}

private class EventManagerImpl : EventManager {
    private val onCompilationFinished = arrayListOf<() -> Unit>()

    @Throws(RemoteException::class)
    override fun onCompilationFinished(f: () -> Unit) {
        onCompilationFinished.add(f)
    }

    fun fireCompilationFinished() {
        onCompilationFinished.forEach { it() }
    }
}

typealias AnyMessage = Server.AnyMessage<CompileServiceServerSide>
typealias Message = Server.Message<CompileServiceServerSide>
typealias EndConnectionMessage = Server.EndConnectionMessage<CompileServiceServerSide>

class CompileServiceServerSideImpl(
    override val serverPort: Int,
    val compiler: CompilerSelector,
    val compilerId: CompilerId,
    val daemonOptions: DaemonOptions,
    val daemonJVMOptions: DaemonJVMOptions,
    val port: Int,
    val timer: Timer,
    val onShutdown: () -> Unit
) : CompileServiceServerSide {

    constructor(
        serverPort: Int,
        compilerId: CompilerId,
        daemonOptions: DaemonOptions,
        daemonJVMOptions: DaemonJVMOptions,
        port: Int,
        timer: Timer,
        onShutdown: () -> Unit
    ) : this(
        serverPort,
        CompilerSelector.getDefault(),
        compilerId,
        daemonOptions,
        daemonJVMOptions,
        port,
        timer,
        onShutdown
    )

    private val log by lazy { Logger.getLogger("compiler") }

    init {

        log.info("init(port= $serverPort)")

        // assuming logically synchronized
        System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        // TODO UNCOMMENT THIS : this.toRMIServer(daemonOptions, compilerId) // also create RMI server in order to support old clients

        timer.schedule(10) {
            exceptionLoggingTimerThread { initiateElections() }
        }
        timer.schedule(delay = DAEMON_PERIODIC_CHECK_INTERVAL_MS, period = DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread { periodicAndAfterSessionCheck() }
        }
        timer.schedule(delay = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS + 100, period = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread { periodicSeldomCheck() }
        }

    }

    // wrapped in a class to encapsulate alive check logic
    private class ClientOrSessionProxy<out T : Any>(
        val aliveFlagPath: String?,
        val data: T? = null,
        private var disposable: Disposable? = null
    ) {
        val isAlive: Boolean
            get() = aliveFlagPath?.let { File(it).exists() } ?: true // assuming that if no file was given, the client is alive

        fun dispose() {
            disposable?.let {
                Disposer.dispose(it)
                disposable = null
            }
        }
    }

    private val compilationsCounter = AtomicInteger(0)

    private val classpathWatcher = LazyClasspathWatcher(compilerId.compilerClasspath)

    enum class Aliveness {
        // !!! ordering of values is used in state comparison
        Dying,
        LastSession, Alive
    }

    private class SessionsContainer {

        private val lock = ReentrantReadWriteLock()
        private val sessions: MutableMap<Int, ClientOrSessionProxy<Any>> = hashMapOf()
        private val sessionsIdCounter = AtomicInteger(0)

        fun <T : Any> leaseSession(session: ClientOrSessionProxy<T>): Int = lock.write {
            val newId = getValidId(sessionsIdCounter) {
                it != CompileService.NO_SESSION && !sessions.containsKey(it)
            }
            sessions.put(newId, session)
            newId
        }

        fun isEmpty(): Boolean = lock.read { sessions.isEmpty() }

        operator fun get(sessionId: Int) = lock.read { sessions[sessionId] }

        fun remove(sessionId: Int): Boolean = lock.write {
            sessions.remove(sessionId)?.apply { dispose() } != null
        }

        fun cleanDead(): Boolean {
            var anyDead = false
            lock.read {
                val toRemove = sessions.filterValues { !it.isAlive }
                if (toRemove.isNotEmpty()) {
                    anyDead = true
                    lock.write {
                        toRemove.forEach { sessions.remove(it.key)?.dispose() }
                    }
                }
            }
            return anyDead
        }
    }

    // TODO: encapsulate operations on state here
    private val state = object {

        private val clientsLock = ReentrantReadWriteLock()
        private val clientProxies: MutableSet<ClientOrSessionProxy<Any>> = hashSetOf()

        val sessions = SessionsContainer()

        val delayedShutdownQueued = AtomicBoolean(false)

        var alive = AtomicInteger(Aliveness.Alive.ordinal)

        val aliveClientsCount: Int get() = clientProxies.size

        fun addClient(aliveFlagPath: String?) {
            clientsLock.write {
                clientProxies.add(ClientOrSessionProxy(aliveFlagPath))
            }
        }

        fun getClientsFlagPaths(): List<String> = clientsLock.read {
            clientProxies.mapNotNull { it.aliveFlagPath }
        }

        fun cleanDeadClients(): Boolean =
            clientProxies.cleanMatching(clientsLock, { !it.isAlive }, { if (clientProxies.remove(it)) it.dispose() })
    }

    private fun Int.toAlivenessName(): String =
        try {
            Aliveness.values()[this].name
        } catch (_: Throwable) {
            "invalid($this)"
        }

    private inline fun <T> Iterable<T>.cleanMatching(
        lock: ReentrantReadWriteLock,
        crossinline pred: (T) -> Boolean,
        crossinline clean: (T) -> Unit
    ): Boolean {
        var anyDead = false
        lock.read {
            val toRemove = filter(pred)
            if (toRemove.isNotEmpty()) {
                anyDead = true
                lock.write {
                    toRemove.forEach(clean)
                }
            }
        }
        return anyDead
    }

    @Volatile
    private var _lastUsedSeconds = nowSeconds()
    val lastUsedSeconds: Long get() = if (rwlock.isWriteLocked || rwlock.readLockCount - rwlock.readHoldCount > 0) nowSeconds() else _lastUsedSeconds

    private val rwlock = ReentrantReadWriteLock()

    private var runFile: File

    init {
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        log.info("port.toString() = $port | serverPort = $serverPort")
        runFile = File(
            runFileDir,
            makeRunFilenameString(
                timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                port = port.toString()
            )
        )
        try {
            if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
        } catch (e: Throwable) {
            throw IllegalStateException("Unable to create runServer file '${runFile.absolutePath}'", e)
        }
        runFile.deleteOnExit()
        log.info("last_init_end")
    }

    // RMI-exposed API

    override suspend fun getDaemonInfo(): CompileService.CallResult<String> = ifAlive(minAliveness = Aliveness.Dying) {
        CompileService.CallResult.Good("Kotlin daemon on socketPort $port")
    }

    override suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = ifAlive {
        CompileService.CallResult.Good(daemonOptions)
    }

    override suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = ifAlive {
        log.info("getDaemonJVMOptions: $daemonJVMOptions")// + daemonJVMOptions.mappers.flatMap { it.toArgs("-") })

        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> =
        ifAlive(minAliveness = Aliveness.Alive) {
            state.addClient(aliveFlagPath)
            log.info("Registered a client alive file: $aliveFlagPath")
            CompileService.CallResult.Ok()
        }

    override suspend fun getClients(): CompileService.CallResult<List<String>> = ifAlive {
        CompileService.CallResult.Good(state.getClientsFlagPaths())
    }

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
        rwlock.write {
            clearJarCache()
        }
        if (state.sessions.isEmpty()) {
            // TODO: and some goes here
        }
        timer.schedule(0) {
            periodicAndAfterSessionCheck()
        }
        CompileService.CallResult.Ok()
    }

    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean =
        (compilerId.compilerVersion.isEmpty() || compilerId.compilerVersion == expectedCompilerId.compilerVersion) &&
                (compilerId.compilerClasspath.all { expectedCompilerId.compilerClasspath.contains(it) }) &&
                !classpathWatcher.isChanged

    override suspend fun getUsedMemory(): CompileService.CallResult<Long> =
        ifAlive { CompileService.CallResult.Good(usedMemory(withGC = true)) }

    override suspend fun shutdown(): CompileService.CallResult<Nothing> = ifAliveExclusive(minAliveness = Aliveness.LastSession) {
        shutdownWithDelay()
        CompileService.CallResult.Ok()
    }

    override suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> =
        ifAlive(minAliveness = Aliveness.LastSession) {
            val res = when {
                graceful -> gracefulShutdown(true)
                else -> {
                    shutdownWithDelay()
                    true
                }
            }
            CompileService.CallResult.Good(res)
        }

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseClientSide,
        compilationResults: CompilationResultsClientSide?
    ): CompileService.CallResult<Int> = ifAlive {
        servicesFacade.connectToServer()
        compilationResults?.connectToServer()
        val messageCollector = CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
        val daemonReporter = DaemonMessageReporterAsync(servicesFacade, compilationOptions)
        val targetPlatform = compilationOptions.targetPlatform
        log.info("Starting compilation with args: " + compilerArguments.joinToString(" "))

        @Suppress("UNCHECKED_CAST")
        val compiler = when (targetPlatform) {
            CompileService.TargetPlatform.JVM -> K2JVMCompiler()
            CompileService.TargetPlatform.JS -> K2JSCompiler()
            CompileService.TargetPlatform.METADATA -> K2MetadataCompiler()
        } as CLICompiler<CommonCompilerArguments>

        val k2PlatformArgs = compiler.createArguments()
        parseCommandLineArguments(compilerArguments.asList(), k2PlatformArgs)
        val argumentParseError = validateArguments(k2PlatformArgs.errors)
        if (argumentParseError != null) {
            messageCollector.report(CompilerMessageSeverity.ERROR, argumentParseError)
            CompileService.CallResult.Good(ExitCode.COMPILATION_ERROR.code)
        } else when (compilationOptions.compilerMode) {
            CompilerMode.JPS_COMPILER -> {
                val jpsServicesFacade = servicesFacade as CompilerCallbackServicesFacadeClientSide

                withIC(enabled = servicesFacade.hasIncrementalCaches()) {
                    doCompile(sessionId, daemonReporter, tracer = null) { eventManger, profiler ->
                        val services = createCompileServices(jpsServicesFacade, eventManger, profiler)
                        compiler.exec(messageCollector, services, k2PlatformArgs)
                    }
                }
            }
            CompilerMode.NON_INCREMENTAL_COMPILER -> {
                doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                    compiler.exec(messageCollector, Services.EMPTY, k2PlatformArgs)
                }
            }
            CompilerMode.INCREMENTAL_COMPILER -> {
                val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
                val gradleIncrementalServicesFacade = servicesFacade as IncrementalCompilerServicesFacadeAsync

                when (targetPlatform) {
                    CompileService.TargetPlatform.JVM -> {
                        val k2jvmArgs = k2PlatformArgs as K2JVMCompilerArguments
                        withIC {
                            doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                                runBlocking(Unconfined) {
                                    execIncrementalCompiler(
                                        k2jvmArgs, gradleIncrementalArgs, gradleIncrementalServicesFacade, compilationResults!!,
                                        messageCollector, daemonReporter
                                    )
                                }
                            }
                        }
                    }
                    CompileService.TargetPlatform.JS -> {
                        val k2jsArgs = k2PlatformArgs as K2JSCompilerArguments

                        withJsIC {
                            doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                                execJsIncrementalCompiler(
                                    k2jsArgs,
                                    gradleIncrementalArgs,
                                    gradleIncrementalServicesFacade,
                                    compilationResults!!,
                                    messageCollector
                                )
                            }
                        }
                    }
                    else -> throw IllegalStateException("Incremental compilation is not supported for target platform: $targetPlatform")

                }
            }
            else -> throw IllegalStateException("Unknown compilation mode ${compilationOptions.compilerMode}")
        }
    }

    private fun execJsIncrementalCompiler(
        args: K2JSCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        servicesFacade: IncrementalCompilerServicesFacadeAsync,
        compilationResults: CompilationResultsClientSide,
        compilerMessageCollector: MessageCollector
    ): ExitCode {
        val allKotlinFiles = arrayListOf<File>()
        val freeArgsWithoutKotlinFiles = arrayListOf<String>()
        args.freeArgs.forEach {
            if (it.endsWith(".kt") && File(it).exists()) {
                allKotlinFiles.add(File(it))
            } else {
                freeArgsWithoutKotlinFiles.add(it)
            }
        }
        args.freeArgs = freeArgsWithoutKotlinFiles

        val reporter = RemoteICReporterAsync(servicesFacade, compilationResults, incrementalCompilationOptions)

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        } else {
            ChangedFiles.Unknown()
        }

        val workingDir = incrementalCompilationOptions.workingDir
        val versions = commonCacheVersions(workingDir) +
                customCacheVersion(
                    incrementalCompilationOptions.customCacheVersion,
                    incrementalCompilationOptions.customCacheVersionFileName,
                    workingDir,
                    enabled = true
                )

        val compiler = IncrementalJsCompilerRunner(workingDir, versions, reporter)
        return compiler.compile(allKotlinFiles, args, compilerMessageCollector, changedFiles)
    }

    private suspend fun execIncrementalCompiler(
        k2jvmArgs: K2JVMCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        servicesFacade: IncrementalCompilerServicesFacadeAsync,
        compilationResults: CompilationResultsClientSide,
        compilerMessageCollector: MessageCollector,
        daemonMessageReporterAsync: DaemonMessageReporterAsync
    ): ExitCode {
        val reporter = RemoteICReporterAsync(servicesFacade, compilationResults, incrementalCompilationOptions)
        val annotationFileUpdater =
            if (servicesFacade.hasAnnotationsFileUpdater())
                RemoteAnnotationsFileUpdaterAsync(servicesFacade)
            else
                null

        val moduleFile = k2jvmArgs.buildFile?.let(::File)
        assert(moduleFile?.exists() ?: false) { "Module does not exist ${k2jvmArgs.buildFile}" }

        // todo: pass javaSourceRoots and allKotlinFiles using IncrementalCompilationOptions
        val parsedModule = run {
            val bytesOut = ByteArrayOutputStream()
            val printStream = PrintStream(bytesOut)
            val mc = PrintingMessageCollector(printStream, MessageRenderer.PLAIN_FULL_PATHS, false)
            val parsedModule = ModuleXmlParser.parseModuleScript(k2jvmArgs.buildFile!!, mc)
            if (mc.hasErrors()) {
                daemonMessageReporterAsync.report(ReportSeverity.ERROR, bytesOut.toString("UTF8"))
            }
            parsedModule
        }

        val javaSourceRoots = parsedModule.modules.flatMapTo(HashSet()) {
            it.getJavaSourceRoots().map { JvmSourceRoot(File(it.path), it.packagePrefix) }
        }

        val allKotlinFiles = parsedModule.modules.flatMap { it.getSourceFiles().map(::File) }
        k2jvmArgs.friendPaths = parsedModule.modules.flatMap(Module::getFriendPaths).toTypedArray()

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        } else {
            ChangedFiles.Unknown()
        }

        val artifactChanges = RemoteArtifactChangesProviderAsync(servicesFacade)
        val changesRegistry = RemoteChangesRegistryAsync(servicesFacade)

        val workingDir = incrementalCompilationOptions.workingDir
        val versions = commonCacheVersions(workingDir) +
                customCacheVersion(
                    incrementalCompilationOptions.customCacheVersion,
                    incrementalCompilationOptions.customCacheVersionFileName,
                    workingDir,
                    enabled = true
                )

        val compiler = IncrementalJvmCompilerRunner(
            workingDir, javaSourceRoots, versions,
            reporter, annotationFileUpdater,
            artifactChanges, changesRegistry,
            buildHistoryFile = incrementalCompilationOptions.resultDifferenceFile,
            friendBuildHistoryFile = incrementalCompilationOptions.friendDifferenceFile,
            usePreciseJavaTracking = incrementalCompilationOptions.usePreciseJavaTracking
        )
        return compiler.compile(allKotlinFiles, k2jvmArgs, compilerMessageCollector, changedFiles)
    }

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseClientSide,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        if (compilationOptions.targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val messageCollector =
                CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
            val repl = KotlinJvmReplService(
                disposable, port, templateClasspath, templateClassName,
                messageCollector, null
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
                CompileService.CallResult.Good(createRemoteState(port).clientSide)
            }
        }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                withValidReplState(replStateId) { state ->
                    runBlocking(Unconfined) {
                        check(state, codeLine)
                    }
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

    // -----------------------------------------------------------------------
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

    private inline fun exceptionLoggingTimerThread(body: () -> Unit) {
        try {
            body()
        } catch (e: Throwable) {
            System.err.println("Exception in timer thread: " + e.message)
            e.printStackTrace(System.err)
            log.log(Level.SEVERE, "Exception in timer thread", e)
        }
    }

    private fun periodicAndAfterSessionCheck() {

        if (state.delayedShutdownQueued.get()) return

        val anyDead = state.sessions.cleanDead() || state.cleanDeadClients()

        ifAliveUnit(minAliveness = Aliveness.LastSession) {
            when {
            // check if in graceful shutdown state and all sessions are closed
                state.alive.get() == Aliveness.LastSession.ordinal && state.sessions.isEmpty() -> {
                    log.info("All sessions finished")
                    shutdownWithDelay()
                    return
                }
                state.aliveClientsCount == 0 -> {
                    log.info("No more clients left")
                    shutdownWithDelay()
                    return
                }
            // discovery file removed - shutdown
                !runFile.exists() -> {
                    log.info("Run file removed")
                    shutdownWithDelay()
                    return
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
                    runBlocking(Unconfined) {
                        clearJarCache()
                    }
                }
            }
        }
    }

    private fun periodicSeldomCheck() {
        ifAliveUnit(minAliveness = Aliveness.Alive) {

            // compiler changed (seldom check) - shutdown
            if (classpathWatcher.isChanged) {
                log.info("Compiler changed.")
                gracefulShutdown(false)
            }
        }
    }


    // TODO: handover should include mechanism for client to switch to a new daemon then previous "handed over responsibilities" and shot down
    private fun initiateElections() {
        ifAliveUnit {

            log.info("initiate elections")
            runBlocking(Unconfined) {
                val aliveWithOpts = walkDaemonsAsync(
                    File(daemonOptions.runFilesPathOrDefault),
                    compilerId,
                    runFile,
                    filter = { _, p -> p != port },
                    report = { _, msg -> log.info(msg) }, useRMI = false
                )
                val comparator = compareByDescending<DaemonWithMetadataAsync, DaemonJVMOptions>(
                    DaemonJVMOptionsMemoryComparator(),
                    { it.jvmOptions }
                ).thenBy(FileAgeComparator()) { it.runFile }
                aliveWithOpts.maxWith(comparator)?.let { bestDaemonWithMetadata ->
                    val fattestOpts = bestDaemonWithMetadata.jvmOptions
                    if (fattestOpts memorywiseFitsInto daemonJVMOptions && FileAgeComparator().compare(
                            bestDaemonWithMetadata.runFile,
                            runFile
                        ) < 0
                    ) {
                        runBlocking(Unconfined) {
                            // all others are smaller that me, take overs' clients and shut them down
                            log.info("${LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE} lower prio, taking clients from them and schedule them to shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                            aliveWithOpts.map { (daemon, runFile, _) ->
                                async {
                                    try {
                                        daemon.getClients().takeIf { it.isGood }?.let {
                                            it.get().map { clientAliveFile ->
                                                async {
                                                    registerClient(clientAliveFile)
                                                }
                                            }.forEach { it.await() }
                                        }
                                        daemon.scheduleShutdown(true)
                                    } catch (e: Throwable) {
                                        log.info("Cannot connect to a daemon, assuming dying ('${runFile.canonicalPath}'): ${e.message}")
                                    }
                                }
                            }.forEach { it.await() }
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
                        runBlocking(Unconfined) {
                            // there is at least one bigger, handover my clients to it and shutdown
                            log.info("${LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE} higher prio, handover clients to it and schedule shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                            getClients().takeIf { it.isGood }?.let {
                                it.get().forEach { bestDaemonWithMetadata.daemon.registerClient(it) }
                            }
                            scheduleShutdown(true)
                        }
                    } else {
                        // undecided, do nothing
                        log.info("${LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE} equal prio, continue: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
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

        // TODO : unexportSelf(true) - what is the purpose?
        log.info("Shutdown complete")
        onShutdown()
        log.handlers.forEach { it.flush() }
    }

    private fun shutdownWithDelay() {
        state.delayedShutdownQueued.set(true)
        val currentCompilationsCount = compilationsCounter.get()
        log.info("Delayed shutdown in ${daemonOptions.shutdownDelayMilliseconds}ms")
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            state.delayedShutdownQueued.set(false)
            if (currentCompilationsCount == compilationsCounter.get()) {
                ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession) {
                    log.fine("Execute delayed shutdown")
                    shutdownNow()
                }
            } else {
                log.info("Cancel delayed shutdown due to new client")
            }
        }
    }

    private fun gracefulShutdown(onAnotherThread: Boolean): Boolean {

        fun shutdownIfIdle() = when {
            state.sessions.isEmpty() -> shutdownWithDelay()
            else -> {
                daemonOptions.autoshutdownIdleSeconds =
                        TimeUnit.MILLISECONDS.toSeconds(daemonOptions.forceShutdownTimeoutMilliseconds).toInt()
                daemonOptions.autoshutdownUnusedSeconds = daemonOptions.autoshutdownIdleSeconds
                log.info("Some sessions are active, waiting for them to finish")
                log.info("Unused/idle timeouts are set to ${daemonOptions.autoshutdownUnusedSeconds}/${daemonOptions.autoshutdownIdleSeconds}s")
            }
        }

        if (!state.alive.compareAndSet(Aliveness.Alive.ordinal, Aliveness.LastSession.ordinal)) {
            log.info("Invalid state for graceful shutdown: ${state.alive.get().toAlivenessName()}")
            return false
        }
        log.info("Graceful shutdown signalled")

        if (!onAnotherThread) {
            shutdownIfIdle()
        } else {
            timer.schedule(1) {
                ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession) {
                    shutdownIfIdle()
                }
            }
        }
        return true
    }

    private fun doCompile(
        sessionId: Int,
        daemonMessageReporterAsync: DaemonMessageReporterAsync,
        tracer: RemoteOperationsTracer?,
        body: (EventManager, Profiler) -> ExitCode
    ): CompileService.CallResult<Int> =
        ifAlive {
            withValidClientOrSessionProxy(sessionId) {
                tracer?.before("compile")
                val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
                val eventManger = EventManagerImpl()
                try {
                    val exitCode = checkedCompile(daemonMessageReporterAsync, rpcProfiler) {
                        body(eventManger, rpcProfiler).code
                    }
                    CompileService.CallResult.Good(exitCode)
                } finally {
                    eventManger.fireCompilationFinished()
                    tracer?.after("compile")
                }
            }
        }

    private fun createCompileServices(
        facade: CompilerCallbackServicesFacadeClientSide,
        eventManager: EventManager,
        rpcProfiler: Profiler
    ): Services = runBlocking(Unconfined) {
        val builder = Services.Builder()
        if (facade.hasIncrementalCaches()) {
            builder.register(
                IncrementalCompilationComponents::class.java,
                RemoteIncrementalCompilationComponentsClient(facade, eventManager, rpcProfiler)
            )
        }
        if (facade.hasLookupTracker()) {
            builder.register(LookupTracker::class.java, RemoteLookupTrackerClient(facade, eventManager, rpcProfiler))
        }
        if (facade.hasCompilationCanceledStatus()) {
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        }
        builder.build()
    }


    private fun <R> checkedCompile(daemonMessageReporterAsync: DaemonMessageReporterAsync, rpcProfiler: Profiler, body: () -> R): R {
        try {
            val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            val res = profiler.withMeasure(null, body)

            val endMem = if (daemonOptions.reportPerf) usedMemory(withGC = false) else 0L

            log.info("Done with result " + res.toString())

            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                fun Long.kb() = this / 1024
                val pc = profiler.getTotalCounters()
                val rpc = rpcProfiler.getTotalCounters()

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; rpc: ${rpc.count} calls, ${rpc.time.ms()} ms, thread ${rpc.threadTime.ms()} ms; memory: ${endMem.kb()} kb (${"%+d".format(
                    pc.memory.kb()
                )} kb)".let {
                    daemonMessageReporterAsync.report(ReportSeverity.INFO, it)
                    log.info(it)
                }

                // this will only be reported if if appropriate (e.g. ByClass) profiler is used
                for ((obj, counters) in rpcProfiler.getCounters()) {
                    "PERF: rpc by $obj: ${counters.count} calls, ${counters.time.ms()} ms, thread ${counters.threadTime.ms()} ms".let {
                        daemonMessageReporterAsync.report(ReportSeverity.INFO, it)
                        log.info(it)
                    }
                }
            }
            return res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Throwable) {
            log.info("Error: $e")
            throw e
        }
    }

    override suspend fun clearJarCache() {
        ZipHandler.clearFileAccessorCache()
        (KotlinCoreEnvironment.applicationEnvironment?.jarFileSystem as? CoreJarFileSystem)?.clearHandlersCache()
    }

    private inline fun <R> ifAlive(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> = rwlock.read {
        ifAliveChecksImpl(minAliveness, body)
    }

    private inline fun ifAliveUnit(minAliveness: Aliveness = Aliveness.LastSession, body: () -> Unit): Unit = rwlock.read {
        ifAliveChecksImpl(minAliveness) {
            body()
            CompileService.CallResult.Ok()
        }
    }

    private inline fun <R> ifAliveExclusive(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: () -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> = rwlock.write {
        ifAliveChecksImpl(minAliveness, body)
    }

    private inline fun ifAliveExclusiveUnit(minAliveness: Aliveness = Aliveness.LastSession, body: () -> Unit): Unit = rwlock.write {
        ifAliveChecksImpl(minAliveness) {
            body()
            CompileService.CallResult.Ok()
        }
    }

    private inline fun <R> ifAliveChecksImpl(
        minAliveness: Aliveness = Aliveness.LastSession,
        body: () -> CompileService.CallResult<R>
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

    private inline fun <R> withValidClientOrSessionProxy(
        sessionId: Int,
        body: (ClientOrSessionProxy<Any>?) -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> {
        val session: ClientOrSessionProxy<Any>? =
            if (sessionId == CompileService.NO_SESSION) null
            else state.sessions[sessionId] ?: return CompileService.CallResult.Error("Unknown or invalid session $sessionId")
        try {
            compilationsCounter.incrementAndGet()
            return body(session)
        } finally {
            _lastUsedSeconds = nowSeconds()
        }
    }

    private inline fun <R> withValidRepl(sessionId: Int, body: KotlinJvmReplServiceAsync.() -> R): CompileService.CallResult<R> =
        withValidClientOrSessionProxy(sessionId) { session ->
            (session?.data as? KotlinJvmReplServiceAsync?)?.let {
                CompileService.CallResult.Good(it.body())
            } ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
        }

    @JvmName("withValidRepl1")
    private inline fun <R> withValidRepl(
        sessionId: Int,
        body: KotlinJvmReplServiceAsync.() -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> =
        withValidClientOrSessionProxy(sessionId) { session ->
            (session?.data as? KotlinJvmReplServiceAsync?)?.body() ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
        }

}