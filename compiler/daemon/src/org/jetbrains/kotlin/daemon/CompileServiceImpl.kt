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
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.RemoteBuildReporter
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarHandler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.report.CompileServicesFacadeMessageCollector
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporterPrintStreamAdapter
import org.jetbrains.kotlin.daemon.report.getBuildReporter
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryAndroid
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJvm
import org.jetbrains.kotlin.incremental.parsing.classesFqNames
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
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
import kotlin.concurrent.read
import kotlin.concurrent.schedule
import kotlin.concurrent.write

const val REMOTE_STREAM_BUFFER_SIZE = 4096

fun nowSeconds() = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime())

interface CompilerSelector {
    operator fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*>
}

interface EventManager {
    fun onCompilationFinished(f: () -> Unit)
}

class EventManagerImpl : EventManager {
    val onCompilationFinished = arrayListOf<() -> Unit>()

    override fun onCompilationFinished(f: () -> Unit) {
        onCompilationFinished.add(f)
    }

    fun fireCompilationFinished() {
        onCompilationFinished.forEach { it() }
    }
}

abstract class CompileServiceImplBase(
    val daemonOptions: DaemonOptions,
    val compilerId: CompilerId,
    val port: Int,
    val timer: Timer
) {
    protected val log by lazy { Logger.getLogger("compiler") }

    init {
        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
    }

    // wrapped in a class to encapsulate alive check logic
    protected class ClientOrSessionProxy<out T : Any>(
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

    protected val compilationsCounter = AtomicInteger(0)

    protected val classpathWatcher = LazyClasspathWatcher(compilerId.compilerClasspath)

    enum class Aliveness {
        // !!! ordering of values is used in state comparison
        Dying,
        LastSession, Alive
    }

    protected class SessionsContainer {

        private val lock = ReentrantReadWriteLock()
        private val sessions: MutableMap<Int, ClientOrSessionProxy<Any>> = hashMapOf()
        private val sessionsIdCounter = AtomicInteger(0)

        val lastSessionId get() = sessionsIdCounter.get()

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
    protected class CompileServiceState {
        private val clientsLock = ReentrantReadWriteLock()
        private val clientProxies: MutableSet<ClientOrSessionProxy<Any>> = hashSetOf()

        val sessions = SessionsContainer()

        val delayedShutdownQueued = AtomicBoolean(false)

        var alive = AtomicInteger(Aliveness.Alive.ordinal)

        val aliveClientsCount: Int get() = clientProxies.size

        private val _clientsCounter = AtomicInteger(0)

        val clientsCounter get() = _clientsCounter.get()

        fun addClient(aliveFlagPath: String?) {
            clientsLock.write {
                _clientsCounter.incrementAndGet()
                clientProxies.add(ClientOrSessionProxy(aliveFlagPath))
            }
        }

        fun getClientsFlagPaths(): List<String> = clientsLock.read {
            clientProxies.mapNotNull { it.aliveFlagPath }
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

        fun cleanDeadClients(): Boolean =
            clientProxies.cleanMatching(clientsLock, { !it.isAlive }, { if (clientProxies.remove(it)) it.dispose() })
    }

    protected val state = CompileServiceState()

    protected fun Int.toAlivenessName(): String =
        try {
            Aliveness.values()[this].name
        } catch (_: Throwable) {
            "invalid($this)"
        }

    @Volatile
    protected var _lastUsedSeconds = nowSeconds()
    abstract protected val lastUsedSeconds: Long

    protected var runFile: File

    init {
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        runFile = File(
            runFileDir,
            makeRunFilenameString(
                timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                digest = compilerId.digest(),
                port = port.toString()
            )
        )
        try {
            if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
        } catch (e: Throwable) {
            throw IllegalStateException("Unable to create run file '${runFile.absolutePath}'", e)
        }
        runFile.deleteOnExit()
    }

    protected fun postReleaseCompileSession(): CompileService.CallResult<Nothing> {
        if (state.sessions.isEmpty()) {
            // TODO: and some goes here
        }
        timer.schedule(0) {
            periodicAndAfterSessionCheck()
        }
        return CompileService.CallResult.Ok()
    }

    protected abstract fun periodicAndAfterSessionCheck()
    protected abstract fun periodicSeldomCheck()
    protected abstract fun initiateElections()

    protected inline fun exceptionLoggingTimerThread(body: () -> Unit) {
        try {
            body()
        } catch (e: Throwable) {
            System.err.println("Exception in timer thread: " + e.message)
            e.printStackTrace(System.err)
            log.log(Level.SEVERE, "Exception in timer thread", e)
        }
    }

    protected inline fun <ServicesFacadeT, JpsServicesFacadeT, CompilationResultsT> compileImpl(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: ServicesFacadeT,
        compilationResults: CompilationResultsT,
        hasIncrementalCaches: JpsServicesFacadeT.() -> Boolean,
        createMessageCollector: (ServicesFacadeT, CompilationOptions) -> MessageCollector,
        createReporter: (ServicesFacadeT, CompilationOptions) -> DaemonMessageReporter,
        createServices: (JpsServicesFacadeT, EventManager, Profiler) -> Services,
        getICReporter: (ServicesFacadeT, CompilationResultsT?, IncrementalCompilationOptions) -> RemoteBuildReporter
    ) = kotlin.run {
        val messageCollector = createMessageCollector(servicesFacade, compilationOptions)
        val daemonReporter = createReporter(servicesFacade, compilationOptions)
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
                @Suppress("UNCHECKED_CAST")
                servicesFacade as JpsServicesFacadeT
                withIC(k2PlatformArgs, enabled = servicesFacade.hasIncrementalCaches()) {
                    doCompile(sessionId, daemonReporter, tracer = null) { eventManger, profiler ->
                        val services = createServices(servicesFacade, eventManger, profiler)
                        compiler.exec(messageCollector, services, k2PlatformArgs)
                    }
                }
            }
            CompilerMode.NON_INCREMENTAL_COMPILER -> {
                doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                    val exitCode = compiler.exec(messageCollector, Services.EMPTY, k2PlatformArgs)

                    val perfString = compiler.defaultPerformanceManager.renderCompilerPerformance()
                    compilationResults?.also {
                        (it as CompilationResults).add(
                            CompilationResultCategory.BUILD_REPORT_LINES.code,
                            arrayListOf(perfString)
                        )
                    }

                    exitCode
                }
            }
            CompilerMode.INCREMENTAL_COMPILER -> {
                val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
                val gradleIncrementalServicesFacade = servicesFacade

                when (targetPlatform) {
                    CompileService.TargetPlatform.JVM -> withIC(k2PlatformArgs) {
                        doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                            execIncrementalCompiler(
                                k2PlatformArgs as K2JVMCompilerArguments,
                                gradleIncrementalArgs,
                                messageCollector,
                                getICReporter(
                                    gradleIncrementalServicesFacade,
                                    compilationResults!!,
                                    gradleIncrementalArgs
                                )
                            )
                        }
                    }
                    CompileService.TargetPlatform.JS -> withJsIC(k2PlatformArgs) {
                        doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                            execJsIncrementalCompiler(
                                k2PlatformArgs as K2JSCompilerArguments,
                                gradleIncrementalArgs,
                                messageCollector,
                                getICReporter(
                                    gradleIncrementalServicesFacade,
                                    compilationResults!!,
                                    gradleIncrementalArgs
                                )
                            )
                        }
                    }
                    else -> throw IllegalStateException("Incremental compilation is not supported for target platform: $targetPlatform")

                }
            }
            else -> throw IllegalStateException("Unknown compilation mode ${compilationOptions.compilerMode}")
        }
    }


    protected inline fun doCompile(
        sessionId: Int,
        daemonMessageReporter: DaemonMessageReporter,
        tracer: RemoteOperationsTracer?,
        body: (EventManager, Profiler) -> ExitCode
    ): CompileService.CallResult<Int> = run {
        log.fine("alive!")
        withValidClientOrSessionProxy(sessionId) {
            tracer?.before("compile")
            val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
            val eventManager = EventManagerImpl()
            try {
                log.fine("trying get exitCode")
                val exitCode = checkedCompile(daemonMessageReporter, rpcProfiler) {
                    body(eventManager, rpcProfiler).code
                }
                CompileService.CallResult.Good(exitCode)
            } finally {
                eventManager.fireCompilationFinished()
                tracer?.after("compile")
            }
        }
    }

    fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
    fun Long.kb() = this / 1024

    protected inline fun <R> checkedCompile(
        daemonMessageReporter: DaemonMessageReporter,
        rpcProfiler: Profiler,
        body: () -> R
    ): R {
        try {
            val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

            val res = profiler.withMeasure(null, body)

            val endMem = if (daemonOptions.reportPerf) usedMemory(withGC = false) else 0L

            log.info("Done with result $res")

            if (daemonOptions.reportPerf) {
                val pc = profiler.getTotalCounters()
                val rpc = rpcProfiler.getTotalCounters()

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; rpc: ${rpc.count} calls, ${rpc.time.ms()} ms, thread ${rpc.threadTime.ms()} ms; memory: ${endMem.kb()} kb (${"%+d".format(
                    pc.memory.kb()
                )} kb)".let {
                    daemonMessageReporter.report(ReportSeverity.INFO, it)
                    log.info(it)
                }

                // this will only be reported if if appropriate (e.g. ByClass) profiler is used
                for ((obj, counters) in rpcProfiler.getCounters()) {
                    "PERF: rpc by $obj: ${counters.count} calls, ${counters.time.ms()} ms, thread ${counters.threadTime.ms()} ms".let {
                        daemonMessageReporter.report(ReportSeverity.INFO, it)
                        log.info(it)
                    }
                }
            }
            return res
        }
        // TODO: consider possibilities to handle OutOfMemory
        catch (e: Throwable) {
            log.log(
                Level.SEVERE,
                "Exception: $e\n  ${e.stackTrace.joinToString("\n  ")}${
                if (e.cause != null && e.cause != e) {
                    "\nCaused by: ${e.cause}\n  ${e.cause!!.stackTrace.joinToString("\n  ")}"
                } else ""
                }"
            )
            throw e
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

    fun startDaemonElections() {
        timer.schedule(10) {
            exceptionLoggingTimerThread { initiateElections() }
        }
    }

    fun configurePeriodicActivities() {
        timer.schedule(delay = DAEMON_PERIODIC_CHECK_INTERVAL_MS, period = DAEMON_PERIODIC_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread { periodicAndAfterSessionCheck() }
        }
        timer.schedule(delay = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS + 100, period = DAEMON_PERIODIC_SELDOM_CHECK_INTERVAL_MS) {
            exceptionLoggingTimerThread { periodicSeldomCheck() }
        }
    }


    protected inline fun <R> ifAliveChecksImpl(
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

    protected inline fun <R> withValidClientOrSessionProxy(
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

    protected fun execJsIncrementalCompiler(
        args: K2JSCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        compilerMessageCollector: MessageCollector,
        reporter: RemoteBuildReporter
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

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        } else {
            ChangedFiles.Unknown()
        }

        val workingDir = incrementalCompilationOptions.workingDir
        val modulesApiHistory = ModulesApiHistoryJs(incrementalCompilationOptions.modulesInfo)

        val compiler = IncrementalJsCompilerRunner(
            workingDir = workingDir,
            reporter = reporter,
            buildHistoryFile = incrementalCompilationOptions.multiModuleICSettings.buildHistoryFile,
            scopeExpansion = if (args.isIrBackendEnabled()) CompileScopeExpansionMode.ALWAYS else CompileScopeExpansionMode.NEVER,
            modulesApiHistory = modulesApiHistory,
            withAbiSnapshot = incrementalCompilationOptions.withAbiSnapshot
        )
        return try {
            compiler.compile(allKotlinFiles, args, compilerMessageCollector, changedFiles)
        } finally {
            reporter.flush()
        }
    }

    protected fun execIncrementalCompiler(
        k2jvmArgs: K2JVMCompilerArguments,
        incrementalCompilationOptions: IncrementalCompilationOptions,
        compilerMessageCollector: MessageCollector,
        reporter: RemoteBuildReporter
    ): ExitCode {
        val allKotlinExtensions = (DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS +
                (incrementalCompilationOptions.kotlinScriptExtensions ?: emptyArray())).distinct()
        val dotExtensions = allKotlinExtensions.map { ".$it" }
        val freeArgs = arrayListOf<String>()
        val allKotlinFiles = arrayListOf<File>()
        for (arg in k2jvmArgs.freeArgs) {
            val file = File(arg)
            if (file.isFile && dotExtensions.any { ext -> file.path.endsWith(ext, ignoreCase = true) }) {
                allKotlinFiles.add(file)
            } else {
                freeArgs.add(arg)
            }
        }
        k2jvmArgs.freeArgs = freeArgs

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        } else {
            ChangedFiles.Unknown()
        }

        val workingDir = incrementalCompilationOptions.workingDir

        val modulesApiHistory = incrementalCompilationOptions.run {
            reporter.report { "Use module detection: ${multiModuleICSettings.useModuleDetection}" }

            if (!multiModuleICSettings.useModuleDetection) {
                ModulesApiHistoryJvm(modulesInfo)
            } else {
                ModulesApiHistoryAndroid(modulesInfo)
            }
        }

        val projectRoot = incrementalCompilationOptions.modulesInfo.projectRoot

        val compiler = IncrementalJvmCompilerRunner(
            workingDir,
            reporter,
            buildHistoryFile = incrementalCompilationOptions.multiModuleICSettings.buildHistoryFile,
            outputFiles = incrementalCompilationOptions.outputFiles,
            usePreciseJavaTracking = incrementalCompilationOptions.usePreciseJavaTracking,
            modulesApiHistory = modulesApiHistory,
            kotlinSourceFilesExtensions = allKotlinExtensions,
            classpathChanges = incrementalCompilationOptions.classpathChanges,
            withAbiSnapshot = incrementalCompilationOptions.withAbiSnapshot
        )
        return try {
            compiler.compile(allKotlinFiles, k2jvmArgs, compilerMessageCollector, changedFiles, projectRoot)
        } finally {
            reporter.flush()
        }
    }

    protected inline fun <R, KotlinJvmReplServiceT> withValidReplImpl(
        sessionId: Int,
        body: KotlinJvmReplServiceT.() -> CompileService.CallResult<R>
    ): CompileService.CallResult<R> =
        withValidClientOrSessionProxy(sessionId) { session ->
            @Suppress("UNCHECKED_CAST")
            (session?.data as? KotlinJvmReplServiceT?)?.body() ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
        }

}

