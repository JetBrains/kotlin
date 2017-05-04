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
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.incremental.RemoteAnnotationsFileUpdater
import org.jetbrains.kotlin.daemon.incremental.RemoteArtifactChangesProvider
import org.jetbrains.kotlin.daemon.incremental.RemoteChangesRegistry
import org.jetbrains.kotlin.daemon.report.CompileServicesFacadeMessageCollector
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporter
import org.jetbrains.kotlin.daemon.report.DaemonMessageReporterPrintStreamAdapter
import org.jetbrains.kotlin.daemon.report.RemoteICReporter
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.stackTraceStr
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
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
    fun onCompilationFinished(f : () -> Unit)
}

private class EventManagerImpl : EventManager {
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

    private val log by lazy { Logger.getLogger("compiler") }

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

    private val compilationsCounter = AtomicInteger(0)

    private val classpathWatcher = LazyClasspathWatcher(compilerId.compilerClasspath)

    enum class Aliveness {
        // !!! ordering of values is used in state comparison
        Dying, LastSession, Alive
    }

    private class SessionsContainer {

        private val lock = ReentrantReadWriteLock()
        private val sessions: MutableMap<Int, ClientOrSessionProxy<Any>> = hashMapOf()
        private val sessionsIdCounter = AtomicInteger(0)

        fun<T: Any> leaseSession(session: ClientOrSessionProxy<T>): Int = lock.write {
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

        val clientProxies: MutableSet<ClientOrSessionProxy<Any>> = hashSetOf()
        val sessions = SessionsContainer()

        val delayedShutdownQueued = AtomicBoolean(false)

        var alive = AtomicInteger(Aliveness.Alive.ordinal)
    }

    @Volatile private var _lastUsedSeconds = nowSeconds()
    val lastUsedSeconds: Long get() = if (rwlock.isWriteLocked || rwlock.readLockCount - rwlock.readHoldCount > 0) nowSeconds() else _lastUsedSeconds

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
        log.info("getDaemonJVMOptions: $daemonJVMOptions")// + daemonJVMOptions.mappers.flatMap { it.toArgs("-") })

        CompileService.CallResult.Good(daemonJVMOptions)
    }

