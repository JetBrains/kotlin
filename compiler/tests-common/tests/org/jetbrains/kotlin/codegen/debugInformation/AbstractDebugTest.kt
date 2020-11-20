/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.debugInformation

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.PathUtil
import com.intellij.util.SystemProperties
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.*
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.EventRequest.SUSPEND_ALL
import com.sun.jdi.request.StepRequest
import com.sun.tools.jdi.SocketAttachingConnector
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.clientserver.TestProcessServer
import org.jetbrains.kotlin.test.clientserver.TestProxy
import org.jetbrains.kotlin.test.clientserver.getGeneratedClass
import org.junit.After
import org.junit.Before
import java.io.File
import java.lang.IllegalStateException
import java.net.URLClassLoader
import kotlin.properties.Delegates

data class ProcessAndPort(val process: Process, val port: Int)

abstract class AbstractDebugTest : CodegenTestCase() {

    abstract val virtualMachine: VirtualMachine
    abstract val proxyPort: Int

    companion object {
        const val DEBUG_ADDRESS = "127.0.0.1"
        const val MAIN_CLASS = "org.jetbrains.kotlin.test.clientserver.TestProcessServer"
        const val TEST_CLASS = "TestKt"
        const val BOX_METHOD = "box"

        fun setUpVM(virtualMachine: VirtualMachine) {
            val manager = virtualMachine.eventRequestManager()

            val methodEntryReq = manager.createMethodEntryRequest()
            methodEntryReq.addClassFilter(TEST_CLASS)
            methodEntryReq.setSuspendPolicy(EventRequest.SUSPEND_ALL)
            methodEntryReq.enable()

            val methodExitReq = manager.createMethodExitRequest()
            methodExitReq.addClassFilter(TEST_CLASS)
            methodExitReq.setSuspendPolicy(EventRequest.SUSPEND_ALL)
            methodExitReq.enable()
        }

        fun getProxyPort(process: Process): Int {
            val reader = process.inputStream.bufferedReader()
            reader.readLine()
            val proxyPort = reader.readLine()
                .split("port ")
                .last()
                .trim()
                .toInt()
            reader.close()
            return proxyPort
        }

        fun startDebuggeeProcess(): ProcessAndPort {
            val classpath = listOf(
                PathUtil.getJarPathForClass(TestProcessServer::class.java),
                PathUtil.getJarPathForClass(Delegates::class.java) // Add Kotlin runtime JAR
            )

            val javaExecutablePath = findJavaExecutable().absolutePath
            val command = arrayOf(
                if (SystemInfo.isWindows) "\"$javaExecutablePath\"" else javaExecutablePath,
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:0",
                "-ea",
                "-classpath", classpath.joinToString(File.pathSeparator),
                MAIN_CLASS,
                TestProcessServer.DEBUG_TEST
            )

            val process = ProcessBuilder(*command).start()
            val port = process.inputStream.bufferedReader().readLine()
                .split("address:")
                .last()
                .trim()
                .toInt()
            return ProcessAndPort(process, port)
        }

        fun attachDebugger(port: Int): VirtualMachine {
            val connector = SocketAttachingConnector()
            return connector.attach(connector.defaultArguments().toMutableMap().apply {
                getValue("port").setValue("$port")
                getValue("hostname").setValue(DEBUG_ADDRESS)
            })
        }

        fun findJavaExecutable(): File {
            val javaBin = File(SystemProperties.getJavaHome(), "bin")
            return File(javaBin, "java.exe").takeIf { it.exists() }
                ?: File(javaBin, "java").also { assert(it.exists()) }
        }

    }

    @Before
    public override fun setUp() {
        super.setUp()
    }

    @After
    public override fun tearDown() {
        super.tearDown()
    }

    internal fun invokeBoxInSeparateProcess(classLoader: URLClassLoader, aClass: Class<*>, port: Int): String {

        val classPath = classLoader.extractUrls().toMutableList()
        if (classLoader is GeneratedClassLoader) {
            val outDir = KotlinTestUtils.tmpDirForTest(this)
            val currentOutput = SimpleOutputFileCollection(classLoader.allGeneratedFiles)
            currentOutput.writeAllTo(outDir)
            classPath.add(0, outDir.toURI().toURL())
        }

        return TestProxy(Integer.valueOf(port), aClass.canonicalName, classPath).runTestNoOutput()
    }

    internal fun createGeneratedClassLoader(classesDir: File): URLClassLoader {
        return URLClassLoader(
            listOf(classesDir.toURI().toURL()).toTypedArray(),
            ForTestCompileRuntime.runtimeJarClassLoader()
        )
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(
            ConfigurationKind.ALL, *listOfNotNull(
                writeJavaFiles(
                    files
                )
            ).toTypedArray()
        )

        loadMultiFiles(files)
        val classBuilderFactory =
            OriginCollectingClassBuilderFactory(ClassBuilderMode.FULL)
        val generationState =
            GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)
        classFileFactory = generationState.factory

