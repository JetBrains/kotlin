/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.unit

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.walkDaemonsAsync
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket
import org.jetbrains.kotlin.daemon.experimental.CompileServiceServerSideImpl
import org.jetbrains.kotlin.daemon.experimental.KotlinCompileDaemon
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import java.awt.SystemColor.info
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

class ConnectionsTest : KotlinIntegrationTestBase() {
    fun testConnectionMechanism() {
        val daemonJVMOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = true,
            inheritAdditionalProperties = true
        )
        val compilerId = CompilerId()
        val daemonOptions = DaemonOptions()
        val port = findPortForSocket(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            COMPILE_DAEMON_PORTS_RANGE_START,
            COMPILE_DAEMON_PORTS_RANGE_END
        )
        // timer with a daemon thread, meaning it should not prevent JVM to exit normally
        val timer = Timer(true)
        val compilerService = CompileServiceServerSideImpl(
            port,
            compilerId,
            daemonOptions,
            daemonJVMOptions,
            port,
            timer,
            {
                if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
                    // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
                    timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                        cancel()
                        KotlinCompileDaemon.log.info("force JVM shutdown")
                        System.exit(0)
                    }
                } else {
                    timer.cancel()
                }
            })
        compilerService.runServer()
        println("service started")
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        val runFile = File(
            runFileDir,
            makeRunFilenameString(
                timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                port = port.toString()
            )
        )
        val daemons = runBlocking(Unconfined) {
            walkDaemonsAsync(
                File(daemonOptions.runFilesPathOrDefault),
                compilerId,
                runFile,
                filter = { _, _ -> true },
                report = { _, msg -> println("[report] : " + msg) }
            ).toList()
        }
        println("daemons : $daemons")
        assert(daemons.isNotEmpty())
        val daemon = daemons[0].daemon
        val info = runBlocking(Unconfined) { daemon.getDaemonInfo() }
        println("info : $info")
        assert(info.isGood)
    }
}