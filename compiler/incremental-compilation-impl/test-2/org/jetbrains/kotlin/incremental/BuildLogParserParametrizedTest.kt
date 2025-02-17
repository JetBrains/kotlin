/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class BuildLogParserParametrizedTest {

    @Parameterized.Parameter
    @JvmField
    var testDirName: String = ""

    @Test
    fun testParser() {
        fun String.normalizeSeparators() = replace("\r\n", "\n").trim()

        val testDir = File(TEST_ROOT, testDirName)
        val logFile = File(testDir, LOG_FILE_NAME)
        assert(logFile.isFile) { "Log file: $logFile does not exist" }

        val actualNormalized = dumpBuildLog(parseTestBuildLog(logFile)).normalizeSeparators()
        val expectedFile = File(testDir, EXPECTED_PARSED_LOG_FILE_NAME)

        if (!expectedFile.isFile) {
            expectedFile.createNewFile()
            expectedFile.writeText(actualNormalized)

            throw AssertionError("Expected file log did not exist, created: $expectedFile")
        }

        val expectedNormalized = expectedFile.readText().normalizeSeparators()
        Assert.assertEquals("Parsed content was unexpected: ", expectedNormalized, actualNormalized)

        // parse expected, dump again and compare (to check that dumped log can be parsed again)
        val reparsedActualNormalized = dumpBuildLog(parseTestBuildLog(expectedFile)).normalizeSeparators()
        Assert.assertEquals("Reparsed content was unexpected: ", expectedNormalized, reparsedActualNormalized)
    }

    companion object {
        private val TEST_ROOT = File("compiler/incremental-compilation-impl/testData/buildLogsParserData")
        private val LOG_FILE_NAME = "build.log"
        private val EXPECTED_PARSED_LOG_FILE_NAME = "expected.txt"

        @Suppress("unused")
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Array<String>> {
            val directories = TEST_ROOT.listFiles().filter { it.isDirectory }
            return directories.map { arrayOf(it.name) }
        }
    }
}