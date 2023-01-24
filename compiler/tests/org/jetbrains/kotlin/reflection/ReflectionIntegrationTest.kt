/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.reflection

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.reflect.KClass

class ReflectionIntegrationTest : KtUsefulTestCase() {
    // This test checks that we use the correct class loader to load .kotlin_builtins resource files.
    // On Android, the class loader used to load the application classes differs from the boot class loader (which loads core JDK classes),
    // so attempting to find kotlin/kotlin.kotlin_builtins in the same class loader that found java.lang.Object can fail.
    // We should always use the class loader that loads stdlib class files to locate .kotlin_builtins resource files
    fun testClassLoaderForBuiltIns() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)

        val root = KtTestUtil.getTestDataPathBase() + "/reflection/classLoaderForBuiltIns"
        KotlinTestUtils.compileJavaFiles(
            listOf(File("$root/Main.java")),
            listOf("-d", tmpdir.absolutePath)
        )

        val lib = CompilerTestUtil.compileJvmLibrary(File("$root/test.kt"))

        runJava(
            "-ea",
            "-classpath",
            tmpdir.absolutePath,
            "Main",
            lib.absolutePath,
            ForTestCompileRuntime.runtimeJarForTests().absolutePath,
            ForTestCompileRuntime.reflectJarForTests().absolutePath,
        )
    }

    // This test checks that simultaneous access to kotlin-reflect from different threads works, in case the URLClassLoader instance is
    // being closed in one of the threads. It creates two threads that for several seconds continuously load kotlin-reflect in a new
    // class loader, call something from it, and close the class loader.
    fun testParallelAccess() {
        val urls = arrayOf(
            ForTestCompileRuntime.reflectJarForTests().toURI().toURL(),
            ForTestCompileRuntime.runtimeJarForTests().toURI().toURL(),
        )

        val latch = CountDownLatch(1)
        val error = AtomicReference<Throwable?>()
        repeat(2) {
            thread {
                while (latch.count == 1L) {
                    try {
                        val classLoader = URLClassLoader(urls, null)

                        // Invoke KClass.primaryConstructor on String::class reflectively.
                        // Among other things, it leads to reading a resource kotlin/kotlin.kotlin_builtins from the JAR file.
                        val getPrimaryConstructor = classLoader.loadClass("kotlin.reflect.full.KClasses")
                            .getDeclaredMethod("getPrimaryConstructor", classLoader.loadClass(KClass::class.java.name))
                        val createKClass = classLoader.loadClass("kotlin.jvm.internal.Reflection")
                            .getDeclaredMethod("getOrCreateKotlinClass", Class::class.java)
                        getPrimaryConstructor(null, createKClass(null, String::class.java))

                        // Close URLClassLoader to verify that it won't affect reading the class loader resources from the other thread(s).
                        classLoader.close()
                    } catch (e: Throwable) {
                        error.set(e)
                        latch.countDown()
                    }
                }
            }
        }

        latch.await(5L, TimeUnit.SECONDS)
        latch.countDown()

        error.get()?.let { throw it }
    }

    fun testConcurrentAccessToPropertyDelegate() {
        compileAndRunProgram(KtTestUtil.getTestDataPathBase() + "/reflection/concurrentAccessToPropertyDelegate")
    }

    fun testConcurrentAccessToPrivateFunction() {
        compileAndRunProgram(KtTestUtil.getTestDataPathBase() + "/reflection/concurrentAccessToPrivateFunction")
    }

    private fun compileAndRunProgram(root: String) {
        val lib = CompilerTestUtil.compileJvmLibrary(File("$root/test.kt"))

        runJava(
            "-ea",
            "-classpath",
            listOf(
                ForTestCompileRuntime.runtimeJarForTests().absolutePath,
                ForTestCompileRuntime.reflectJarForTests().absolutePath,
                lib.absolutePath,
            ).joinToString(File.pathSeparator),
            "TestKt",
        )
    }

    private fun runJava(vararg args: String) {
        val javaHome = System.getProperty("java.home")
        val javaExe = File(javaHome, "bin/java.exe").takeIf(File::exists)
            ?: File(javaHome, "bin/java").takeIf(File::exists)
            ?: error("Can't find 'java' executable in $javaHome")

        val process = ProcessBuilder(javaExe.absolutePath, *args).start()
        process.waitFor(1, TimeUnit.MINUTES)
        val stderr = process.errorStream.reader().readText()
        val stdout = process.inputStream.reader().readText()
        val exitCode = process.exitValue()
        assertEquals("Program exited with exit code $exitCode.\nStdout:\n$stdout\nStderr:\n$stderr", 0, exitCode)
    }
}
