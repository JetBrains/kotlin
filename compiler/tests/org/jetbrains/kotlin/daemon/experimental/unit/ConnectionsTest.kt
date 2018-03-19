/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.unit

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.daemon.CompileServiceImpl
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.DaemonWithMetadataAsync
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket
import org.jetbrains.kotlin.daemon.common.experimental.walkDaemonsAsync
import org.jetbrains.kotlin.daemon.experimental.CompileServiceServerSideImpl
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import java.io.File
import java.util.*
import java.util.logging.Logger
import kotlin.concurrent.schedule

class ConnectionsTest : KotlinIntegrationTestBase() {

    init {
//        val logFile = createTempFile("/Users/jetbrains/Documents/kotlin/my_fork/kotlin", ".txt")
//        println("log file path : ${logFile.loggerCompatiblePath}")
//        val cfg: String =
//            "handlers = java.util.logging.FileHandler\n" +
//                    "java.util.logging.FileHandler.level     = ALL\n" +
//                    "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
//                    "java.util.logging.FileHandler.encoding  = UTF-8\n" +
//                    "java.util.logging.FileHandler.limit     = 0\n" + // if file is provided - disabled, else - 1Mb
//                    "java.util.logging.FileHandler.count     = 1\n" +
//                    "java.util.logging.FileHandler.append    = true\n" +
//                    "java.util.logging.FileHandler.pattern   = ${logFile.loggerCompatiblePath}\n" +
//                    "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s%n\n"
//        LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
    }

    private val log by lazy { Logger.getLogger("ConnectionsTest") }

    private val daemonJVMOptions
        get() = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )

    private val compilerId get() = CompilerId()

    private val daemonOptions get() = DaemonOptions()

    private val port by lazy {
        findPortForSocket(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            COMPILE_DAEMON_PORTS_RANGE_START,
            COMPILE_DAEMON_PORTS_RANGE_END
        )
    }

    private val timer = Timer(true)

    private val runFile: File
        get() {
            val runFileDir = File(daemonOptions.runFilesPathOrDefault)
            runFileDir.mkdirs()
            return File(
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


    private fun getNewDaemonsOrAsyncWrappers() = runBlocking(Unconfined) {
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

    private fun getOldDaemonsOrRMIWrappers() = runBlocking(Unconfined) {
        walkDaemons(
            File(daemonOptions.runFilesPathOrDefault),
            compilerId,
            runFile,
            filter = { _, _ -> true },
            report = { _, msg -> log.info(msg) }
        ).toList()
    }

    private fun runNewServer(): Deferred<Unit> =
        CompileServiceServerSideImpl(
            port,
            compilerId,
            daemonOptions,
            daemonJVMOptions,
            port,
            timer,
            onShutdown
        ).let {
            log.info("service created")
            it.runServer()
        }

    private fun runOldServer() {
        val (registry, port) = findPortAndCreateRegistry(
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
        CompileServiceImpl(
            registry = registry,
            compiler = compilerSelector,
            compilerId = compilerId,
            daemonOptions = daemonOptions,
            daemonJVMOptions = daemonJVMOptions,
            port = port,
            timer = timer,
            onShutdown = onShutdown
        )
    }

    val comparator = compareByDescending<DaemonWithMetadataAsync, DaemonJVMOptions>(
        DaemonJVMOptionsMemoryComparator(),
        { it.jvmOptions }
    )
        .thenBy(FileAgeComparator()) { it.runFile }
        .thenBy { it.daemon.serverPort }

    fun testConnectionMEchanism_OldClient_OldServer() {
        runOldServer()
        val daemons = getOldDaemonsOrRMIWrappers()
        log.info("daemons : $daemons")
        assert(daemons.isNotEmpty())
        val daemon = daemons[0].daemon
        println("chosen : $daemon")
        val info = runBlocking(Unconfined) { daemon.getDaemonInfo() }
        log.info("info : $info")
        assert(info.isGood)
        println("test passed")
    }


    fun testConnectionMechanism_NewClient_NewServer() {
        val runService = runNewServer()
        val daemons = getNewDaemonsOrAsyncWrappers()
        log.info("daemons : $daemons")
        assert(daemons.isNotEmpty())
        val daemon = daemons.maxWith(comparator)!!.daemon
        println("chosen : $daemon")
        val info = runBlocking(Unconfined) { daemon.getDaemonInfo() }
        log.info("info : $info")
        assert(info.isGood)
        println("test passed")
    }

    fun testConnectionMechanism_OldClient_NewServer() {
        val runService = runNewServer()
        val daemons = getOldDaemonsOrRMIWrappers()
        log.info("daemons : $daemons")
        assert(daemons.isNotEmpty())
        val daemon = daemons[0].daemon
        println("chosen : $daemon")
        val info = runBlocking(Unconfined) { daemon.getDaemonInfo() }
        log.info("info : $info")
        assert(info.isGood)
        println("test passed")
    }

    fun testConnectionMechanism_NewClient_OldServer() {
        runOldServer()
        val daemons = getNewDaemonsOrAsyncWrappers()
        log.info("daemons : $daemons")
        assert(daemons.isNotEmpty())
        val daemon = daemons.maxWith(comparator)!!.daemon
        println("chosen : $daemon")
        val info = runBlocking(Unconfined) { daemon.getDaemonInfo() }
        log.info("info : $info")
        assert(info.isGood)
        println("test passed")
    }

}