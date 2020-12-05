/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental.integration

import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.captureOut
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerDaemonClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.loggerCompatiblePath
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.test.IgnoreAll
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.resetApplicationToNull
import org.junit.Assert
import org.junit.runner.RunWith
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.io.path.*

private val logFiles = arrayListOf<String>()

// TODO: remove ignore annotation from tests.

@OptIn(ExperimentalPathApi::class)
@RunWith(IgnoreAll::class)
class CompilerApiTest : KotlinIntegrationTestBase() {

    val kotlinCompilerClient = KotlinCompilerDaemonClient
        .instantiate(DaemonProtocolVariant.SOCKETS) // TODO(SOCKETS)

    private val compilerLibDir = getCompilerLib()

    private fun createNewLogFile(): Path {
        println("creating logFile")
        val newLogFile = createTempFile("kotlin-daemon-experimental-test.", ".log")
        println("logFile created (${newLogFile.loggerCompatiblePath})")
        logFiles.add(newLogFile.loggerCompatiblePath)
        return newLogFile
    }

    private val currentLogFile: Path by lazy {
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
        newLogFile
    }

    private val externalLogFile: Path by lazy { createNewLogFile() }

    private val log by lazy {
        currentLogFile
        Logger.getLogger("test")
    }

    val compilerClassPath = listOf(
        File(compilerLibDir, "kotlin-compiler.jar"),
        File(compilerLibDir, "kotlin-daemon.jar")
    )
    val scriptRuntimeClassPath = listOf(
        File(compilerLibDir, "kotlin-runtime.jar"),
        File(compilerLibDir, "kotlin-script-runtime.jar")
    )
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private fun compileLocally(
        messageCollector: TestMessageCollector,
        vararg args: String
    ): Pair<Int, Collection<OutputMessageUtil.Output>> {
        val application = ApplicationManager.getApplication()
        try {
            val code = K2JVMCompiler().exec(messageCollector,
                                            Services.EMPTY,
                                            K2JVMCompilerArguments().apply { K2JVMCompiler().parseArguments(args, this) }).code
            val outputs = messageCollector.messages.filter { it.severity == CompilerMessageSeverity.OUTPUT }.mapNotNull {
                OutputMessageUtil.parseOutputMessage(it.message)?.let { outs ->
                    outs.outputFile?.let { OutputMessageUtil.Output(outs.sourceFiles, it) }
                }
            }
            return code to outputs
        } finally {
            resetApplicationToNull(application)
        }
    }

    private fun compileOnDaemon(
        clientAliveFile: File,
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        messageCollector: MessageCollector,
        vararg args: String
    ): Pair<Int, Collection<OutputMessageUtil.Output>> = runBlocking {

        log.info("kotlinCompilerClient.connectToCompileService() call")
        val daemon = kotlinCompilerClient.connectToCompileService(
            compilerId,
            clientAliveFile,
            daemonJVMOptions,
            daemonOptions,
            DaemonReportingTargets(messageCollector = messageCollector),
            autostart = true
        )
        log.info("kotlinCompilerClient.connectToCompileService() called! (daemon = $daemon)")

        assertNotNull("failed to connect daemon", daemon)

        log.info("runBlocking { ")
        log.info("register client...")
        daemon?.registerClient(clientAliveFile.absolutePath)
        log.info("   client registered")
        log.info("} ^ runBlocking")


        val outputs = arrayListOf<OutputMessageUtil.Output>()

        val code = kotlinCompilerClient.compile(
            daemon!!,
            CompileService.NO_SESSION,
            CompileService.TargetPlatform.JVM,
            args,
            messageCollector,
            { outFile, srcFiles -> outputs.add(OutputMessageUtil.Output(srcFiles, outFile)) },
            reportSeverity = ReportSeverity.DEBUG
        )
        code to outputs
    }

    private fun getHelloAppBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp"
    private fun getSimpleScriptBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/simpleScript"

    private fun run(baseDir: String, logName: String, vararg args: String): Int = runJava(baseDir, logName, *args)

    private fun runScriptWithArgs(
        testDataDir: String,
        logName: String?,
        scriptClassName: String,
        classpath: List<File>,
        vararg arguments: String
    ) {

        val cl = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray())
        val scriptClass = cl.loadClass(scriptClassName)

        val scriptOut = captureOut { scriptClass.constructors.first().newInstance(arguments) }

