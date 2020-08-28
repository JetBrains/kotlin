/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.CompileServiceImplBase
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.KotlinCompileDaemonBase
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.CompileServiceServerSide
import org.jetbrains.kotlin.daemon.common.experimental.findPortForSocket
import java.util.*
import kotlin.concurrent.schedule

object KotlinCompileDaemon : KotlinCompileDaemonBase() {
    override fun <T> runSynchronized(block: () -> T) = runBlocking { block() }

    override fun getCompileServiceAndPort(
        compilerSelector: CompilerSelector,
        compilerId: CompilerId,
        daemonOptions: DaemonOptions,
        daemonJVMOptions: DaemonJVMOptions,
        timer: Timer
    ) = run {
        val port = findPortForSocket(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            COMPILE_DAEMON_PORTS_RANGE_START,
            COMPILE_DAEMON_PORTS_RANGE_END
        )
        val compilerService = CompileServiceServerSideImpl(
            port,
            compilerSelector,
            compilerId,
            daemonOptions,
            daemonJVMOptions,
            port.port,
            timer,
            {
                if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
                    // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
                    timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                        cancel()
                        log.info("force JVM shutdown")
                        System.exit(0)
                    }
                } else {
                    timer.cancel()
                }
            })
        Pair(compilerService, port.port)
    }

    override fun runCompileService(compileService: CompileServiceImplBase) = (compileService as CompileServiceServerSide).runServer()
    override fun awaitServerRun(serverRun: Any?) {
        runBlocking {
            @Suppress("UNCHECKED_CAST")
            (serverRun as Deferred<Unit>?)?.await()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        mainImpl(args)
    }
}