        val tempDirForTest = KotlinTestUtils.tmpDir("debuggerTest")
        val classesDir = File(tempDirForTest, "classes")
        try {
            classFileFactory.writeAllTo(classesDir)
            doTest(wholeFile, classesDir)
        } finally {
            tempDirForTest.deleteRecursively()
        }
    }

    protected open fun doTest(wholeFile: File, classesDir: File) {

        val classLoader = createGeneratedClassLoader(classesDir)
        val aClass = getGeneratedClass(classLoader, TEST_CLASS)
        assert(aClass.declaredMethods.any { it.name == BOX_METHOD }) {
            "Test method $BOX_METHOD not present on test class $TEST_CLASS"
        }
        if (virtualMachine.allThreads().any { it.isSuspended }) {
            virtualMachine.resume()
        }
        invokeBoxInSeparateProcess(classLoader, aClass, proxyPort)

        val manager = virtualMachine.eventRequestManager()

        val loggedItems = ArrayList<Any>()
        var inBoxMethod = false
        vmLoop@
        while (true) {
            val eventSet = virtualMachine.eventQueue().remove(1000) ?: continue
            for (event in eventSet) {
                when (event) {
                    is VMDeathEvent, is VMDisconnectEvent -> {
                        break@vmLoop
                    }
                    // We start VM with option 'suspend=n', in case VMStartEvent is still received, discard.
                    is VMStartEvent -> {

                    }
                    is MethodEntryEvent -> {
                        if (!inBoxMethod && event.location().method().name() == BOX_METHOD) {
                            if (manager.stepRequests().isEmpty()) {
                                // Create line stepping request to get all normal line steps starting now.
                                val stepReq = manager.createStepRequest(event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO)
                                stepReq.setSuspendPolicy(SUSPEND_ALL)
                                stepReq.addClassExclusionFilter("java.*")
                                stepReq.addClassExclusionFilter("sun.*")
                                stepReq.addClassExclusionFilter("kotlin.*")
                                // Create class prepare request to be able to set breakpoints on class initializer lines.
                                // There are no line stepping events for class initializers, so we depend on breakpoints.
                                val prepareReq = manager.createClassPrepareRequest()
                                prepareReq.setSuspendPolicy(SUSPEND_ALL)
                                prepareReq.addClassExclusionFilter("java.*")
                                prepareReq.addClassExclusionFilter("sun.*")
                                prepareReq.addClassExclusionFilter("kotlin.*")
                            }
                            manager.stepRequests().map { it.enable() }
                            manager.classPrepareRequests().map { it.enable() }
                            inBoxMethod = true
                            storeStep(loggedItems, event)
                        }
                    }
                    is StepEvent -> {
                        // Handle the case where an Exception causing program to exit without MethodExitEvent.
                        if (inBoxMethod && event.location().method().name() == "run") {
                            manager.stepRequests().map { it.disable() }
                            manager.classPrepareRequests().map { it.disable() }
                            manager.breakpointRequests().map { it.disable() }
                            break@vmLoop
                        }
                        if (inBoxMethod) {
                            storeStep(loggedItems, event)
                        }
                    }
                    is MethodExitEvent -> {
                        if (event.location().method().name() == BOX_METHOD) {
                            manager.stepRequests().map { it.disable() }
                            manager.classPrepareRequests().map { it.disable() }
                            manager.breakpointRequests().map { it.disable() }
                            break@vmLoop
                        }
                    }
                    is ClassPrepareEvent -> {
                        if (inBoxMethod) {
                            val initializer = event.referenceType().methods().find { it.isStaticInitializer }
                            try {
                                initializer?.allLineLocations()?.forEach {
                                    manager.createBreakpointRequest(it).enable()
                                }
                            } catch (e: AbsentInformationException) {
                                // If there is no line information, do not set breakpoints.
                            }
                        }
                    }
                    is BreakpointEvent -> {
                        if (inBoxMethod) {
                            storeStep(loggedItems, event)
                        }
                    }
                    else -> {
                        throw IllegalStateException("event not handled: $event")
                    }
                }
            }
            eventSet.resume()
        }
        virtualMachine.resume()
        checkResult(wholeFile, loggedItems)
    }

    fun Location.formatAsExpectation(): String {
        val synthetic = if (method().isSynthetic) " (synthetic)" else ""
        return "${sourceName()}:${lineNumber()} ${method().name()}$synthetic"
    }

    /*
       Compresses runs of the same, linenumber-less location in the log:
       specifically removes locations without linenumber, that would otherwise
       print as byte offsets. This avoids overspecifying code generation
       strategy in debug tests.
     */
    fun <T> compressRunsWithoutLinenumber(loggedItems: List<T>, getLocation: (T) -> Location): List<T> {
        if (loggedItems.isEmpty()) return listOf()

        val logIterator = loggedItems.iterator()
        var currentItem = logIterator.next()
        val result = mutableListOf(currentItem)

        for (logItem in logIterator) {
            if (getLocation(currentItem).lineNumber() != -1 || getLocation(currentItem).formatAsExpectation() != getLocation(logItem).formatAsExpectation()) {
                result.add(logItem)
                currentItem = logItem
            }
        }

        return result
    }

    abstract fun storeStep(loggedItems: ArrayList<Any>, event: Event)

    abstract fun checkResult(wholeFile: File, loggedItems: List<Any>)
}