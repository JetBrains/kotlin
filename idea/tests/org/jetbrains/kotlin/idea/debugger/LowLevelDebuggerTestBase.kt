/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.util.PathUtil
import com.intellij.util.SystemProperties
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine
import com.sun.tools.jdi.SocketAttachingConnector
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File
import java.io.IOException
import java.net.Socket
import java.nio.file.Files
import kotlin.properties.Delegates
import org.jetbrains.org.objectweb.asm.Type as AsmType

abstract class LowLevelDebuggerTestBase : CodegenTestCase() {
    private companion object {
        private const val DEBUG_ADDRESS = "127.0.0.1"
        private const val DEBUG_PORT = 5115
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)

        val options = wholeFile.readLines()
            .asSequence()
            .filter { it.matches("^// ?[\\w_]+(:.*)?$".toRegex()) }
            .map { it.drop(2).trim() }
            .filter { !it.startsWith("FILE:") }
            .toSet()

        val skipLoadingClasses = skipLoadingClasses(options)

        loadMultiFiles(files)
        val classBuilderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.FULL)
        val generationState = GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)
        classFileFactory = generationState.factory

        val tempDirForTest = Files.createTempDirectory("debuggerTest").toFile()

        try {
            val classesDir = File(tempDirForTest, "classes").apply {
                writeMainClass(this)
                for (classFile in classFileFactory.getClassFiles()) {
                    File(this, classFile.relativePath).mkdirAndWriteBytes(classFile.asByteArray())
                }
            }

            val process = startDebuggeeProcess(classesDir, skipLoadingClasses)
            waitUntil { isPortOpen() }

            val virtualMachine = attachDebugger()

            try {
                val mainThread = virtualMachine.allThreads().single { it.name() == "main" }
                waitUntil { areCompiledClassesLoaded(mainThread, classFileFactory, skipLoadingClasses) }
                doTest(options, mainThread, classBuilderFactory, classFileFactory, generationState)
            } finally {
                virtualMachine.exit(0)
                process.destroy()
            }
        } finally {
            tempDirForTest.deleteRecursively()
        }
    }

    protected abstract fun doTest(
        options: Set<String>,
        mainThread: ThreadReference,
        factory: OriginCollectingClassBuilderFactory,
        classFileFactory: ClassFileFactory,
        state: GenerationState
    )

    private fun isPortOpen(): Boolean {
        return try {
            Socket(DEBUG_ADDRESS, DEBUG_PORT).close()
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun areCompiledClassesLoaded(
        mainThread: ThreadReference,
        classFileFactory: ClassFileFactory,
        skipLoadingClasses: Set<String>
    ): Boolean {

        for (outputFile in classFileFactory.getClassFiles()) {
            val fqName = outputFile.internalName.replace('/', '.')
            if (fqName in skipLoadingClasses) {
                continue
            }

            mainThread.virtualMachine().classesByName(fqName).firstOrNull() ?: return false
        }
        return true
    }

    protected open fun skipLoadingClasses(options: Set<String>): Set<String> {
        return emptySet()
    }

    private fun startDebuggeeProcess(classesDir: File, skipLoadingClasses: Set<String>): Process {
        val classesToLoad = this.classFileFactory.getClassFiles()
            .map { it.qualifiedName }
            .filter { it !in skipLoadingClasses }
            .joinToString(",")

        val classpath = listOf(
            classesDir.absolutePath,
            PathUtil.getJarPathForClass(Delegates::class.java) // Add Kotlin runtime JAR
        )

        val command = arrayOf(
            findJavaExecutable().absolutePath,
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT",
            "-ea",
            "-classpath", classpath.joinToString(File.pathSeparator),
            "-D${DebuggerMain.CLASSES_TO_LOAD}=$classesToLoad",
            DebuggerMain::class.java.name
        )

        return ProcessBuilder(*command).inheritIO().start()
    }

    private fun attachDebugger(): VirtualMachine {
        val connector = SocketAttachingConnector()
        return connector.attach(connector.defaultArguments().toMutableMap().apply {
            getValue("port").setValue("$DEBUG_PORT")
            getValue("hostname").setValue(DEBUG_ADDRESS)
        })
    }

    private fun findJavaExecutable(): File {
        val javaBin = File(SystemProperties.getJavaHome(), "bin")
        return File(javaBin, "java.exe").takeIf { it.exists() }
            ?: File(javaBin, "java").also { assert(it.exists()) }
    }

    private fun writeMainClass(classesDir: File) {
        val mainClassResourceName = DebuggerMain::class.java.name.replace('.', '/') + ".class"
        val mainClassBytes = javaClass.classLoader.getResource(mainClassResourceName).readBytes()
        File(classesDir, mainClassResourceName).mkdirAndWriteBytes(mainClassBytes)
    }

    internal val OutputFile.internalName
        get() = relativePath.substringBeforeLast(".class")

    internal val OutputFile.qualifiedName
        get() = internalName.replace('/', '.')
}

private fun File.mkdirAndWriteBytes(array: ByteArray) {
    parentFile.mkdirs()
    writeBytes(array)
}

private fun waitUntil(condition: () -> Boolean) {
    while (!condition()) {
        Thread.sleep(30)
    }
}

private object DebuggerMain {
    const val CLASSES_TO_LOAD = "classes.to.load"

    @JvmField
    val lock = Any()

    @JvmStatic
    fun main(args: Array<String>) {
        System.getProperty(CLASSES_TO_LOAD).split(',').forEach { Class.forName(it) }
        synchronized(lock) {
            // Wait until debugger is attached
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (lock as java.lang.Object).wait()
        }
    }
}