    override fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> = ifAlive {
        synchronized(state.clientProxies) {
            state.clientProxies.add(ClientOrSessionProxy(aliveFlagPath))
            log.info("Registered a client alive file: $aliveFlagPath")
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
        if (state.sessions.isEmpty()) {
            // TODO: and some goes here
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
        shutdownWithDelay()
        CompileService.CallResult.Ok()
    }

    override fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> = ifAlive(minAliveness = Aliveness.LastSession, ignoreCompilerChanged = true) {
        when {
            !graceful -> {
                shutdownWithDelay()
                CompileService.CallResult.Good(true)
            }
            state.alive.compareAndSet(Aliveness.Alive.ordinal, Aliveness.LastSession.ordinal) -> {
                timer.schedule(1) {
                    ifAliveExclusive(minAliveness = Aliveness.LastSession, ignoreCompilerChanged = true) {
                        when {
                            state.sessions.isEmpty() -> shutdownWithDelay()
                            else -> {
                                daemonOptions.autoshutdownIdleSeconds = TimeUnit.MILLISECONDS.toSeconds(daemonOptions.forceShutdownTimeoutMilliseconds).toInt()
                                daemonOptions.autoshutdownUnusedSeconds = daemonOptions.autoshutdownIdleSeconds
                                log.info("Graceful shutdown signalled; unused/idle timeouts are set to ${daemonOptions.autoshutdownUnusedSeconds}/${daemonOptions.autoshutdownIdleSeconds}s")
                                log.info("Some sessions are active, waiting for them to finish")
                            }
                        }
                        CompileService.CallResult.Ok()
                    }
                }
                CompileService.CallResult.Good(true)
            }
            else -> CompileService.CallResult.Good(false)
        }
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

    override fun compile(
            sessionId: Int,
            compilerArguments: Array<out String>,
            compilationOptions: CompilationOptions,
            servicesFacade: CompilerServicesFacadeBase,
            compilationResults: CompilationResults?
    ): CompileService.CallResult<Int> = ifAlive {
        val messageCollector = CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
        val daemonReporter = DaemonMessageReporter(servicesFacade, compilationOptions)
        val compilerMode = compilationOptions.compilerMode
        val targetPlatform = compilationOptions.targetPlatform
        log.info("Starting compilation with args: " + compilerArguments.joinToString(" "))
        val k2PlatformArgs = try {
            when (targetPlatform) {
                CompileService.TargetPlatform.JVM -> K2JVMCompilerArguments().apply { K2JVMCompiler().parseArguments(compilerArguments, this) }
                CompileService.TargetPlatform.JS -> K2JSCompilerArguments().apply { K2JSCompiler().parseArguments(compilerArguments, this) }
                CompileService.TargetPlatform.METADATA -> K2MetadataCompilerArguments().apply { K2MetadataCompiler().parseArguments(compilerArguments, this) }
            }
        }
        catch (e: IllegalArgumentException) {
            messageCollector.report(CompilerMessageSeverity.EXCEPTION, e.stackTraceStr)
            return@ifAlive CompileService.CallResult.Error("Could not deserialize compiler arguments")
        }

        return@ifAlive when (compilerMode) {
            CompilerMode.JPS_COMPILER -> {
                val jpsServicesFacade = servicesFacade as JpsCompilerServicesFacade

                doCompile(sessionId, daemonReporter, tracer = null) { eventManger, profiler ->
                    val services = createCompileServices(jpsServicesFacade, eventManger, profiler)
                    execCompiler(compilationOptions.targetPlatform, services, k2PlatformArgs, messageCollector)
                }
            }
            CompilerMode.NON_INCREMENTAL_COMPILER -> {
                doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                    execCompiler(targetPlatform, Services.EMPTY, k2PlatformArgs, messageCollector)
                }
            }
            CompilerMode.INCREMENTAL_COMPILER -> {
                if (targetPlatform != CompileService.TargetPlatform.JVM) {
                    throw IllegalStateException("Incremental compilation is not supported for target platform: $targetPlatform")
                }

                val k2jvmArgs = k2PlatformArgs as K2JVMCompilerArguments
                val gradleIncrementalArgs = compilationOptions as IncrementalCompilationOptions
                val gradleIncrementalServicesFacade = servicesFacade as IncrementalCompilerServicesFacade

                withIC {
                    doCompile(sessionId, daemonReporter, tracer = null) { _, _ ->
                        execIncrementalCompiler(k2jvmArgs, gradleIncrementalArgs, gradleIncrementalServicesFacade, compilationResults!!,
                                                messageCollector, daemonReporter)
                    }
                }

            }
            else -> throw IllegalStateException("Unknown compilation mode $compilerMode")
        }
    }

    private fun execCompiler(
            targetPlatform: CompileService.TargetPlatform,
            services: Services,
            args: CommonCompilerArguments,
            messageCollector: MessageCollector
    ): ExitCode =
            when(targetPlatform) {
                CompileService.TargetPlatform.JVM -> {
                    K2JVMCompiler().exec(messageCollector, services, args as K2JVMCompilerArguments)
                }
                CompileService.TargetPlatform.JS -> {
                    K2JSCompiler().exec(messageCollector, services, args as K2JSCompilerArguments)
                }
                CompileService.TargetPlatform.METADATA -> {
                    K2MetadataCompiler().exec(messageCollector, services, args as K2MetadataCompilerArguments)
                }
            }

    private fun execIncrementalCompiler(
            k2jvmArgs: K2JVMCompilerArguments,
            incrementalCompilationOptions: IncrementalCompilationOptions,
            servicesFacade: IncrementalCompilerServicesFacade,
            compilationResults: CompilationResults,
            compilerMessageCollector: MessageCollector,
            daemonMessageReporter: DaemonMessageReporter
    ): ExitCode {
        val reporter = RemoteICReporter(servicesFacade, compilationResults, incrementalCompilationOptions)
        val annotationFileUpdater = if (servicesFacade.hasAnnotationsFileUpdater()) RemoteAnnotationsFileUpdater(servicesFacade) else null

        val moduleFile = k2jvmArgs.module?.let(::File)
        assert(moduleFile?.exists() ?: false) { "Module does not exist ${k2jvmArgs.module}" }

        // todo: pass javaSourceRoots and allKotlinFiles using IncrementalCompilationOptions
        val parsedModule = run {
            val bytesOut = ByteArrayOutputStream()
            val printStream = PrintStream(bytesOut)
            val mc = PrintingMessageCollector(printStream, MessageRenderer.PLAIN_FULL_PATHS, false)
            val parsedModule = ModuleXmlParser.parseModuleScript(k2jvmArgs.module, mc)
            if (mc.hasErrors()) {
                daemonMessageReporter.report(ReportSeverity.ERROR, bytesOut.toString("UTF8"))
            }
            parsedModule
        }
        val javaSourceRoots = parsedModule.modules.flatMapTo(HashSet()) { it.getJavaSourceRoots().map { File(it.path) } }
        val allKotlinFiles = parsedModule.modules.flatMap { it.getSourceFiles().map(::File) }
        k2jvmArgs.friendPaths = parsedModule.modules.flatMap(Module::getFriendPaths).toTypedArray()

        val changedFiles = if (incrementalCompilationOptions.areFileChangesKnown) {
            ChangedFiles.Known(incrementalCompilationOptions.modifiedFiles!!, incrementalCompilationOptions.deletedFiles!!)
        }
        else {
            ChangedFiles.Unknown()
        }

        val artifactChanges = RemoteArtifactChangesProvider(servicesFacade)
        val changesRegistry = RemoteChangesRegistry(servicesFacade)

        val workingDir = incrementalCompilationOptions.workingDir
        val versions = commonCacheVersions(workingDir) +
                       customCacheVersion(incrementalCompilationOptions.customCacheVersion, incrementalCompilationOptions.customCacheVersionFileName, workingDir, forceEnable = true)

        return IncrementalJvmCompilerRunner(workingDir, javaSourceRoots, versions, reporter, annotationFileUpdater,
                                            artifactChanges, changesRegistry)
                .compile(allKotlinFiles, k2jvmArgs, compilerMessageCollector, { changedFiles })
    }

    override fun leaseReplSession(
            aliveFlagPath: String?,
            targetPlatform: CompileService.TargetPlatform,
            servicesFacade: CompilerCallbackServicesFacade,
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
            val compilerMessagesStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(compilerMessagesOutputStream, DummyProfiler()), REMOTE_STREAM_BUFFER_SIZE))
            val messageCollector = KeepFirstErrorMessageCollector(compilerMessagesStream)
            val repl = KotlinJvmReplService(disposable, port, templateClasspath, templateClassName,
                                            messageCollector, operationsTracer)
            val sessionId = state.sessions.leaseSession(ClientOrSessionProxy(aliveFlagPath, repl, disposable))

            CompileService.CallResult.Good(sessionId)
        }
    }

    // TODO: add more checks (e.g. is it a repl session)
    override fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = releaseCompileSession(sessionId)

    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                withValidRepl(sessionId) {
                    CompileService.CallResult.Good(check(codeLine))
                }
            }

    override fun remoteReplLineCompile(sessionId: Int, codeLine: ReplCodeLine, history: List<ReplCodeLine>?): CompileService.CallResult<ReplCompileResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                withValidRepl(sessionId) {
                    CompileService.CallResult.Good(compile(codeLine, history))
                }
            }

    override fun remoteReplLineEval(
            sessionId: Int,
            codeLine: ReplCodeLine,
            history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplEvalResult> =
            ifAlive(minAliveness = Aliveness.Alive) {
                CompileService.CallResult.Error("Eval on daemon is not supported")
            }

    override fun leaseReplSession(aliveFlagPath: String?,
                                  compilerArguments: Array<out String>,
                                  compilationOptions: CompilationOptions,
                                  servicesFacade: CompilerServicesFacadeBase,
                                  templateClasspath: List<File>,
                                  templateClassName: String
    ): CompileService.CallResult<Int> = ifAlive(minAliveness = Aliveness.Alive)  {
        if (compilationOptions.targetPlatform != CompileService.TargetPlatform.JVM)
            CompileService.CallResult.Error("Sorry, only JVM target platform is supported now")
        else {
            val disposable = Disposer.newDisposable()
            val messageCollector = CompileServicesFacadeMessageCollector(servicesFacade, compilationOptions)
            val repl = KotlinJvmReplService(disposable, port, templateClasspath, templateClassName,
                                            messageCollector, null)
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

        timer.schedule(10) {
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
                scheduleShutdown(true)
            }
            else {
                var anyDead = state.sessions.cleanDead()
                var shuttingDown = false


                // 3. check if in graceful shutdown state and all sessions are closed
                if (shutdownCondition({ state.alive.get() == Aliveness.LastSession.ordinal && state.sessions.isEmpty() }, "All sessions finished, shutting down")) {
                    shutdownWithDelay()
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
                    scheduleShutdown(true)
                    shuttingDown = true
                }

                if (anyDead && !shuttingDown) {
                    clearJarCache()
                }
            }
            CompileService.CallResult.Ok()
        }
    }


    // TODO: handover should include mechanism for client to switch to a new daemon then previous "handed over responsibilities" and shot down
    private fun initiateElections() {

        ifAlive {

            val aliveWithOpts = walkDaemons(File(daemonOptions.runFilesPathOrDefault), compilerId, runFile, filter = { _, p -> p != port }, report = { _, msg -> log.info(msg) }).toList()
            val comparator = compareByDescending<DaemonWithMetadata, DaemonJVMOptions>(DaemonJVMOptionsMemoryComparator(), { it.jvmOptions })
                    .thenBy(FileAgeComparator()) { it.runFile }
            aliveWithOpts.maxWith(comparator)?.let { bestDaemonWithMetadata ->
                val fattestOpts = bestDaemonWithMetadata.jvmOptions
                if (fattestOpts memorywiseFitsInto daemonJVMOptions && FileAgeComparator().compare(bestDaemonWithMetadata.runFile, runFile) < 0 ) {
                    // all others are smaller that me, take overs' clients and shut them down
                    log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE lower prio, taking clients from them and schedule them to shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                    aliveWithOpts.forEach { (daemon, runFile, _) ->
                        try {
                            daemon.getClients().takeIf { it.isGood }?.let {
                                it.get().forEach { clientAliveFile -> registerClient(clientAliveFile) }
                            }
                            daemon.scheduleShutdown(true)
                        }
                        catch (e: Exception) {
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
                else if (daemonJVMOptions memorywiseFitsInto fattestOpts && FileAgeComparator().compare(bestDaemonWithMetadata.runFile, runFile) > 0) {
                    // there is at least one bigger, handover my clients to it and shutdown
                    log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE higher prio, handover clients to it and schedule shutdown: my runfile: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                    getClients().takeIf { it.isGood }?.let {
                        it.get().forEach { bestDaemonWithMetadata.daemon.registerClient(it) }
                    }
                    scheduleShutdown(true)
                }
                else {
                    // undecided, do nothing
                    log.info("$LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE equal prio, continue: ${runFile.name} (${runFile.lastModified()}) vs best other runfile: ${bestDaemonWithMetadata.runFile.name} (${bestDaemonWithMetadata.runFile.lastModified()})")
                    // TODO: implement some behaviour here, e.g.:
                    //   - shutdown/takeover smaller daemon
                    //   - run (or better persuade client to run) a bigger daemon (in fact may be even simple shutdown will do, because of client's daemon choosing logic)
                }
            }
            CompileService.CallResult.Ok()
        }
    }

    private fun shutdownImpl() {
        log.info("Shutdown started")
        fun Long.mb() = this / (1024 * 1024)
        with (Runtime.getRuntime()) {
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
        val currentCompilationsCount = compilationsCounter.get()
        timer.schedule(daemonOptions.shutdownDelayMilliseconds) {
            state.delayedShutdownQueued.set(false)
            if (currentCompilationsCount == compilationsCounter.get()) {
                ifAliveExclusive(minAliveness = Aliveness.LastSession, ignoreCompilerChanged = true) {
                    log.fine("Execute delayed shutdown")
                    shutdownImpl()
                    CompileService.CallResult.Ok()
                }
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

    // todo: remove after remoteIncrementalCompile is removed
    private fun doCompile(sessionId: Int,
                          args: Array<out String>,
                          compilerMessagesStreamProxy: RemoteOutputStream,
                          serviceOutputStreamProxy: RemoteOutputStream,
                          operationsTracer: RemoteOperationsTracer?,
                          body: (PrintStream, EventManager, Profiler) -> ExitCode): CompileService.CallResult<Int> =
            ifAlive {
                withValidClientOrSessionProxy(sessionId) {
                    operationsTracer?.before("compile")
                    val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
                    val eventManger = EventManagerImpl()
                    val compilerMessagesStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(compilerMessagesStreamProxy, rpcProfiler), REMOTE_STREAM_BUFFER_SIZE))
                    val serviceOutputStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(serviceOutputStreamProxy, rpcProfiler), REMOTE_STREAM_BUFFER_SIZE))
                    try {
                        val compileServiceReporter = DaemonMessageReporterPrintStreamAdapter(serviceOutputStream)
                        if (args.none())
                            throw IllegalArgumentException("Error: empty arguments list.")
                        log.info("Starting compilation with args: " + args.joinToString(" "))
                        val exitCode = checkedCompile(compileServiceReporter, rpcProfiler) {
                            body(compilerMessagesStream, eventManger, rpcProfiler).code
                        }
                        CompileService.CallResult.Good(exitCode)
                    }
                    finally {
                        serviceOutputStream.flush()
                        compilerMessagesStream.flush()
                        eventManger.fireCompilationFinished()
                        operationsTracer?.after("compile")
                    }
                }
            }

    private fun doCompile(sessionId: Int,
                          daemonMessageReporter: DaemonMessageReporter,
                          tracer: RemoteOperationsTracer?,
                          body: (EventManager, Profiler) -> ExitCode): CompileService.CallResult<Int> =
            ifAlive {
                withValidClientOrSessionProxy(sessionId) {
                    tracer?.before("compile")
                    val rpcProfiler = if (daemonOptions.reportPerf) WallAndThreadTotalProfiler() else DummyProfiler()
                    val eventManger = EventManagerImpl()
                    try {
                        val exitCode = checkedCompile(daemonMessageReporter, rpcProfiler) {
                            body(eventManger, rpcProfiler).code
                        }
                        CompileService.CallResult.Good(exitCode)
                    }
                    finally {
                        eventManger.fireCompilationFinished()
                        tracer?.after("compile")
                    }
                }
            }

    private fun createCompileServices(facade: CompilerCallbackServicesFacade, eventManager: EventManager, rpcProfiler: Profiler): Services {
        val builder = Services.Builder()
        if (facade.hasIncrementalCaches() || facade.hasLookupTracker()) {
            builder.register(IncrementalCompilationComponents::class.java, RemoteIncrementalCompilationComponentsClient(facade, eventManager, rpcProfiler))
        }
        if (facade.hasCompilationCanceledStatus()) {
            builder.register(CompilationCanceledStatus::class.java, RemoteCompilationCanceledStatusClient(facade, rpcProfiler))
        }
        return builder.build()
    }


    private fun<R> checkedCompile(daemonMessageReporter: DaemonMessageReporter, rpcProfiler: Profiler, body: () -> R): R {
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

                "PERF: Compile on daemon: ${pc.time.ms()} ms; thread: user ${pc.threadUserTime.ms()} ms, sys ${(pc.threadTime - pc.threadUserTime).ms()} ms; rpc: ${rpc.count} calls, ${rpc.time.ms()} ms, thread ${rpc.threadTime.ms()} ms; memory: ${endMem.kb()} kb (${"%+d".format(pc.memory.kb())} kb)".let {
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
        catch (e: Exception) {
            log.info("Error: $e")
            throw e
        }
    }

    override fun clearJarCache() {
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

    inline private fun<R> ifAliveChecksImpl(minAliveness: Aliveness = Aliveness.Alive, ignoreCompilerChanged: Boolean = false, body: () -> CompileService.CallResult<R>): CompileService.CallResult<R> {
        val curState = state.alive.get()
        return when {
            curState < minAliveness.ordinal -> {
                log.info("Cannot perform operation, requested state: ${minAliveness.name} > actual: ${try {
                    Aliveness.values()[curState].name
                }
                catch (_: Throwable) {
                    "invalid($curState)"
                }}")
                CompileService.CallResult.Dying()
            }
            !ignoreCompilerChanged && classpathWatcher.isChanged -> {
                log.info("Compiler changed, scheduling shutdown")
                shutdownWithDelay()
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

    @JvmName("withValidRepl1")
    private inline fun<R> withValidRepl(sessionId: Int, body: KotlinJvmReplService.() -> CompileService.CallResult<R>): CompileService.CallResult<R> =
            withValidClientOrSessionProxy(sessionId) { session ->
                (session?.data as? KotlinJvmReplService?)?.body() ?: CompileService.CallResult.Error("Not a REPL session $sessionId")
            }
}
