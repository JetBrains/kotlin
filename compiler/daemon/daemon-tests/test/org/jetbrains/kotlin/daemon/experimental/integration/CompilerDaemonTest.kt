/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.integration

import junit.framework.TestCase
import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.daemon.*
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClientInstance
import org.jetbrains.kotlin.daemon.client.KotlinCompilerDaemonClient
import org.jetbrains.kotlin.daemon.client.experimental.CompilerCallbackServicesFacadeServerServerSide
import org.jetbrains.kotlin.daemon.client.experimental.KotlinRemoteReplCompilerClientAsync
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.findCallbackServerSocket
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.progress.experimental.CompilationCanceledStatus
import org.jetbrains.kotlin.test.IgnoreAll
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.KotlinPaths
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.net.URL
import java.net.URLClassLoader
import java.nio.channels.ClosedChannelException
import java.nio.charset.Charset
import java.rmi.ConnectException
import java.rmi.ConnectIOException
import java.rmi.UnmarshalException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.LogManager
import kotlin.concurrent.thread
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.test.fail

val TIMEOUT_DAEMON_RUNNER_EXIT_MS = 10000L

// TODO: remove ignore annotation from tests.

@RunWith(IgnoreAll::class)
class CompilerDaemonTest : KotlinIntegrationTestBase() {

    val kotlinCompilerClientInstance = KotlinCompilerDaemonClient.instantiate(DaemonProtocolVariant.SOCKETS)

    private fun createNewLogFile(): File {
        println("creating logFile")
        val newLogFile = createTempFile("kotlin-daemon-experimental-test.", ".log")
        println("logFile created (${newLogFile.loggerCompatiblePath})")
        return newLogFile
    }

    init {
        val newLogFile = createNewLogFile()
        val cfg: String =
            "handlers = java.util.logging.FileHandler\n" +
                    "java.util.logging.FileHandler.level     = ALL\n" +
                    "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
                    "java.util.logging.FileHandler.encoding  = UTF-8\n" +
                    "java.util.logging.FileHandler.limit     = 0\n" + // if file is provided - disabled, else - 1Mb
                    "java.util.logging.FileHandler.count     = 1\n" +
                    "java.util.logging.FileHandler.append    = true\n" +
                    "java.util.logging.FileHandler.pattern   = ${newLogFile.loggerCompatiblePath}\n" +
                    "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s%n\n"
        LogManager.getLogManager().readConfiguration(cfg.byteInputStream())
    }

    data class CompilerResults(val resultCode: Int, val out: String)

    val compilerClassPath = getKotlinPaths().classPath(KotlinPaths.ClassPaths.Compiler)

    val scriptingCompilerClassPath = listOf(
        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-scripting-compiler.jar"),
        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-scripting-common.jar"),
        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-scripting-jvm.jar")
    )

