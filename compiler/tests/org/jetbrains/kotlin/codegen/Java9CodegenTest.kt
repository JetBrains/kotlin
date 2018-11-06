/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.util.concurrent.TimeUnit

// TODO: merge into main codegen box tests somehow
class Java9CodegenTest : AbstractBlackBoxCodegenTest() {
    override fun setUp() {
        super.setUp()
        val fileName = KotlinTestUtils.getTestDataPathBase() + "/codegen/" + getPrefix() + "/" + getTestName(true) + ".kt"
        val testFile = TestFile(fileName, File(fileName).readText())
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.NO_KOTLIN_REFLECT, listOf(testFile), TestJdkKind.FULL_JDK_9)
    }

    override fun getPrefix(): String = "java9/box"

    override fun blackBox(reportFailures: Boolean) {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)
        generateClassesInFile().writeAll(tmpdir, null)

        val jdk9Home = KotlinTestUtils.getJdk9Home()
        val javaExe = File(jdk9Home, "bin/java.exe").takeIf(File::exists)
            ?: File(jdk9Home, "bin/java").takeIf(File::exists)
            ?: error("Can't find 'java' executable in $jdk9Home")

        val command = arrayOf(
            javaExe.absolutePath,
            "-ea",
            "-classpath",
            listOf(tmpdir, ForTestCompileRuntime.runtimeJarForTests()).joinToString(File.pathSeparator, transform = File::getAbsolutePath),
            PackagePartClassUtils.getFilePartShortName(getTestName(false))
        )

        val process = ProcessBuilder(*command).inheritIO().start()
        process.waitFor(1, TimeUnit.MINUTES)
        assertEquals(0, process.exitValue())
    }

    fun testVarHandle() {
        loadFile()
        blackBox(true)
    }
}
