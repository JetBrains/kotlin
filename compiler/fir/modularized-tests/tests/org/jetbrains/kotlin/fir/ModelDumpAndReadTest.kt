/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

class ModelDumpAndReadTest : TestCaseWithTmpdir() {

    fun testModelDump() {
        // We don't want to bother with windows specifics yet, model dumping is unix-only now anyway.
        if (SystemInfo.isWindows) return
        val mainKt = tmpdir.resolve("main.kt").apply {
            writeText("fun main() {}")
        }
        val main2Kt = tmpdir.resolve("main2.kt").apply {
            writeText("fun main2() {}")
        }
        val jarFile = tmpdir.resolve("output.jar")
        val args = listOf(
            "-d", jarFile.absolutePath,
            "-XXdump-model=${tmpdir.absolutePath}",
            "-module-name", "testmain",
            mainKt.absolutePath,
            main2Kt.absolutePath,
        )
        val res = CompilerTestUtil.executeCompiler(K2JVMCompiler(), args)
        assertEquals(ExitCode.OK, res.second)

        val modelDumpFile = tmpdir.resolve("model-testmain.xml")
        val moduleData = loadModuleDumpFile(modelDumpFile, ModularizedTestConfig()).single()

        val arguments = moduleData.arguments as K2JVMCompilerArguments
        assertEquals(jarFile.absolutePath, arguments.destination)
        assertEquals(listOf(mainKt.absolutePath, main2Kt.absolutePath), arguments.freeArgs)
        assertEquals("testmain", arguments.moduleName)

        assertEquals("testmain", moduleData.name)
        assertEquals(listOf(mainKt, main2Kt), moduleData.sources.toList())
        assertTrue(moduleData.classpath.any { it.name == "kotlin-stdlib.jar" })
    }

    fun testOldModelFormatDeserialization() {
        // We don't want to bother with windows specifics yet, model dumping is unix-only now anyway.
        if (SystemInfo.isWindows) return
        val src1 = tmpdir.resolve("A.kt").apply { writeText("fun a() {}") }
        val src2 = tmpdir.resolve("A2.kt").apply { writeText("fun a2() {}") }
        val outDir = tmpdir.resolve("out").apply { mkdirs() }
        val dest = tmpdir.resolve("out.jar").absolutePath
        val cp1 = tmpdir.resolve("lib1.jar").absolutePath
        val cp2 = tmpdir.resolve("lib2.jar").absolutePath
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <modules>
              <compilerArguments>
                <K2JVMCompilerArguments>
                  <option name="moduleName" value="m1" />
                  <option name="destination" value="$dest" />
                  <option name="noStdlib" value="true" />
                  <option name="noJdk" value="true" />
                  <option name="noReflect" value="true" />
                  <option name="languageVersion" value="2.0" />
                  <option name="jvmTarget" value="17" />
                  <option name="reportOutputFiles" value="true" />
                  <option name="optIn">
                    <array>
                      <option value="kotlin.ExperimentalStdlibApi" />
                      <option value="kotlin.RequiresOptIn" />
                    </array>
                  </option>
                  <option name="freeArgs">
                    <list>
                      <option value="${src1.absolutePath}" />
                      <option value="${src2.absolutePath}" />
                    </list>
                  </option>
                </K2JVMCompilerArguments>
              </compilerArguments>
              <module timestamp="1" name="m1" type="java-production" outputDir="${outDir.absolutePath}">
                <sources path="${src1.absolutePath}" />
                <sources path="${src2.absolutePath}" />
                <classpath path="$cp1" />
                <classpath path="$cp2" />
                <friendDir path="${outDir.absolutePath}" />
                <javaSourceRoots path="${tmpdir.absolutePath}" packagePrefix="com.example" />
                <modularJdkRoot path="/fake/jdk" />
                <useOptIn annotation="kotlin.ExperimentalStdlibApi" />
              </module>
            </modules>
        """.trimIndent()
        val xmlFile = tmpdir.resolve("model-oldformat-simple.xml").apply { writeText(xml) }
        val modules = loadModuleDumpFile(xmlFile, ModularizedTestConfig())
        assertEquals(1, modules.size)
        val m = modules.single()
        assertEquals("m1", m.name)
        assertEquals(listOf(src1, src2), m.sources.toList())
        // classpath keeps order
        val cpList = m.classpath.map { it.absolutePath }
        assertTrue(cp1 in cpList && cp2 in cpList)

        val arguments = m.arguments as K2JVMCompilerArguments
        assertEquals("m1", arguments.moduleName)
        assertEquals(dest, arguments.destination)
        assertTrue(arguments.noStdlib)
        assertTrue(arguments.noJdk)
        assertTrue(arguments.noReflect)
        assertEquals("2.0", arguments.languageVersion)
        assertEquals("17", arguments.jvmTarget)
        assertTrue(arguments.reportOutputFiles)
        // optIn maps to list/array
        assertTrue(arguments.optIn?.contains("kotlin.ExperimentalStdlibApi") == true)
        assertEquals(listOf(src1.absolutePath, src2.absolutePath), arguments.freeArgs)
    }

}