    val daemonClientClassPath = listOf(
        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-daemon-client.jar"),
        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar")
    )

    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    val compilerWithScriptingId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(compilerClassPath + scriptingCompilerClassPath)
    }

    private fun compileOnDaemon(
        clientAliveFile: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        vararg args: String
    ): Deferred<CompilerResults> = GlobalScope.async {
        val daemon = kotlinCompilerClientInstance.connectToCompileService(
            compilerId,
            clientAliveFile,
            daemonJVMOptions,
            daemonOptions,
            DaemonReportingTargets(out = System.err),
            autostart = true
        )
        assertNotNull("failed to connect daemon", daemon)
        daemon?.registerClient(clientAliveFile.absolutePath)
        val strm = ByteArrayOutputStream()
        val code = kotlinCompilerClientInstance.compile(
            daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM,
            args, PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true),
            reportSeverity = ReportSeverity.DEBUG
        )
        CompilerResults(code, strm.toString())
    }

    private fun runDaemonCompilerTwice(
        clientAliveFile: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        vararg args: String
    ) {
        runBlocking {
            val res1 = compileOnDaemon(clientAliveFile, compilerId, daemonJVMOptions, daemonOptions, *args).await()
            assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)
            val res2 = compileOnDaemon(clientAliveFile, compilerId, daemonJVMOptions, daemonOptions, *args).await()
            assertEquals("second compilation failed:\n${res2.out}", 0, res2.resultCode)
            assertEquals(
                "build results differ",
                AbstractCliTest.removePerfOutput(res1.out).split('\n').toSortedSet(),
                AbstractCliTest.removePerfOutput(res2.out).split('\n').toSortedSet()
            )
        }
    }

    private fun getTestBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)
    private fun getHelloAppBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp"

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)

    fun makeTestDaemonOptions(testName: String, shutdownDelay: Int = 5000) =
        DaemonOptions(
            runFilesPath = File(tmpdir, testName).absolutePath,
            shutdownDelayMilliseconds = shutdownDelay.toLong(),
            verbose = true,
            reportPerf = true
        )

    fun makeTestDaemonJvmOptions(logFile: File? = null, xmx: Int = 384, args: Iterable<String> = listOf()): DaemonJVMOptions {
        val additionalArgs = arrayListOf<String>()
        if (logFile != null) {
            additionalArgs.add("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"")
        }
        args.forEach { additionalArgs.add(it) }
        val baseOpts = if (xmx > 0) DaemonJVMOptions(maxMemory = "${xmx}m") else DaemonJVMOptions()
        return configureDaemonJVMOptions(
            baseOpts,
            *additionalArgs.toTypedArray(),
            inheritMemoryLimits = xmx <= 0,
            inheritAdditionalProperties = false,
            inheritOtherJvmOptions = false
        )
    }

    fun ignore_testHelloApp() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = makeTestDaemonOptions(getTestName(true))

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    var daemonShotDown = false

                    try {
                        val jar = tmpdir.absolutePath + File.separator + "hello.jar"
                        runDaemonCompilerTwice(
                            flagFile, compilerId, daemonJVMOptions, daemonOptions,
                            "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar
                        )
                        kotlinCompilerClientInstance.shutdownCompileService(compilerId, daemonOptions)
                        delay(100)
                        daemonShotDown = true
                        var compileTime1 = 0L
                        var compileTime2 = 0L
                        logFile.assertLogContainsSequence(
                            LinePattern("Kotlin compiler daemon version"),
                            LinePattern("Starting compilation with args: "),
                            LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime1 = it }; true }),
                            LinePattern("Starting compilation with args: "),
                            LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime2 = it }; true }),
                            LinePattern("Shutdown started")
                        )
                        assertTrue(
                            "Expecting that compilation 1 ($compileTime1 ms) is at least two times longer than compilation 2 ($compileTime2 ms)",
                            compileTime1 > compileTime2 * 2
                        )
                        logFile.delete()
                        run("hello.run", "-cp", jar, "Hello.HelloKt")
                    } finally {
                        if (!daemonShotDown)
                            kotlinCompilerClientInstance.shutdownCompileService(compilerId, daemonOptions)
                    }
                }
            }
        }
    }

    fun ignore_testDaemonJvmOptionsParsing() {
        val backupJvmOptions = System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
        try {
            System.setProperty(
                COMPILE_DAEMON_JVM_OPTIONS_PROPERTY,
                "-aaa,-bbb\\,ccc,-ddd,-Xmx200m,-XX:MaxMetaspaceSize=10k,-XX:ReservedCodeCacheSize=100,-xxx\\,yyy"
            )
            val opts =
                configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false, inheritOtherJvmOptions = false)
            assertEquals("200m", opts.maxMemory)
            assertEquals("10k", opts.maxMetaspaceSize)
            assertEquals("100", opts.reservedCodeCacheSize)
            assertEquals(arrayListOf("aaa", "bbb,ccc", "ddd", "xxx,yyy", "ea"), opts.jvmParams)

            System.setProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, "-Xmx300m,-XX:MaxMetaspaceSize=10k,-XX:ReservedCodeCacheSize=100")
            val opts2 =
                configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false, inheritOtherJvmOptions = false)
            assertEquals("300m", opts2.maxMemory)
            assertEquals(-1, DaemonJVMOptionsMemoryComparator().compare(opts, opts2))
            assertEquals("300m", listOf(opts, opts2).maxWithOrNull(DaemonJVMOptionsMemoryComparator())?.maxMemory)

            val myXmxParam = ManagementFactory.getRuntimeMXBean().inputArguments.first { it.startsWith("-Xmx") }
            TestCase.assertNotNull(myXmxParam)
            val myXmxVal = myXmxParam.substring(4)
            System.clearProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
            val opts3 = configureDaemonJVMOptions(
                inheritMemoryLimits = true,
                inheritOtherJvmOptions = true,
                inheritAdditionalProperties = false
            )
            assertEquals(myXmxVal, opts3.maxMemory)
        } finally {
            restoreSystemProperty(
                COMPILE_DAEMON_JVM_OPTIONS_PROPERTY,
                backupJvmOptions
            )
        }
    }

    fun ignore_testDaemonAssertsOptions() {
        val allAssetionsArgs = setOf(
            "-ea", "-enableassertions",
            "-da", "-disableassertions",
            "-esa", "-enablesystemassertions",
            "-dsa", "-disablesystemassertions"
        )

        fun assertionsJvmArgs() = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).mappers.flatMap { it.toArgs("-") }.filter { it in allAssetionsArgs }.joinToString(", ")

        for (assertArgValue in allAssetionsArgs) {
            withDaemonJvmOptionsSetTo(assertArgValue) {
                assertEquals(assertArgValue, assertionsJvmArgs())
            }
        }

        withDaemonJvmOptionsSetTo(null) {
            assertEquals("-ea", assertionsJvmArgs())
        }
    }

    private fun withDaemonJvmOptionsSetTo(newValue: String?, fn: () -> Unit) {
        val backup = getAndSetSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, newValue)

        try {
            fn()
        } finally {
            getAndSetSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, backup)
        }
    }

    private fun getAndSetSystemProperty(property: String, newValue: String?): String? {
        val oldValue = System.getProperty(property)

        if (newValue != null) {
            System.setProperty(property, newValue)
        } else {
            System.clearProperty(property)
        }

        return oldValue
    }

    fun ignore_testDaemonOptionsParsing() {
        val backupOptions = System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, "runFilesPath=abcd,autoshutdownIdleSeconds=1111")
            val opts = configureDaemonOptions(DaemonOptions(shutdownDelayMilliseconds = 1))
            assertEquals("abcd", opts.runFilesPath)
            assertEquals(1111, opts.autoshutdownIdleSeconds)
        } finally {
            restoreSystemProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, backupOptions)
        }
    }

    fun ignore_testDaemonInstancesSimple() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = makeTestDaemonOptions(getTestName(true))
                val compilerId2 = CompilerId.makeCompilerId(
                    compilerClassPath +
                            File(getCompilerLib(), "kotlin-compiler-sources.jar")
                )

                withLogFile("kotlin-daemon1-test") { logFile1 ->
                    withLogFile("kotlin-daemon2-test") { logFile2 ->
                        val daemonJVMOptions1 = makeTestDaemonJvmOptions(logFile1)
                        val daemonJVMOptions2 = makeTestDaemonJvmOptions(logFile2)

                        assertTrue(logFile1.length() == 0L && logFile2.length() == 0L)

                        val jar1 = tmpdir.absolutePath + File.separator + "hello1.jar"
                        val res1 = compileOnDaemon(
                            flagFile,
                            compilerId,
                            daemonJVMOptions1,
                            daemonOptions,
                            "-include-runtime",
                            File(getHelloAppBaseDir(), "hello.kt").absolutePath,
                            "-d",
                            jar1
                        ).await()
                        assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)

                        logFile1.assertLogContainsSequence("Starting compilation with args: ")
                        assertEquals("expecting '${logFile2.absolutePath}' to be empty", 0L, logFile2.length())

                        val jar2 = tmpdir.absolutePath + File.separator + "hello2.jar"
                        val res2 = compileOnDaemon(
                            flagFile,
                            compilerId2,
                            daemonJVMOptions2,
                            daemonOptions,
                            "-include-runtime",
                            File(getHelloAppBaseDir(), "hello.kt").absolutePath,
                            "-d",
                            jar2
                        ).await()
                        assertEquals("second compilation failed:\n${res2.out}", 0, res1.resultCode)

                        logFile2.assertLogContainsSequence("Starting compilation with args: ")

                        kotlinCompilerClientInstance.shutdownCompileService(compilerId, daemonOptions)
                        kotlinCompilerClientInstance.shutdownCompileService(compilerId2, daemonOptions)

                        delay(100)

                        logFile1.assertLogContainsSequence("Shutdown started")
                        logFile2.assertLogContainsSequence("Shutdown started")
                    }
                }
            }
        }
    }

    fun ignore_testDaemonRunError() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions =
                    DaemonOptions(
                        shutdownDelayMilliseconds = 1,
                        verbose = true,
                        runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                    )

                val daemonJVMOptions = configureDaemonJVMOptions(
                    "-abracadabra",
                    inheritMemoryLimits = false,
                    inheritOtherJvmOptions = false,
                    inheritAdditionalProperties = false
                )

                val messageCollector = TestMessageCollector()

                val daemon = kotlinCompilerClientInstance.connectToCompileService(
                    compilerId, flagFile, daemonJVMOptions, daemonOptions,
                    DaemonReportingTargets(messageCollector = messageCollector), autostart = true
                )

                assertNull(daemon)

                messageCollector.assertHasMessage("Unrecognized option: --abracadabra")
            }
        }
    }

    // TODO: find out how to reliably cause the retry
    fun ignore_testDaemonStartRetry() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions =
                    DaemonOptions(
                        shutdownDelayMilliseconds = 1,
                        verbose = true,
                        runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                    )

                val daemonJVMOptions =
                    configureDaemonJVMOptions(
                        inheritMemoryLimits = false,
                        inheritOtherJvmOptions = false,
                        inheritAdditionalProperties = false
                    )

                val messageCollector = TestMessageCollector()

                val daemon = kotlinCompilerClientInstance.connectToCompileService(
                    compilerId, flagFile, daemonJVMOptions, daemonOptions,
                    DaemonReportingTargets(messageCollector = messageCollector), autostart = true
                )

                assertNull(daemon)

                messageCollector.assertHasMessage("retrying(0) on:")
                messageCollector.assertHasMessage("retrying(1) on:")
                // TODO: messageCollector.assertHasNoMessage("retrying(2) on:")
                messageCollector.assertHasMessage("no more retries on:")
            }
        }
    }

    fun ignore_testDaemonAutoshutdownOnUnused() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = DaemonOptions(
                    autoshutdownUnusedSeconds = 1,
                    shutdownDelayMilliseconds = 1,
                    runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                )

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.registerClient(flagFile.absolutePath)

                    // wait up to 4s (more than 1s unused timeout)
                    for (attempts in 1..20) {
                        if (logFile.isLogContainsSequence("Unused timeout exceeded 1s")) break
                        delay(200)
                    }
                    delay(200)

                    logFile.assertLogContainsSequence(
                        "Unused timeout exceeded 1s",
                        "Shutdown started"
                    )
                }
            }
        }
    }

    fun ignore_testDaemonAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = DaemonOptions(
                    autoshutdownIdleSeconds = 1,
                    shutdownDelayMilliseconds = 1,
                    runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                )

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.registerClient(flagFile.absolutePath)
                    val jar = tmpdir.absolutePath + File.separator + "hello1.jar"
                    val strm = ByteArrayOutputStream()
                    val code = kotlinCompilerClientInstance.compile(
                        daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM,
                        arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                        PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true),
                        reportSeverity = ReportSeverity.DEBUG
                    )
                    assertEquals("compilation failed:\n$strm", 0, code)

                    logFile.assertLogContainsSequence("Starting compilation with args: ")

                    // wait up to 4s (more than 1s idle timeout)
                    for (attempts in 1..20) {
                        if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                        delay(200)
                    }
                    delay(200)
                    logFile.assertLogContainsSequence(
                        "Idle timeout exceeded 1s",
                        "Shutdown started"
                    )
                }
            }
        }
    }

    fun ignore_testDaemonGracefulShutdown() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = DaemonOptions(
                    autoshutdownIdleSeconds = 1,
                    shutdownDelayMilliseconds = 1,
                    forceShutdownTimeoutMilliseconds = 60000,
                    runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                )

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.registerClient(flagFile.absolutePath)
                    val sessionId = daemon?.leaseCompileSession(null)
                    val scheduleShutdownRes = daemon?.scheduleShutdown(true)

                    assertTrue(
                        "failed to schedule shutdown ($scheduleShutdownRes)",
                        scheduleShutdownRes?.let { it.isGood && it.get() } ?: false)

                    delay(100) // to allow timer task to run in the daemon

                    logFile.assertLogContainsSequence("Some sessions are active, waiting for them to finish")

                    val res = daemon?.leaseCompileSession(null)

                    assertEquals("Invalid state", CompileService.CallResult.Dying(), res)

                    daemon?.releaseCompileSession(sessionId!!.get())

                    delay(100) // allow after session timed action to run

                    logFile.assertLogContainsSequence(
                        "All sessions finished",
                        "Shutdown started"
                    )
                }
            }
        }
    }

    fun ignore_testDaemonExitsOnClientFlagDeletedWithActiveSessions() {
        runBlocking {
            val daemonOptions = DaemonOptions(
                autoshutdownIdleSeconds = 1000,
                shutdownDelayMilliseconds = 1,
                runFilesPath = File(tmpdir, getTestName(true)).absolutePath
            )
            val clientFlag = createTempFile(getTestName(true), "-client.alive")
            val sessionFlag = createTempFile(getTestName(true), "-session.alive")
            try {
                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        clientFlag,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.leaseCompileSession(sessionFlag.canonicalPath)

                    clientFlag.delete()

                    delay(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o
                    // TODO: consider possibility to set DAEMON_PERIODIC_CHECK_INTERVAL_MS from tests, to allow shorter sleeps

                    logFile.assertLogContainsSequence(
                        "No more clients left",
                        "Shutdown started"
                    )
                }
            } finally {
                sessionFlag.delete()
                clientFlag.delete()
            }
        }
    }

    fun ignore_testDaemonExitsOnClientFlagDeletedWithAllSessionsReleased() {
        runBlocking {
            val daemonOptions = DaemonOptions(
                autoshutdownIdleSeconds = 1000,
                shutdownDelayMilliseconds = 1,
                runFilesPath = File(tmpdir, getTestName(true)).absolutePath
            )
            val clientFlag = createTempFile(getTestName(true), "-client.alive")
            val sessionFlag = createTempFile(getTestName(true), "-session.alive")
            try {
                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        clientFlag,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.leaseCompileSession(sessionFlag.canonicalPath)

                    sessionFlag.delete()

                    delay(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o

                    clientFlag.delete()

                    delay(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o

                    logFile.assertLogContainsSequence(
                        "No more clients left",
                        "Shutdown started"
                    )
                }
            } finally {
                sessionFlag.delete()
                clientFlag.delete()
            }
        }
    }

    fun ignore_testDaemonCancelShutdownOnANewClient() {
        runBlocking {
            val daemonOptions = DaemonOptions(
                autoshutdownIdleSeconds = 1000,
                shutdownDelayMilliseconds = 3000,
                runFilesPath = File(tmpdir, getTestName(true)).absolutePath
            )
            val clientFlag = createTempFile(getTestName(true), "-client.alive")
            val clientFlag2 = createTempFile(getTestName(true), "-client.alive")
            try {
                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        clientFlag,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.leaseCompileSession(null)

                    clientFlag.delete()

                    delay(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o
                    // TODO: consider possibility to set DAEMON_PERIODIC_CHECK_INTERVAL_MS from tests, to allow shorter sleeps

                    val daemon2 = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        clientFlag2,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon2)
                    daemon2?.leaseCompileSession(null)

                    delay(3000) // allow to trigger delayed shutdown timer

                    logFile.assertLogContainsSequence(
                        "No more clients left",
                        "Delayed shutdown in 3000ms",
                        "Cancel delayed shutdown due to a new activity"
                    )
                }
            } finally {
                clientFlag.delete()
                clientFlag2.delete()
            }
        }
    }

    /** Testing that running daemon in the child process doesn't block on s child process.waitFor()
     *  that may happen on windows if simple processBuilder.start is used due to handles inheritance:
     *  - process A starts process B using ProcessBuilder and waits for it using process.waitFor()
     *  - process B starts daemon and exits
     *  - due to default behavior of CreateProcess on windows, the handles of process B are inherited by the daemon
     *    (in particular handles of stdin/out/err) and therefore these handles remain open while daemon is running
     *  - (seems) due to the way how waiting for process is implemented, waitFor() hangs until daemon is killed
     *  This seems a known problem, e.g. gradle uses a library with native code that prevents io handles inheritance when launching it's daemon
     *  (the same solution is used in kotlin daemon client - see next commit)
     */
    fun ignore_testDaemonExecutionViaIntermediateProcess() {
        val clientAliveFile = createTempFile("kotlin-daemon-transitive-run-test", ".run")
        val daemonOptions = makeTestDaemonOptions(getTestName(true))
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"
        val args = listOf(
            File(File(System.getProperty("java.home"), "bin"), "java").absolutePath,
            "-Xmx256m",
            "-D$COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY",
            "-cp",
            daemonClientClassPath.joinToString(File.pathSeparator) { it.absolutePath },
            KotlinCompilerClientInstance::class.qualifiedName!!
        ) +
                daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                File(getHelloAppBaseDir(), "hello.kt").absolutePath +
                "-d" + jar +
                KotlinCompilerClientInstance.SOCKETS_FLAG
        try {
            var resOutput: String? = null
            var resCode: Int? = null
            // running intermediate process (daemon command line controller) that executes the daemon
            val runnerProcess = ProcessBuilder(args).redirectErrorStream(true).start()
            thread {
                resOutput = runnerProcess.inputStream.reader().readText()
            }
            val waitThread = thread {
                resCode = runnerProcess.waitFor()
            }
            waitThread.join(TIMEOUT_DAEMON_RUNNER_EXIT_MS)

            assertFalse("process.waitFor() hangs:\n$resOutput", waitThread.isAlive)
            assertEquals("Compilation failed:\n$resOutput", 0, resCode)
            println("OK")
        } finally {
            if (clientAliveFile.exists())
                clientAliveFile.delete()
        }
    }

    private val PARALLEL_THREADS_TO_COMPILE = 10
    private val PARALLEL_WAIT_TIMEOUT_S = 60L

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun runCompile(
        daemon: CompileServiceAsync,
        resultCodes: Array<Int?>,
        localEndSignal: CountDownLatch,
        outStreams: Array<ByteArrayOutputStream>,
        threadNo: Int
    ) = GlobalScope.async(newSingleThreadContext(name = "tread$threadNo")) {
        println("thread : ${Thread.currentThread().name}")
        val jar = tmpdir.absolutePath + File.separator + "hello.$threadNo.jar"
        val res = kotlinCompilerClientInstance.compile(
            daemon,
            CompileService.NO_SESSION,
            CompileService.TargetPlatform.JVM,
            arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
            PrintingMessageCollector(PrintStream(outStreams[threadNo]), MessageRenderer.WITHOUT_PATHS, true)
        )
        synchronized(resultCodes) {
            resultCodes[threadNo] = res
        }
        localEndSignal.countDown()
    }

    fun ignore_testParallelCompilationOnDaemon() {

        assertTrue(PARALLEL_THREADS_TO_COMPILE <= LoopbackNetworkInterface.SERVER_SOCKET_BACKLOG_SIZE)

        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = makeTestDaemonOptions(getTestName(true))

                withLogFile("kotlin-daemon-test") { logFile ->


                    //                    val cfg: String =
//                        "handlers = java.util.logging.FileHandler\n" +
//                                "java.util.logging.FileHandler.level     = ALL\n" +
//                                "java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter\n" +
//                                "java.util.logging.FileHandler.encoding  = UTF-8\n" +
//                                "java.util.logging.FileHandler.limit     = 0\n" + // if file is provided - disabled, else - 1Mb
//                                "java.util.logging.FileHandler.count     = 1\n" +
//                                "java.util.logging.FileHandler.append    = true\n" +
//                                "java.util.logging.FileHandler.pattern   = ${logFile.loggerCompatiblePath}\n" +
//                                "java.util.logging.SimpleFormatter.format = %1\$tF %1\$tT.%1\$tL [%3\$s] %4\$s: %5\$s%n\n"
//                    LogManager.getLogManager().readConfiguration(cfg.byteInputStream())


                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile, xmx = -1)

                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    TestCase.assertTrue("daemon is not new!", daemon !is CompileService)

                    val resultCodes = arrayOfNulls<Int>(PARALLEL_THREADS_TO_COMPILE)
                    val localEndSignal = CountDownLatch(PARALLEL_THREADS_TO_COMPILE)
                    val outStreams = Array(PARALLEL_THREADS_TO_COMPILE, { ByteArrayOutputStream() })

                    val time = nowSeconds()

                    (1..PARALLEL_THREADS_TO_COMPILE).forEach {
                        runCompile(
                            daemon!!,
                            resultCodes,
                            localEndSignal,
                            outStreams,
                            it - 1
                        )
                    }

                    val succeeded = localEndSignal.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)

                    println("finished in ${nowSeconds() - time} seconds")

                    assertTrue(
                        "parallel compilation failed to complete in $PARALLEL_WAIT_TIMEOUT_S s, ${localEndSignal.count} unfinished threads",
                        succeeded
                    )

                    (1..PARALLEL_THREADS_TO_COMPILE).forEach {
                        assertEquals("Compilation on thread $it failed:\n${outStreams[it - 1]}", 0, resultCodes[it - 1])
                        println("result[$it] = ${resultCodes[it - 1]}")
                    }
                }
            }
        }
    }

    private object ParallelStartParams {
        const val threads = 10
        const val performCompilation = false
        const val connectionFailedErr = -100
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    fun ignore_testParallelDaemonStart() {

        val doneLatch = CountDownLatch(ParallelStartParams.threads)

        val resultCodes = arrayOfNulls<Int>(ParallelStartParams.threads)
        val outStreams = Array(ParallelStartParams.threads, { ByteArrayOutputStream() })
        val logFiles = arrayOfNulls<File>(ParallelStartParams.threads)
        val daemonInfos = arrayOfNulls<Pair<CompileService.CallResult<String>?, Int?>>(ParallelStartParams.threads)

        val daemonOptions = makeTestDaemonOptions(getTestName(true), 100)

        fun connectThread(threadNo: Int) = GlobalScope.async(newSingleThreadContext(name = "daemonConnect$threadNo")) {
            // (name = "daemonConnect$threadNo")
            try {
                withFlagFile(getTestName(true), ".alive") { flagFile ->
                    withLogFile(
                        "kotlin-daemon-test",
                        printLogOnException = false
                    ) { logFile ->
                        logFiles[threadNo] = logFile
                        val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
//                        println("-1      I'm working in thread ${Thread.currentThread().name}")
                        val compileServiceSession =
                            kotlinCompilerClientInstance.connectAndLease(
                                compilerId, flagFile, daemonJVMOptions, daemonOptions,
                                DaemonReportingTargets(out = PrintStream(outStreams[threadNo])), autostart = true,
                                leaseSession = true
                            )
//                        println("0      I'm working in thread ${Thread.currentThread().name}")
                        daemonInfos[threadNo] = try {
                            compileServiceSession?.compileService?.getDaemonInfo()
                        } catch (e: ClosedChannelException) {
                            null
                        } to compileServiceSession?.sessionId

                        resultCodes[threadNo] = when {
                            compileServiceSession?.compileService == null -> {
                                println("[$threadNo] not-compile!")
                                ParallelStartParams.connectionFailedErr
                            }
                            ParallelStartParams.performCompilation -> {
                                println("[$threadNo] compile!")
                                val jar = tmpdir.absolutePath + File.separator + "hello.$threadNo.jar"
                                kotlinCompilerClientInstance.compile(
                                    compileServiceSession.compileService,
                                    compileServiceSession.sessionId,
                                    CompileService.TargetPlatform.JVM,
                                    arrayOf(File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                                    PrintingMessageCollector(PrintStream(outStreams[threadNo]), MessageRenderer.WITHOUT_PATHS, true)
                                )
                            }
                            else -> {
                                println("[$threadNo] compilation skipped, assuming - successful!")
                                0 // compilation skipped, assuming - successful
                            }
                        }
                    }
                }
            } finally {
                doneLatch.countDown()
            }

        }

        runBlocking {
            System.setProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY, "true")
            System.setProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY, "100000")

            val succeeded = try {
                (1..ParallelStartParams.threads).forEach { connectThread(it - 1) }
                doneLatch.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)
            } finally {
                System.clearProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY)
                System.clearProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)
            }

            delay(1000) // Wait for processes to finish and close log files

            val electionLogs = arrayOfNulls<String>(ParallelStartParams.threads)
            val port2logs = arrayOfNulls<Pair<Int?, File?>>(ParallelStartParams.threads)

            for (i in 0..(ParallelStartParams.threads - 1)) {
                val logContents = logFiles[i]?.readLines()
                port2logs[i] = logContents?.find { it.contains("daemon is listening on port") }?.split(" ")?.last()?.toIntOrNull() to
                        logFiles[i]
                electionLogs[i] = logContents?.find { it.contains(LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE) }
            }

            val electionsSuccess = electionLogs.any { it != null && (it.contains("lower prio") || it.contains("equal prio")) }
            val resultsFailures = resultCodes.count { it != null && it == 0 }

            if (!succeeded || !electionsSuccess || resultsFailures > 0) {
                val msg = buildString {
                    for (i in 0..ParallelStartParams.threads - 1) {
                        val daemonInfoRes = daemonInfos[i]?.first
                        val daemonInfo = when (daemonInfoRes) {
                            is CompileService.CallResult.Good -> daemonInfoRes.get()
                            is CompileService.CallResult.Dying -> "<dying>"
                            is CompileService.CallResult.Error -> "<error: ${daemonInfoRes.message}>"
                            else -> "?"
                        }
                        val compiledPort: Int? = daemonInfo.trim().split(" ").last().toIntOrNull()
                        appendLine("#$i\tcompiled on $daemonInfo, session ${daemonInfos[i]?.second}, result ${resultCodes[i]}; started daemon on port ${port2logs[i]?.first}, log: ${logFiles[i]?.canonicalPath}")
                        if (resultCodes[i] != 0 || electionLogs[i] == null) {
                            appendLine("--- out $i, result ${resultCodes[i]}:\n${outStreams[i].toByteArray().toString(Charset.defaultCharset())}\n---")
                            compiledPort?.let { port -> port2logs.find { it?.first == port } }?.second?.let { logFile ->
                                appendLine("--- log file ${logFile.name}:\n${logFile.readText()}\n---")
                            }
                                ?: appendLine("--- log not found (port: $compiledPort)")
                        }
                    }
                }
                assertTrue(
                    "parallel daemons start failed to complete in $PARALLEL_WAIT_TIMEOUT_S s, ${doneLatch.count} unfinished threads:\n\n$msg",
                    succeeded
                )
                assertTrue("No daemon elected:\n\n$msg\n--- elections:\n${electionLogs.joinToString("\n")}\n---", electionsSuccess)
                assertTrue("Compilations failed: $resultsFailures of ${ParallelStartParams.threads}:\n\n$msg", resultsFailures > 0)
                println("test passed (elected : $electionsSuccess, resultsFailures : $resultsFailures)")
            }
        }
    }

    fun ignore_testDaemonConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = makeTestDaemonOptions(getTestName(true))
                val daemonJVMOptions = makeTestDaemonJvmOptions()
                val daemon = kotlinCompilerClientInstance.connectToCompileService(
                    compilerId,
                    flagFile,
                    daemonJVMOptions,
                    daemonOptions,
                    DaemonReportingTargets(out = System.err),
                    autostart = true
                )
                assertNotNull("failed to connect daemon", daemon)
                daemon?.registerClient(flagFile.absolutePath)
                kotlinCompilerClientInstance.shutdownCompileService(compilerId, daemonOptions)
                delay(2000L)
                val exception: Exception? = try {
                    daemon!!.getUsedMemory()
                    null
                } catch (e: ConnectException) {
                    e
                } catch (e: UnmarshalException) {
                    e
                } catch (e: ConnectIOException) {
                    e
                } catch (e: IOException) {
                    e
                }

                println("${(exception ?: Exception())::class.java.simpleName} : ${exception?.message}")
                assertNotNull(exception)
            }
        }
    }

    // TODO: fix this test
    fun ignore_testDaemonCallbackConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = makeTestDaemonOptions(getTestName(true))

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                    val daemon: CompileServiceAsync? = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    assertNotNull("failed to connect daemon", daemon)
                    daemon?.registerClient(flagFile.absolutePath)

                    val file = File(tmpdir, "largeKotlinFile.kt")
                    file.writeText(generateLargeKotlinFile(10))
                    val jar = File(tmpdir, "largeKotlinFile.jar").absolutePath

                    val strm = ByteArrayOutputStream()
                    var callbackServices: CompilerCallbackServicesFacadeServerServerSide? = null
                    callbackServices = CompilerCallbackServicesFacadeServerServerSide(
                        compilationCanceledStatus = object : CompilationCanceledStatus {
                            override suspend fun checkCanceled() {
                                thread {
                                    Thread.sleep(10)
                                    callbackServices!!.shutdownServer()
                                }
                            }
                        },
                        messageCollector = PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true),
                        serverSocketWithPort = findCallbackServerSocket()
                    )
                    callbackServices.runServer()
                    PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true)
                    val code = daemon!!.compile(
                        CompileService.NO_SESSION,
                        arrayOf("-include-runtime", file.absolutePath, "-d", jar),
                        CompilationOptions(
                            CompilerMode.JPS_COMPILER,
                            CompileService.TargetPlatform.JVM,
                            arrayOf(
                                ReportCategory.COMPILER_MESSAGE.code,
                                ReportCategory.DAEMON_MESSAGE.code,
                                ReportCategory.EXCEPTION.code,
                                ReportCategory.OUTPUT_MESSAGE.code
                            ),
                            ReportSeverity.DEBUG.code,
                            emptyArray()
                        ),
                        callbackServices.clientSide,
                        kotlinCompilerClientInstance.createCompResults().clientSide
                    ).get()

                    TestCase.assertEquals(0, code)

                    val compilerOutput = strm.toString()
                    assertTrue("Expecting cancellation message in:\n$compilerOutput", compilerOutput.contains("Compilation was canceled"))
                    logFile.assertLogContainsSequence("error communicating with host, assuming compilation canceled")

                }
            }
        }
    }

    fun ignore_testDaemonReplScriptingNotInClasspathError() {
        withDaemon(compilerId) { daemon ->
            var repl: KotlinRemoteReplCompilerClientAsync? = null
            var isErrorThrown = false
            try {
                repl = KotlinRemoteReplCompilerClientAsync(
                    daemon, null, CompileService.TargetPlatform.JVM, emptyArray(), TestMessageCollector(),
                    classpathFromClassloader(), ScriptWithNoParam::class.qualifiedName!!
                )
            } catch (e: Exception) {
                TestCase.assertEquals(
                    "Unable to use scripting/REPL in the daemon, no kotlin-scripting-compiler.jar or its dependencies are found in the compiler classpath",
                    e.message
                )
                isErrorThrown = true
            } finally {
                repl?.dispose()
            }
            TestCase.assertTrue("Expecting exception that kotlin-scripting-plugin is not found in the classpath", isErrorThrown)
        }
    }

    fun ignore_testDaemonReplLocalEvalNoParams() {
        withDaemon(compilerWithScriptingId) { daemon ->
            val repl = KotlinRemoteReplCompilerClientAsync(
                daemon, null, CompileService.TargetPlatform.JVM,
                emptyArray(),
                TestMessageCollector(),
                classpathFromClassloader(),
                ScriptWithNoParam::class.qualifiedName!!
            )
            println("repl = $repl")
            println("sessionId : ${repl.sessionId}")

            val localEvaluator = GenericReplEvaluator(emptyList(), Thread.currentThread().contextClassLoader)

            doReplTestWithLocalEval(repl, localEvaluator)
            repl.dispose()
        }
    }

    // TODO: fix "Front-end Internal error: Failed to analyze declaration Line_1"
    fun ignore_testDaemonReplLocalEvalStandardTemplate() {
        withDaemon(compilerWithScriptingId) { daemon ->
            val repl = KotlinRemoteReplCompilerClientAsync(
                daemon, null, CompileService.TargetPlatform.JVM, emptyArray(),
                TestMessageCollector(),
                classpathFromClassloader(),
                "kotlin.script.templates.standard.ScriptTemplateWithArgs"
            )

            val localEvaluator = GenericReplEvaluator(
                emptyList(), Thread.currentThread().contextClassLoader,
                ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class))
            )

            doReplTestWithLocalEval(repl, localEvaluator)
            repl.dispose()
        }
    }

    fun ignore_testDaemonReplLocalEvalStandardTemplate_OldDaemon_NewClient() {
        withOldDaemon { daemon ->
            val repl = KotlinRemoteReplCompilerClientAsync(
                daemon, null, CompileService.TargetPlatform.JVM, emptyArray(),
                TestMessageCollector(),
                classpathFromClassloader(),
                "kotlin.script.templates.standard.ScriptTemplateWithArgs"
            )

            val localEvaluator = GenericReplEvaluator(
                emptyList(), Thread.currentThread().contextClassLoader,
                ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class))
            )

            doReplTestWithLocalEval(repl, localEvaluator)
            repl.dispose()
        }
    }

    private suspend fun doReplTestWithLocalEval(replCompiler: KotlinRemoteReplCompilerClientAsync, localEvaluator: ReplEvaluator) {
        println("doReplTestWithLocalEval...")
        val compilerState = replCompiler.createState()
        println("compilerState = $compilerState")
        val evaluatorState = localEvaluator.createState()
        println("evaluatorState = $evaluatorState")

        val res0 = replCompiler.check(compilerState, ReplCodeLine(0, 0, "val x ="))
        TestCase.assertTrue("Unexpected check results: $res0", res0 is ReplCheckResult.Incomplete)

        val codeLine1 = ReplCodeLine(1, 0, "val lst = listOf(1)\nval x = 5")
        val res1 = replCompiler.compile(compilerState, codeLine1)
        val res1c = res1 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res1", res1c)

        val res11 = localEvaluator.eval(evaluatorState, res1c!!)
        val res11e = res11 as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $res11", res11e)

        val codeLine2 = ReplCodeLine(2, 0, "x + 2")
        val res2 = replCompiler.compile(compilerState, codeLine2)
        val res2c = res2 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res2", res2c)

        val res21 = localEvaluator.eval(evaluatorState, res2c!!)
        val res21e = res21 as? ReplEvalResult.ValueResult
        TestCase.assertNotNull("Unexpected eval result: $res21", res21e)
        TestCase.assertEquals(7, res21e!!.value)
    }

    fun ignore_testDaemonReplAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            runBlocking {
                val daemonOptions = DaemonOptions(
                    autoshutdownIdleSeconds = 1,
                    autoshutdownUnusedSeconds = 1,
                    shutdownDelayMilliseconds = 1,
                    runFilesPath = File(tmpdir, getTestName(true)).absolutePath
                )

                withLogFile("kotlin-daemon-test") { logFile ->
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                    val daemon = kotlinCompilerClientInstance.connectToCompileService(
                        compilerWithScriptingId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )

                    println("daemon : $daemon")
                    assertNotNull("failed to connect daemon", daemon)

                    val replCompiler = KotlinRemoteReplCompilerClientAsync(
                        daemon!!, null, CompileService.TargetPlatform.JVM,
                        emptyArray(),
                        TestMessageCollector(),
                        classpathFromClassloader(),
                        ScriptWithNoParam::class.qualifiedName!!
                    )

                    val compilerState = replCompiler.createState()

                    // use repl compiler for >> 1s, making sure that idle/unused timeouts are not firing
                    for (attempts in 1..10) {
                        val codeLine1 = ReplCodeLine(attempts, 0, "3 + 5")
                        val res1 = replCompiler.compile(compilerState, codeLine1)
                        val res1c = res1 as? ReplCompileResult.CompiledClasses
                        TestCase.assertNotNull("Unexpected compile result: $res1", res1c)
                        delay(500)
                    }

                    // wait up to 4s (more than 1s idle timeout)
                    for (attempts in 1..20) {
                        if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                        delay(500)
                    }
                    replCompiler.dispose()

                    delay(500)
                    logFile.assertLogContainsSequence(
                        "Idle timeout exceeded 1s",
                        "Shutdown started"
                    )
                }
            }
        }
    }

    internal fun withDaemon(compilerId: CompilerId = this.compilerId, body: suspend (CompileServiceAsync) -> Unit) {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))
            withLogFile("kotlin-daemon-test") { logFile ->
                runBlocking {
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    val daemon: CompileServiceAsync? = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    println("daemon : $daemon")
                    assertNotNull("failed to connect daemon", daemon)
                    assertTrue("daemon is not New", daemon !is CompileServiceAsyncWrapper)

                    body(daemon!!)
                }
            }
        }
    }

    internal fun withOldDaemon(body: suspend (CompileServiceAsync) -> Unit) {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))
            withLogFile("kotlin-daemon-test") { logFile ->
                runBlocking {
                    val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                    runOldServer(daemonOptions, daemonJVMOptions)
                    val daemon: CompileServiceAsync? = kotlinCompilerClientInstance.connectToCompileService(
                        compilerId,
                        flagFile,
                        daemonJVMOptions,
                        daemonOptions,
                        DaemonReportingTargets(out = System.err),
                        autostart = true
                    )
                    println("daemon : $daemon, port : ${daemon?.serverPort}")
                    TestCase.assertTrue(daemon is CompileServiceAsyncWrapper)
                    assertNotNull("failed to connect daemon", daemon)

                    body(daemon!!)
                }
            }
        }
    }

    private fun runOldServer(
        daemonOptions: DaemonOptions,
        daemonJVMOptions: DaemonJVMOptions,
        timer: Timer = Timer(),
        onShutdown: () -> Unit = {}
    ) {
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
        println("old daemon init: port = $serverPort")
    }
}