        if (logName != null) {
            val expectedFile = File(testDataDir, logName + ".expected")
            val normalizedContent = normalizeOutput(File(testDataDir), "OUT:\n$scriptOut\nReturn code: 0")

            KotlinTestUtils.assertEqualsToFile(expectedFile, normalizedContent)
        }
    }

    fun ignore_testHelloAppLocal() {
        val messageCollector = TestMessageCollector()
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"
        val (code, outputs) = compileLocally(
            messageCollector, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath,
            "-d", jar, "-Xreport-output-files"
        )
        Assert.assertEquals(0, code)
        Assert.assertTrue(outputs.isNotEmpty())
        Assert.assertEquals(jar, outputs.first().outputFile?.absolutePath)
        run(getHelloAppBaseDir(), "hello.run", "-cp", jar, "Hello.HelloKt")
    }

    private fun terminate(@Suppress("UNUSED_PARAMETER") daemonOptions: DaemonOptions) {
        println("\n\nkillall -9 Console && open ${logFiles.joinToString(" ")}\n\n")
        log.info("in finally")
//        runBlocking {
//            log.info("in runBlocking")
//            delay(1000L)
//            kotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
//        }
//        currentLogFile.delete()
//        externalLogFile.delete()
    }

    fun ignore_testHelloApp() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            log.info("sarting test...")

            log.info("assigning daemonOptions")
            val daemonOptions = DaemonOptions(
                runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                verbose = true,
                reportPerf = true
            )
            log.info("daemonOptions assigned")

            log.info("creating daemonJVMOptions")
            val daemonJVMOptions = configureDaemonJVMOptions(
                "D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${externalLogFile.loggerCompatiblePath}\"",
                inheritMemoryLimits = false,
                inheritOtherJvmOptions = false,
                inheritAdditionalProperties = false
            )
            log.info("daemonJVMOptions created")

            log.info("creating jar")
            val jar = tmpdir.absolutePath + File.separator + "hello.jar"
            log.info("jar created")

            try {
                log.info("compileOnDaemon call")
                val (code, outputs) = compileOnDaemon(
                    flagFile,
                    compilerId,
                    daemonJVMOptions,
                    daemonOptions,
                    TestMessageCollector(),
                    "-include-runtime",
                    File(getHelloAppBaseDir(), "hello.kt").absolutePath,
                    "-d",
                    jar,
                    "-Xreport-output-files"
                )
                log.info("compileOnDaemon called")

                Assert.assertEquals(0, code)
                Assert.assertTrue(outputs.isNotEmpty())
                Assert.assertEquals(jar, outputs.first().outputFile?.absolutePath)
                run(getHelloAppBaseDir(), "hello.run", "-cp", jar, "Hello.HelloKt")
            } finally {
                terminate(daemonOptions)
            }
            println("test passed")
        }
    }

    fun ignore_testSimpleScriptLocal() {
        val messageCollector = TestMessageCollector()
        val (code, outputs) = compileLocally(
            messageCollector,
            File(getSimpleScriptBaseDir(), "script.kts").absolutePath,
            "-d",
            tmpdir.absolutePath,
            "-Xreport-output-files"
        )
        Assert.assertEquals(0, code)
        Assert.assertTrue(outputs.isNotEmpty())
        Assert.assertEquals(File(tmpdir, "Script.class").absolutePath, outputs.first().outputFile?.absolutePath)
        runScriptWithArgs(getSimpleScriptBaseDir(), "script", "Script", scriptRuntimeClassPath + tmpdir, "hi", "there")
    }

    fun ignore_testSimpleScript() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(
                runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                verbose = true,
                reportPerf = true
            )
            val daemonJVMOptions = configureDaemonJVMOptions(
                "D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${externalLogFile.loggerCompatiblePath}\"",
                inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false
            )
            try {
                val (code, outputs) = compileOnDaemon(
                    flagFile,
                    compilerId,
                    daemonJVMOptions,
                    daemonOptions,
                    TestMessageCollector(),
                    File(getSimpleScriptBaseDir(), "script.kts").absolutePath,
                    "-Xreport-output-files",
                    "-d",
                    tmpdir.absolutePath
                )
                Assert.assertEquals(0, code)
                Assert.assertTrue(outputs.isNotEmpty())
                Assert.assertEquals(File(tmpdir, "Script.class").absolutePath, outputs.first().outputFile?.absolutePath)
                runScriptWithArgs(getSimpleScriptBaseDir(), "script", "Script", scriptRuntimeClassPath + tmpdir, "hi", "there")
            } finally {
                terminate(daemonOptions)
            }
        }
    }

}

class TestMessageCollector : MessageCollector {
    data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageSourceLocation?)

    val messages = arrayListOf<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        messages.add(Message(severity, message, location))
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR }

    override fun toString(): String {
        return messages.joinToString("\n") { "${it.severity}: ${it.message}${it.location?.let { " at $it" } ?: ""}" }
    }
}

fun TestMessageCollector.assertHasMessage(msg: String, desiredSeverity: CompilerMessageSeverity? = null) {
    assert(messages.any { it.message.contains(msg) && (desiredSeverity == null || it.severity == desiredSeverity) }) {
        "Expecting message \"$msg\" with severity ${desiredSeverity?.toString() ?: "Any"}, actual:\n" +
                messages.joinToString("\n") { it.severity.toString() + ": " + it.message }
    }
}

