/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.unit

import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.daemon.CompileServiceImpl
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.client.KotlinCompilerDaemonClient
import org.jetbrains.kotlin.daemon.client.experimental.BasicCompilerServicesWithResultsFacadeServerServerSide
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.DaemonWithMetadataAsync
import org.jetbrains.kotlin.daemon.common.experimental.findCallbackServerSocket
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket
import org.jetbrains.kotlin.daemon.common.experimental.walkDaemonsAsync
import org.jetbrains.kotlin.daemon.experimental.CompileServiceServerSideImpl
import org.jetbrains.kotlin.daemon.loggerCompatiblePath
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.test.IgnoreAll
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.util.*
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.concurrent.schedule

@RunWith(IgnoreAll::class)
class ConnectionsTest : KotlinIntegrationTestBase() {

    val kotlinCompilerClient = KotlinCompilerDaemonClient.instantiate(DaemonProtocolVariant.SOCKETS)

    private val logFile = createTempFile("/Users/jetbrains/Documents/kotlin/my_fork/kotlin", ".txt")

    private val cfg = "handlers = java.util.logging.FileHandler\n" +
            "java.util.logging.FileHandler.level     = ALL\n" +
            "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
            "java.util.logging.FileHandler.encoding  = UTF-8\n" +
            "java.util.logging.FileHandler.limit     = 0\n" + // if file is provided - disabled, else - 1Mb
            "java.util.logging.FileHandler.count     = 1\n" +
            "java.util.logging.FileHandler.append    = true\n" +
            "java.util.logging.FileHandler.pattern   = ${logFile.loggerCompatiblePath}\n" +
            "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s%n\n"

    init {
        LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
    }

    val log by lazy { Logger.getLogger("ConnectionsTest") }

    private val daemonJVMOptions by lazy {
        configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
    }

    private val compilerId by lazy { CompilerId() }

    private val daemonOptions by lazy { DaemonOptions() }

    private val port
        get() = findPortForSocket(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            COMPILE_DAEMON_PORTS_RANGE_START,
            COMPILE_DAEMON_PORTS_RANGE_END
        )

    private val timer by lazy { Timer(true) }

    private val runFile by lazy {
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        File(
            runFileDir,
            makeRunFilenameString(
                    timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                    digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                    port = port.toString()
            )
        )
    }

