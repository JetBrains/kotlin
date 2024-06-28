/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.resetApplicationToNull
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

@OptIn(ExperimentalPathApi::class)
class CompilerApiTest : KotlinIntegrationTestBase() {

    private val compilerLibDir = getCompilerLib()

    val compilerClassPath = listOf(
            File(compilerLibDir, "kotlin-compiler.jar"),
            File(compilerLibDir, "kotlin-daemon.jar")
    )
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private fun compileLocally(messageCollector: TestMessageCollector, vararg args: String): Pair<Int, Collection<OutputMessageUtil.Output>> {
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
        }
        finally {
            resetApplicationToNull(application)
        }
    }

    private fun compileOnDaemon(clientAliveFile: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions,
                                messageCollector: MessageCollector, vararg args: String): Pair<Int, Collection<OutputMessageUtil.Output>> {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientAliveFile, daemonJVMOptions, daemonOptions,
                                                                  DaemonReportingTargets(messageCollector = messageCollector), autostart = true)
        assertNotNull("failed to connect daemon", daemon)

        daemon?.registerClient(clientAliveFile.absolutePath)

        val outputs = arrayListOf<OutputMessageUtil.Output>()

        val code = KotlinCompilerClient.compile(daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, args, messageCollector,
                                                { outFile, srcFiles -> outputs.add(OutputMessageUtil.Output(srcFiles, outFile)) },
                                                reportSeverity = ReportSeverity.DEBUG)
        return code to outputs
    }

    private fun getHelloAppBaseDir(): String = KtTestUtil.getTestDataPathBase() + "/integration/smoke/helloApp"
    private fun getSimpleScriptBaseDir(): String = KtTestUtil.getTestDataPathBase() + "/integration/smoke/simpleScript"

    private fun run(baseDir: String, logName: String, vararg args: String): Int = runJava(baseDir, logName, *args)

    private fun runScriptWithArgs(testDataDir: String, logName: String?, scriptClassName: String, classpath: List<File>, vararg arguments: String) {

        val cl = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray())
        val scriptClass = cl.loadClass(scriptClassName)

        val scriptOut = captureOut { scriptClass.constructors.first().newInstance(arguments) }

        if (logName != null) {
            val expectedFile = File(testDataDir, logName + ".expected")
            val normalizedContent = normalizeOutput(File(testDataDir), "OUT:\n$scriptOut\nReturn code: 0")

            KotlinTestUtils.assertEqualsToFile(expectedFile, normalizedContent)
        }
    }

    fun testHelloAppLocal() {
        val messageCollector = TestMessageCollector()
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"
        val (code, outputs) = compileLocally(messageCollector, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath,
                                             "-d", jar, "-Xreport-output-files")
        if (code != 0) {
            Assert.fail("Result code: $code\n${messageCollector.messages.joinToString("\n")}")
        }
        Assert.assertTrue(outputs.isNotEmpty())
        Assert.assertEquals(jar, outputs.first().outputFile?.absolutePath)
        run(getHelloAppBaseDir(), "hello.run", "-cp", jar, "Hello.HelloKt")
    }

    fun testHelloApp() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                                              verbose = true,
                                              reportPerf = true)

            val logFile: Path = createTempFile("kotlin-daemon-test.", ".log")

            val daemonJVMOptions = configureDaemonJVMOptions("D${CompilerSystemProperties.COMPILE_DAEMON_LOG_PATH_PROPERTY.property}=\"${logFile.loggerCompatiblePath}\"",
                                                             inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false)
            val jar = tmpdir.absolutePath + File.separator + "hello.jar"

            try {
                val (code, outputs) = compileOnDaemon(
                        flagFile, compilerId, daemonJVMOptions, daemonOptions, TestMessageCollector(), "-include-runtime",
                        File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar, "-Xreport-output-files"
                )
                Assert.assertEquals(0, code)
                Assert.assertTrue(outputs.isNotEmpty())
                Assert.assertEquals(jar, outputs.first().outputFile?.absolutePath)
                run(getHelloAppBaseDir(), "hello.run", "-cp", jar, "Hello.HelloKt")
            }
            finally {
                KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                runCatching { logFile.deleteIfExists() }
                    .onFailure { e -> println("Failed to delete log file: $e") }
            }
        }
    }

    fun testSimpleScriptLocal() {
        val messageCollector = TestMessageCollector()
        val (code, outputs) = compileLocally(
            messageCollector, File(getSimpleScriptBaseDir(), "script.kts").absolutePath,
            "-d", tmpdir.absolutePath, "-Xreport-output-files", "-Xuse-fir-lt=false", "-Xallow-any-scripts-in-source-roots"
        )
        Assert.assertEquals(0, code)
        Assert.assertTrue(outputs.isNotEmpty())
        Assert.assertEquals(File(tmpdir, "Script.class").absolutePath, outputs.first().outputFile?.absolutePath)
        runScriptWithArgs(getSimpleScriptBaseDir(), "script", "Script", listOf(tmpdir), "hi", "there")
    }

    fun testSimpleScript() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                                              verbose = true,
                                              reportPerf = true)

            val logFile: Path = createTempFile("kotlin-daemon-test.", ".log")

            val daemonJVMOptions = configureDaemonJVMOptions("D${CompilerSystemProperties.COMPILE_DAEMON_LOG_PATH_PROPERTY.property}=\"${logFile.loggerCompatiblePath}\"",
                                                             inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false)
            try {
                val (code, outputs) = compileOnDaemon(
                    flagFile, compilerId, daemonJVMOptions, daemonOptions, TestMessageCollector(),
                    File(getSimpleScriptBaseDir(), "script.kts").absolutePath,
                    "-Xreport-output-files", "-Xuse-fir-lt=false", "-Xallow-any-scripts-in-source-roots", "-d", tmpdir.absolutePath
                )
                Assert.assertEquals(0, code)
                Assert.assertTrue(outputs.isNotEmpty())
                Assert.assertEquals(File(tmpdir, "Script.class").absolutePath, outputs.first().outputFile?.absolutePath)
                runScriptWithArgs(getSimpleScriptBaseDir(), "script", "Script", listOf(tmpdir), "hi", "there")
            }
            finally {
                KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                runCatching { logFile.deleteIfExists() }
                    .onFailure { e -> println("Failed to delete log file: $e") }
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

    override fun hasErrors(): Boolean = messages.any { it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR }

    override fun toString(): String {
        return messages.joinToString("\n") { "${it.severity}: ${it.message}${it.location?.let{" at $it"} ?: ""}" }
    }
}

fun TestMessageCollector.assertHasMessage(msg: String, desiredSeverity: CompilerMessageSeverity? = null) {
    assert(messages.any { it.message.contains(msg) && (desiredSeverity == null || it.severity == desiredSeverity) }) {
        "Expecting message \"$msg\" with severity ${desiredSeverity?.toString() ?: "Any"}, actual:\n" +
        messages.joinToString("\n") { it.severity.toString() + ": " + it.message }
    }
}

internal fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}