// stolen from CompilerFileLimitTest
internal fun generateLargeKotlinFile(size: Int): String {
    return buildString {
        append("package large\n\n")
        (0..size).forEach {
            appendLine("class Class$it")
            appendLine("{")
            appendLine("\tfun foo(): Long = $it")
            appendLine("}")
            appendLine("\n")
            repeat(2000) {
                appendLine("// kotlin rules ... and stuff")
            }
        }
        appendLine("fun main(args: Array<String>)")
        appendLine("{")
        appendLine("\tval result = Class5().foo() + Class$size().foo()")
        appendLine("\tprintln(result)")
        appendLine("}")
    }

}


internal fun File.ifLogNotContainsSequence(vararg patterns: String, body: (LinePattern, Int) -> Unit) {
    reader().useLines {
        it.asSequence().ifNotContainsSequence(patterns.map { LinePattern(it) }, body)
    }
}

internal fun File.assertLogContainsSequence(vararg patterns: String) = assertLogContainsSequence(patterns.map { LinePattern(it) })

internal fun File.assertLogContainsSequence(vararg patterns: LinePattern) = assertLogContainsSequence(patterns.asIterable())

internal fun File.assertLogContainsSequence(patterns: Iterable<LinePattern>) {
    val lines = reader().readLines()
    lines.asSequence().ifNotContainsSequence(patterns.iterator())
    { unmatchedPattern, lineNo ->
        fail(
            "pattern not found in the input: ${unmatchedPattern.regex}\nunmatched part of the log file ($absolutePath) from line $lineNo:\n\n${lines.asSequence().drop(
                lineNo
            ).joinToString("\n")}\n-------"
        )
    }
}