    private val onShutdown: () -> Unit = {
        if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
            // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
            timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                cancel()
                org.jetbrains.kotlin.daemon.KotlinCompileDaemon.log.info("force JVM shutdown")
                System.exit(0)
            }
        } else {
            timer.cancel()
        }
    }


    private fun getNewDaemonsOrAsyncWrappers() = runBlocking {
        walkDaemonsAsync(
            File(daemonOptions.runFilesPathOrDefault),
            compilerId,
            runFile,
            filter = { _, _ -> true },
            report = { _, msg -> log.info(msg) },
            useRMI = true,
            useSockets = true
        ).toList()
    }

    private fun getOldDaemonsOrRMIWrappers() = runBlocking {
        walkDaemons(
                File(daemonOptions.runFilesPathOrDefault),
                compilerId,
                runFile,
                filter = { _, _ -> true },
                report = { _, msg -> log.info(msg) }
        ).toList()
    }

    companion object {

    }

    private fun runNewServer(): Deferred<Unit> =
        port.let { serverPort ->
            CompileServiceServerSideImpl(
                serverPort,
                compilerId,
                daemonOptions,
                daemonJVMOptions,
                serverPort.port,
                timer,
                onShutdown
            ).let {
                log.info("service created")
                it.startDaemonElections()
                it.configurePeriodicActivities()
                it.runServer()
            }
        }

    private fun runOldServer() {
        val (registry, serverPort) = findPortAndCreateRegistry(
                COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
                COMPILE_DAEMON_PORTS_RANGE_START,
                COMPILE_DAEMON_PORTS_RANGE_END
        )
        val compilerSelector = object : CompilerSelector {
            private val jvm by lazy { K2JVMCompiler() }
            private val js by lazy { K2JSCompiler() }
            private val metadata by lazy { K2MetadataCompiler() }
            override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = when (targetPlatform) {
                CompileService.TargetPlatform.JVM -> jvm
                CompileService.TargetPlatform.JS -> js
                CompileService.TargetPlatform.METADATA -> metadata
            }
        }
        log.info("old server run: (port= $serverPort, reg= $registry)")
        CompileServiceImpl(
            registry = registry,
            compiler = compilerSelector,
            compilerId = compilerId,
            daemonOptions = daemonOptions,
            daemonJVMOptions = daemonJVMOptions,
            port = serverPort,
            timer = timer,
            onShutdown = onShutdown
        ).let {
            it.startDaemonElections()
            it.configurePeriodicActivities()
        }
    }

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

    private fun <DaemonWithMeta, Daemon> expectDaemon(
        getDaemons: () -> List<DaemonWithMeta>,
        chooseDaemon: (List<DaemonWithMeta>) -> Daemon,
        getInfo: (Daemon) -> CompileService.CallResult<String>,
        registerClient: (Daemon) -> Unit,
        port: (Daemon) -> Int,
        expectedDaemonCount: Int?,
        extraAction: (Daemon) -> Unit = {}
    ) {
        val daemons = getDaemons()
        log.info("daemons (${daemons.size}) : ${daemons.map { (it ?: 0)::class.java.name }.toList()}\n\n")
        expectedDaemonCount?.let {
            log.info("expected $it daemons, found ${daemons.size}")
            assertTrue(
                "daemons.size : ${daemons.size}, but expected : $expectedDaemonCount",
                daemons.size == it
            )
        }
        val daemon = chooseDaemon(daemons)
        log.info("chosen : $daemon (port = ${port(daemon)})")
        val info = getInfo(daemon)
        log.info("info : $info")
        assertTrue("bad info", info.isGood)
        registerClient(daemon)
        extraAction(daemon)
    }

    private enum class ServerType(val instancesNumber: Int?) {
        OLD(1), NEW(2), ANY(null)
    }

    private fun expectNewDaemon(serverType: ServerType, extraAction: (CompileServiceAsync) -> Unit = {}) = expectDaemon(
        getDaemons = ::getNewDaemonsOrAsyncWrappers,
        chooseDaemon = { daemons -> daemons.maxWithOrNull(comparator)!!.daemon },
        getInfo = { d -> runBlocking { d.getDaemonInfo() } },
        registerClient = { d -> runBlocking { d.registerClient(generateClient()) } },
        port = { d -> d.serverPort },
        expectedDaemonCount = serverType.instancesNumber,
        extraAction = extraAction
    )

    private fun expectOldDaemon(shouldCheckNumber: Boolean = true, extraAction: (CompileService) -> Unit = {}) = expectDaemon(
        ::getOldDaemonsOrRMIWrappers,
        { daemons -> daemons[0].daemon },
        { d -> d.getDaemonInfo() },
        { d -> d.registerClient(generateClient()) },
        { _ -> -1 },
        1.takeIf { shouldCheckNumber },
        extraAction
    )

    private val clientFiles = arrayListOf<File>()
    private fun generateClient(): String {
        val file = createTempFile(getTestName(true), ".alive")
        clientFiles.add(file)
        return file.absolutePath
    }

    private fun deleteClients() {
        clientFiles.forEach { it.delete() }
    }

    private fun endTest() {
        deleteClients()
    }

    fun ignore_testConnectionMechanism_OldClient_OldServer() {
        runOldServer()
        expectOldDaemon()
        endTest()
    }


    fun ignore_testConnectionMechanism_NewClient_NewServer() {
        runNewServer()
        expectNewDaemon(ServerType.NEW)
        endTest()
    }

    fun ignore_testConnectionMechanism_OldClient_NewServer() {
        runNewServer()
        expectOldDaemon()
        endTest()
    }

    fun ignore_testConnectionMechanism_NewClient_OldServer() {
        runOldServer()
        expectNewDaemon(ServerType.OLD)
        endTest()
    }


    fun ignore_testConnections_OldDaemon_DifferentClients() {
        runOldServer()
        (0..20).forEach {
            expectNewDaemon(ServerType.OLD)
            expectOldDaemon()
        }
        endTest()
    }

    fun ignore_testConnections_NewDaemon_DifferentClients() {
        runNewServer()
        (0..4).forEach {
            expectNewDaemon(ServerType.NEW)
            expectOldDaemon()
        }
        endTest()
    }

    fun ignore_testConnections_MultipleDaemons_MultipleClients() {
        (0..3).forEach {
            runNewServer()
            runOldServer()
        }
        (0..4).forEach {
            expectNewDaemon(ServerType.ANY)
            expectOldDaemon(shouldCheckNumber = false)
        }
        endTest()
    }

    fun ignore_testShutdown() {
        runNewServer()
        expectNewDaemon(ServerType.NEW) { daemon ->
            runBlocking {
                daemon.shutdown()
                delay(1000L)
                val mem: Long = try {
                    daemon.getUsedMemory().get()
                } catch (e: IOException) {
                    -100500L
                }
                assertTrue(mem == -100500L)
            }
        }
    }


    fun ignore_testCompile() {
        runNewServer()
        expectNewDaemon(ServerType.NEW) { daemon ->
            runBlocking {
                assertTrue("daemon is wrapper", daemon !is CompileServiceAsyncWrapper)
                val outStream = ByteArrayOutputStream()
                val msgCollector = PrintingMessageCollector(PrintStream(outStream), MessageRenderer.WITHOUT_PATHS, true)
                val codes = (0 until 10).toMutableList()
                val services = BasicCompilerServicesWithResultsFacadeServerServerSide(
                    msgCollector,
                    { _, _ -> },
                    findCallbackServerSocket()
                )
                services.runServer()
                val servicesClient = services.clientSide
                val compResultsClient = kotlinCompilerClient.createCompResults().clientSide
                val threadCount = 10
                @OptIn(ObsoleteCoroutinesApi::class)
                fun runThread(i: Int) =
                    async(newSingleThreadContext("thread_$i")) {
                        val jar = tmpdir.absolutePath + File.separator + "hello.$i.jar"
                        val code =
                            daemon.compile(
                                CompileService.NO_SESSION,
                                arrayOf(
                                    "-include-runtime",
                                    File(KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp", "hello.kt").absolutePath,
                                    "-d",
                                    jar
                                ),
                                CompilationOptions(
                                        CompilerMode.NON_INCREMENTAL_COMPILER,
                                        CompileService.TargetPlatform.JVM,
                                        arrayOf(
                                                ReportCategory.COMPILER_MESSAGE.code,
                                                ReportCategory.DAEMON_MESSAGE.code,
                                                ReportCategory.EXCEPTION.code,
                                                ReportCategory.OUTPUT_MESSAGE.code
                                    ),
                                        ReportSeverity.INFO.code,
                                        emptyArray()
                                ),
                                servicesClient,
                                compResultsClient
                            ).get()
                        codes[i] = code
                    }
                (0 until threadCount).map(::runThread).map { it.await() }
                assertTrue("not-null code", codes.all { it == 0 })
            }
        }
    }

}
