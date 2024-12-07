/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.Thread.sleep
import kotlin.test.assertEquals

@DisplayName("Compiler daemon operations test in the LastSession mode")
class LastSessionDaemonTest : BaseDaemonSessionTest() {
    private val logFile
        get() = workingDirectory.resolve("daemon.log")

    override val defaultDaemonOptions: DaemonOptions
        get() = super.defaultDaemonOptions.copy(autoshutdownUnusedSeconds = (DAEMON_PERIODIC_CHECK_INTERVAL_MS / 1000).toInt())

    @DisplayName("Already leased session can perform compilation")
    @Test
    fun canCompileInLastSessionMode() {
        val (compileService, sessionId) = leaseSession(logFile = logFile)
        sleep(DAEMON_PERIODIC_CHECK_INTERVAL_MS + 1_000)
        logFile.assertLogFileContains("Some sessions are active, waiting for them to finish")
        val testMessageCollector = MessageCollectorImpl()
        val exitCode = KotlinCompilerClient.compile(
            compileService,
            sessionId,
            CompileService.TargetPlatform.JVM,
            arrayOf(
                K2JVMCompilerArguments::includeRuntime.cliArgument,
                File(getHelloAppBaseDir(), "hello.kt").absolutePath,
                K2JVMCompilerArguments::destination.cliArgument,
                outputDirectory.absolutePath
            ),
            testMessageCollector
        )
        assertEquals(0, exitCode)
    }

    @DisplayName("can lease a session when the daemon in the LastSession state") // either by starting a new daemon or returning it to the Alive state
    @Test
    fun canLeaseNewSession() {
        leaseSession(logFile = logFile)
        sleep(DAEMON_PERIODIC_CHECK_INTERVAL_MS + 1_000)
        logFile.assertLogFileContains("Some sessions are active, waiting for them to finish")
        // trying to lease a session with the same config again
        leaseSession(logFile = logFile)
    }
}