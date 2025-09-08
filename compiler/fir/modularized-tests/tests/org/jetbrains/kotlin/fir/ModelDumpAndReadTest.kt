/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

class ModelDumpAndReadTest : TestCaseWithTmpdir() {

    fun testModelDump() {
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText("fun main() {}")
        }
        val jarFile = tmpdir.resolve("output.jar")
        val args = listOf(
            "-d", jarFile.absolutePath,
            "-XXdump-model=${tmpdir.absolutePath}",
            "-module-name", "testmain",
            mainKt.absolutePath
        )
        val res = CompilerTestUtil.executeCompiler(K2JVMCompiler(), args)
        assertEquals(ExitCode.OK, res.second)

        val modelDumpFile = tmpdir.resolve("model-testmain.xml")
        val moduleData = loadModuleDumpFile(modelDumpFile).single()

        val arguments = moduleData.arguments as K2JVMCompilerArguments
        assertEquals(jarFile.absolutePath, arguments.destination)
        assertEquals(mainKt.absolutePath, arguments.freeArgs.single())
        assertEquals("testmain", arguments.moduleName)

        assertEquals("testmain", moduleData.name)
        assertEquals(listOf(mainKt), moduleData.sources.toList())
        assertTrue(moduleData.classpath.any { it.name == "kotlin-stdlib.jar" })
    }
}