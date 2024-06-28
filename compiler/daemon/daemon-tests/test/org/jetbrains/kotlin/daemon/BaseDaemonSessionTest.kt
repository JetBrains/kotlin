/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.KotlinPaths
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Use this class as the base for tests that lease daemon sessions.
 *
 * It provides you with the [leaseSession] method that leases a session and registers it, so at the end of the test's execution
 * the daemon will be shut down.
 *
 * [LastSessionDaemonTest] is an example of such a test suit.
 */
abstract class BaseDaemonSessionTest {
    @TempDir
    lateinit var workingDirectory: File

    private val compilerClassPath = KotlinIntegrationTestBase.getKotlinPaths().classPath(KotlinPaths.ClassPaths.Compiler)

    private val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private val compileServices = mutableSetOf<CompileService>()

    val outputDirectory
        get() = workingDirectory.resolve("output")

    open val defaultDaemonOptions
        get() = DaemonOptions(
            File(workingDirectory, "daemon-files").absolutePath,
            shutdownDelayMilliseconds = 0,
            autoshutdownUnusedSeconds = 1,
            autoshutdownIdleSeconds = 1,
        )

    open val defaultDaemonJvmOptions
        get() = DaemonJVMOptions(
            maxMemory = "384m"
        )

    @AfterEach
    fun stopDaemons() {
        for (compileService in compileServices) {
            runCatching { compileService.shutdown() }
        }
        Thread.sleep(500) // wait a bit so that all the daemons are shut down
    }

    fun getHelloAppBaseDir(): String = KtTestUtil.getTestDataPathBase() + "/integration/smoke/helloApp"

    private fun DaemonJVMOptions.withLogFile(logFile: File) = copy(
        jvmParams = (jvmParams + "D${CompilerSystemProperties.COMPILE_DAEMON_LOG_PATH_PROPERTY.property}=\"${logFile.loggerCompatiblePath}\"").toMutableList()
    )

    fun leaseSession(
        clientMarkerFile: File = workingDirectory.resolve("client.alive"),
        sessionMarkerFile: File = workingDirectory.resolve("session.alive"),
        jvmOptions: DaemonJVMOptions = defaultDaemonJvmOptions,
        daemonOptions: DaemonOptions = defaultDaemonOptions,
        logFile: File? = null,
        daemonMessagesCollector: MutableCollection<DaemonReportMessage>? = null,
    ): CompileServiceSession {
        val actualJvmOptions = logFile?.let { jvmOptions.withLogFile(it) } ?: jvmOptions
        println("Leasing a session with $actualJvmOptions and $daemonOptions")
        clientMarkerFile.createNewFile()
        sessionMarkerFile.createNewFile()
        logFile?.createNewFile()
        return KotlinCompilerClient.connectAndLease(
            compilerId,
            clientMarkerFile,
            actualJvmOptions,
            daemonOptions,
            DaemonReportingTargets(messages = daemonMessagesCollector, out = System.err),
            autostart = true,
            leaseSession = true,
            sessionAliveFlagFile = sessionMarkerFile,
        )?.also { compileServices.add(it.compileService) } ?: error("failed to connect daemon")
    }
}

fun File.assertLogFileContains(vararg substrings: String) {
    val text = readText()
    val notFound = substrings.filterNot { it in text }
    assert(notFound.isEmpty()) {
        """
        |$this does not contain the following substrings:
        |${notFound.joinToString(System.lineSeparator())}
        |
        |The whole file content:
        |$text
        """.trimMargin()
    }
}