class CompileServiceImpl(
    val registry: Registry,
    val compiler: CompilerSelector,
    compilerId: CompilerId,
    daemonOptions: DaemonOptions,
    val daemonJVMOptions: DaemonJVMOptions,
    port: Int,
    timer: Timer,
    val onShutdown: () -> Unit
) : CompileService, CompileServiceImplBase(daemonOptions, compilerId, port, timer) {

    private inline fun <R> withValidRepl(
        sessionId: Int,
        body: KotlinJvmReplService.() -> CompileService.CallResult<R>
    ) = withValidReplImpl(sessionId, body)

    override val lastUsedSeconds: Long get() =
        if (rwlock.isWriteLocked || rwlock.readLockCount - rwlock.readHoldCount > 0) nowSeconds() else _lastUsedSeconds

    private val rwlock = ReentrantReadWriteLock()

    // RMI-exposed API

    override fun getDaemonInfo(): CompileService.CallResult<String> = ifAlive(minAliveness = Aliveness.Dying) {
        CompileService.CallResult.Good("Kotlin daemon on port $port")
    }

    override fun getKotlinVersion(): CompileService.CallResult<String> = ifAlive {
        try {
            CompileService.CallResult.Good(KotlinCompilerVersion.VERSION)
        } catch (e: Exception) {
            CompileService.CallResult.Error("Unknown Kotlin version")
        }
    }

    override fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = ifAlive {
        CompileService.CallResult.Good(daemonOptions)
    }

    override fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = ifAlive {
        log.info("getDaemonJVMOptions: $daemonJVMOptions")// + daemonJVMOptions.mappers.flatMap { it.toArgs("-") })

        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> = ifAlive(minAliveness = Aliveness.Alive) {
        state.addClient(aliveFlagPath)
        log.info("Registered a client alive file: $aliveFlagPath")
        CompileService.CallResult.Ok()
    }

    override fun getClients(): CompileService.CallResult<List<String>> = ifAlive {
        CompileService.CallResult.Good(state.getClientsFlagPaths())
    }

    // TODO: consider tying a session to a client and use this info to cleanup
    override fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        CompileService.CallResult.Good(
            state.sessions.leaseSession(ClientOrSessionProxy<Any>(aliveFlagPath)).apply {
                log.info("leased a new session $this, session alive file: $aliveFlagPath")
            })
    }


    override fun releaseCompileSession(sessionId: Int) = ifAlive(minAliveness = Aliveness.LastSession) {
        state.sessions.remove(sessionId)
        log.info("cleaning after session $sessionId")
        rwlock.write {
            clearJarCache()
        }
        postReleaseCompileSession()
    }

    override fun checkCompilerId(expectedCompilerId: CompilerId): Boolean =
        (compilerId.compilerVersion.isEmpty() || compilerId.compilerVersion == expectedCompilerId.compilerVersion) &&
                (compilerId.compilerClasspath.all { expectedCompilerId.compilerClasspath.contains(it) }) &&
                !classpathWatcher.isChanged

    override fun getUsedMemory(): CompileService.CallResult<Long> =
        ifAlive { CompileService.CallResult.Good(usedMemory(withGC = true)) }

    override fun shutdown(): CompileService.CallResult<Nothing> = ifAliveExclusive(minAliveness = Aliveness.LastSession) {
        shutdownWithDelay()
        CompileService.CallResult.Ok()
    }

    override fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> = ifAlive(minAliveness = Aliveness.LastSession) {
        val res = when {
            graceful -> gracefulShutdown(true)
            else -> {
                shutdownWithDelay()
                true
            }
        }
        CompileService.CallResult.Good(res)
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun remoteCompile(
        sessionId: Int,
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
                CompileService.OutputFormat.XML -> compiler[targetPlatform].execAndOutputXml(
                    printStream,
                    createCompileServices(
                        servicesFacade,
                        eventManager,
                        profiler
                    ),
                    *args
                )
            }
        }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun remoteIncrementalCompile(
        sessionId: Int,
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
                CompileService.OutputFormat.XML -> compiler[targetPlatform].execAndOutputXml(
                    printStream,
                    createCompileServices(
                        servicesFacade,
                        eventManager,
                        profiler
                    ),
                    *args
                )
            }
        }

    override fun classesFqNamesByFiles(
        sessionId: Int, sourceFiles: Set<File>
    ): CompileService.CallResult<Set<String>> =
        ifAlive {
            withValidClientOrSessionProxy(sessionId) {
                CompileService.CallResult.Good(classesFqNames(sourceFiles))
            }
        }

    override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?
    ) = ifAlive {
        compileImpl(
            sessionId,
            compilerArguments,
            compilationOptions,
            servicesFacade,
            compilationResults,
            hasIncrementalCaches = JpsCompilerServicesFacade::hasIncrementalCaches,
            createMessageCollector = ::CompileServicesFacadeMessageCollector,
            createReporter = ::DaemonMessageReporter,
            createServices = this::createCompileServices,
            getICReporter = { a, b, c -> getBuildReporter(a, b!!, c)}
        )
    }


    @Deprecated("The usages should be replaced with other `leaseReplSession` method", ReplaceWith("leaseReplSession"))
    override fun leaseReplSession(
        aliveFlagPath: String?,
        targetPlatform: CompileService.TargetPlatform,
        @Suppress("DEPRECATION") servicesFacade: CompilerCallbackServicesFacade,
        templateClasspath: List<File>,
        templateClassName: String,
        scriptArgs: Array<out Any?>?,
        scriptArgsTypes: Array<out Class<out Any>>?,
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
            val compilerMessagesStream = PrintStream(
                BufferedOutputStream(
                    RemoteOutputStreamClient(compilerMessagesOutputStream, DummyProfiler()),
                    REMOTE_STREAM_BUFFER_SIZE
                )
            )
            val messageCollector = KeepFirstErrorMessageCollector(compilerMessagesStream)
            val repl = KotlinJvmReplService(
                disposable, port, compilerId, templateClasspath, templateClassName,
                messageCollector, operationsTracer
            )
            val sessionId = state.sessions.leaseSession(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    // TODO: add more checks (e.g. is it a repl session)
    override fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = releaseCompileSession(sessionId)

    @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                @Suppress("DEPRECATION")
                CompileService.CallResult.Good(check(codeLine))
            }
        }

    private fun createCompileServices(
        @Suppress("DEPRECATION") facade: CompilerCallbackServicesFacade,
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
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        }
        if (facade.hasExpectActualTracker()) {
            builder.register(ExpectActualTracker::class.java, RemoteExpectActualTracker(facade, rpcProfiler))
        }
        if (facade.hasInlineConstTracker()) {
            builder.register(InlineConstTracker::class.java, RemoteInlineConstTracker(facade, rpcProfiler))
        }
        if (facade.hasIncrementalResultsConsumer()) {
            builder.register(IncrementalResultsConsumer::class.java, RemoteIncrementalResultsConsumer(facade, eventManager, rpcProfiler))
        }
        if (facade.hasIncrementalDataProvider()) {
            builder.register(IncrementalDataProvider::class.java, RemoteIncrementalDataProvider(facade, rpcProfiler))
        }

        return builder.build()
    }

    @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun remoteReplLineCompile(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplCompileResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                @Suppress("DEPRECATION")
                CompileService.CallResult.Good(compile(codeLine, history))
            }
        }

    @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun remoteReplLineEval(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplEvalResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            CompileService.CallResult.Error("Eval on daemon is not supported")
        }

    override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive) {
        if (compilationOptions.targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val messageCollector = CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
            val repl = KotlinJvmReplService(
                disposable, port, compilerId, templateClasspath, templateClassName,
                messageCollector, null
            )
            val sessionId = state.sessions.leaseSession(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    override fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacade> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                CompileService.CallResult.Good(createRemoteState(port))
            }
        }

    override fun replCheck(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                withValidReplState(replStateId) { state ->
                    check(state, codeLine)
                }
            }
        }

    override fun replCompile(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCompileResult> =
        ifAlive(minAliveness = Aliveness.Alive) {
            withValidRepl(sessionId) {
                withValidReplState(replStateId) { state ->
                    compile(state, codeLine)
                }
            }
        }

    override fun periodicAndAfterSessionCheck() {

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
                    clearJarCache()
                }
            }
        }
    }

    override fun periodicSeldomCheck() {
        ifAliveUnit(minAliveness = Aliveness.Alive) {

            // compiler changed (seldom check) - shutdown
            if (classpathWatcher.isChanged) {
                log.info("Compiler changed.")
                gracefulShutdown(false)
            }
        }
    }


    // TODO: handover should include mechanism for client to switch to a new daemon then previous "handed over responsibilities" and shot down
    override fun initiateElections() {

        ifAliveUnit {

            log.info("initiate elections")
            val aliveWithOpts = walkDaemons(
                File(daemonOptions.runFilesPathOrDefault),
                compilerId,
                runFile,
                filter = { _, p -> p != port },
                report = { _, msg -> log.info(msg) }).toList()
            val comparator =
                compareByDescending<DaemonWithMetadata, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator(), { it.jvmOptions })
                    .thenBy(FileAgeComparator()) { it.runFile }
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
                            daemon.getClients().takeIf { it.isGood }?.let {
                                it.get().forEach { clientAliveFile -> registerClient(clientAliveFile) }
                            }
                            daemon.scheduleShutdown(true)
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
                    getClients().takeIf { it.isGood }?.let {
                        it.get().forEach { bestDaemonWithMetadata.daemon.registerClient(it) }
                    }
                    scheduleShutdown(true)
                } else {
                    // undecided, do nothing
                    log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE equal prio, continue: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                    // TODO: implement some behaviour here, e.g.:
                    //   - shutdown/takeover smaller daemon
                    //   - run (or better persuade client to run) a bigger daemon (in fact may be even simple shutdown will do, because of client's daemon choosing logic)
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

        UnicastRemoteObject.unexportObject(this, true)
        log.info("Shutdown complete")
        onShutdown()
        log.handlers.forEach { it.flush() }
    }

    private fun shutdownWithDelay() {
        state.delayedShutdownQueued.set(true)
        val currentClientsCount = state.clientsCounter
        val currentSessionId = state.sessions.lastSessionId
        val currentCompilationsCount = compilationsCounter.get()
        log.info("Delayed shutdown in ${daemonOptions.shutdownDelayMilliseconds}ms")
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            state.delayedShutdownQueued.set(false)
            if (currentClientsCount == state.clientsCounter &&
                currentCompilationsCount == compilationsCounter.get() &&
                currentSessionId == state.sessions.lastSessionId
            ) {
                ifAliveExclusiveUnit(minAliveness = Aliveness.LastSession) {
                    log.fine("Execute delayed shutdown")
                    shutdownNow()
                }
            } else {
                log.info("Cancel delayed shutdown due to a new activity")
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

    // todo: remove after remoteIncrementalCompile is removed
    private fun doCompile(
        sessionId: Int,
        args: Array<out String>,
        compilerMessagesStreamProxy: RemoteOutputStream,
        serviceOutputStreamProxy: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?,
        body: (PrintStream, EventManager, Profiler) -> ExitCode
    ): CompileService.CallResult<Int> =
        ifAlive {
            withValidClientOrSessionProxy(sessionId) {
                operationsTracer?.before("compile")
                val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
                val eventManger = EventManagerImpl()
                val compilerMessagesStream = PrintStream(
                    BufferedOutputStream(
                        RemoteOutputStreamClient(compilerMessagesStreamProxy, rpcProfiler),
                        REMOTE_STREAM_BUFFER_SIZE
                    )
                )
                val serviceOutputStream = PrintStream(
                    BufferedOutputStream(
                        RemoteOutputStreamClient(serviceOutputStreamProxy, rpcProfiler),
                        REMOTE_STREAM_BUFFER_SIZE
                    )
                )
                try {
                    val compileServiceReporter = DaemonMessageReporterPrintStreamAdapter(serviceOutputStream)
                    if (args.none())
                        throw IllegalArgumentException("Error: empty arguments list.")
                    log.info("Starting compilation with args: " + args.joinToString(" "))
                    val exitCode = checkedCompile(compileServiceReporter, rpcProfiler) {
                        body(compilerMessagesStream, eventManger, rpcProfiler).code
                    }
                    CompileService.CallResult.Good(exitCode)
                } finally {
                    serviceOutputStream.flush()
                    compilerMessagesStream.flush()
                    eventManger.fireCompilationFinished()
                    operationsTracer?.after("compile")
                }
            }
        }

    init {
        // assuming logicaly synchronized
        try {
            // cleanup for the case of incorrect restart and many other situations
            UnicastRemoteObject.unexportObject(this, false)
        } catch (e: NoSuchObjectException) {
            // ignoring if object already exported
        }

        val stub = UnicastRemoteObject.exportObject(
            this,
            port,
            LoopbackNetworkInterface.clientLoopbackSocketFactory,
            LoopbackNetworkInterface.serverLoopbackSocketFactory
        ) as CompileService
        registry.rebind(COMPILER_SERVICE_RMI_NAME, stub)
    }

    override fun clearJarCache() {
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
}