internal fun File.isLogContainsSequence(vararg patterns: String): Boolean {
    var res = true
    ifLogNotContainsSequence(*patterns) { _, _ -> res = false }
    return res
}

fun restoreSystemProperty(propertyName: String, backupValue: String?) {
    if (backupValue == null) {
        System.clearProperty(propertyName)
    } else {
        System.setProperty(propertyName, backupValue)
    }
}

internal inline fun withFlagFile(prefix: String, suffix: String? = null, body: (File) -> Unit) {
    val file = createTempFile(prefix, suffix)
    try {
        body(file)
    } finally {
        file.delete()
    }
}

internal inline fun withLogFile(prefix: String, suffix: String = ".log", printLogOnException: Boolean = true, body: (File) -> Unit) {
    val logFile = createTempFile(prefix, suffix)
    println("LOG FILE : ${logFile.path}")
    try {
        body(logFile)
    } catch (e: Exception) {
        if (printLogOnException) {
            Thread.sleep(100) // trying to wait log flushing
            System.err.println("--- log file ${logFile.name}:\n${logFile.readText()}\n---")
        }
        throw e
    }
}

// java.util.Logger used in the daemon silently forgets to log into a file specified in the config on Windows,
// if file path is given in windows form (using backslash as a separator); the reason is unknown
// this function makes a path with forward slashed, that works on windows too
internal val File.loggerCompatiblePath: String
    get() =
        if (OSKind.current == OSKind.Windows) absolutePath.replace('\\', '/')
        else absolutePath

open class TestKotlinScriptDummyDependenciesResolver : DependenciesResolver {

    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        ).asSuccess()
    }
}

@ScriptTemplateDefinition(resolver = TestKotlinScriptDummyDependenciesResolver::class)
abstract class ScriptWithNoParam

internal fun classpathFromClassloader(): List<File> {
    val additionalClasspath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
        ?.map { File(it) }.orEmpty()
    return ((TestKotlinScriptDummyDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
        ?.mapNotNull(URL::toFile)
        ?.filter { it.path.contains("out") && it.path.contains("") }
        ?: emptyList()
            ) + additionalClasspath
}

internal fun URL.toFile() =
    try {
        File(toURI().schemeSpecificPart)
    } catch (e: java.net.URISyntaxException) {
        if (protocol != "file") null
        else File(file)
    }

