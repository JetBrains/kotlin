/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.reflection

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import java.util.concurrent.TimeUnit

class ReflectionIntegrationTest : KtUsefulTestCase() {
    // This test checks that we use the correct class loader to load .kotlin_builtins resource files.
    // On Android, the class loader used to load the application classes differs from the boot class loader (which loads core JDK classes),
    // so attempting to find kotlin/kotlin.kotlin_builtins in the same class loader that found java.lang.Object can fail.
    // We should always use the class loader that loads stdlib class files to locate .kotlin_builtins resource files
    fun testClassLoaderForBuiltIns() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)

        val root = KotlinTestUtils.getTestDataPathBase() + "/reflection/classLoaderForBuiltIns"
        KotlinTestUtils.compileJavaFiles(
            listOf(File("$root/Main.java")),
            listOf("-d", tmpdir.absolutePath)
        )

        val lib = CompilerTestUtil.compileJvmLibrary(File("$root/test.kt"))

        val javaHome = System.getProperty("java.home")
        val javaExe = File(javaHome, "bin/java.exe").takeIf(File::exists)
            ?: File(javaHome, "bin/java").takeIf(File::exists)
            ?: error("Can't find 'java' executable in $javaHome")

        val command = arrayOf(
            javaExe.absolutePath,
            "-ea",
            "-classpath",
            tmpdir.absolutePath,
            "Main",
            lib.absolutePath,
            ForTestCompileRuntime.runtimeJarForTests().absolutePath,
            ForTestCompileRuntime.reflectJarForTests().absolutePath
        )

        val process = ProcessBuilder(*command).inheritIO().start()
        process.waitFor(1, TimeUnit.MINUTES)
        assertEquals(0, process.exitValue())
    }
}
