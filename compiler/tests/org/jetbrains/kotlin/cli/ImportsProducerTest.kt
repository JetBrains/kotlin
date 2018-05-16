/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestMetadata
import java.io.File

@TestMetadata("compiler/testData/importsProducer")
@TestDataPath("\$PROJECT_ROOT")
class ImportsProducerTest : TestCaseWithTmpdir() {

    @TestMetadata("simpleCase/importsProducer.args")
    fun testImportsProducer() {
        doTest("compiler/testData/importsProducer/simpleCase")
    }

    fun doTest(testDataDirPath: String) {
        System.setProperty("java.awt.headless", "true")
        val testDataDir = File(testDataDirPath)
        val expectedDumpFile = testDataDir.resolve(testDataDir.name + ".dump")
        val expectedOutputFile = testDataDir.resolve(testDataDir.name + ".out")
        val actualDumpFile = tmpdir.resolve(testDataDir.name + ".dump")

        // Check CLI-output of compiler
        val actualOutput = invokeImportsProducerAndGrabOutput(testDataDir, tmpdir, actualDumpFile)
        KotlinTestUtils.assertEqualsToFile(expectedOutputFile, actualOutput)

        // Check imports dump
        // Note that imports dumper outputs absolute paths to files, which is inconvenient for tests,
        // so we have to relativize them
        val actualRelativizedDump = FileUtil.loadFile(actualDumpFile, Charsets.UTF_8.name(), /* convertLineSeparators = */ true)
            .relativizeAbsolutePaths(testDataDir)

        KotlinTestUtils.assertEqualsToFile(expectedDumpFile, actualRelativizedDump)
    }

    private fun invokeImportsProducerAndGrabOutput(testDataDir: File, tmpDir: File, actualDumpFile: File): String {
        val compiler = K2JVMCompiler()
        val (output, exitCode) = executeCompilerGrabOutput(
            compiler,
            listOf(
                testDataDir.absolutePath,
                DESTINATION_COMPILER_ARGUMENT,
                tmpDir.path,
                OUTPUT_IMPORTS_COMPILER_ARGUMENT + "=" + actualDumpFile.path
            )
        )

//        TestCase.assertEquals("Imports dumper should return ExitCode.OK", ExitCode.OK, exitCode)

        return getNormalizedCompilerOutput(output, exitCode, testDataDir.path)
    }

    companion object {
        const val DESTINATION_COMPILER_ARGUMENT = "-d"
        const val OUTPUT_IMPORTS_COMPILER_ARGUMENT = "-Xoutput-imports"
    }
}

private fun String.relativizeAbsolutePaths(relativeTo: File): String {
    // JSON escapes slashes
    val pattern = relativeTo.absoluteFile.toString().replace("/", "\\/")
    return this.replace(pattern, "\$TESTDATA_DIR$")
}
