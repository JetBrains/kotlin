/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
        val testDir = File(TEST_ROOT, testDirName)
        val logFile = File(testDir, LOG_FILE_NAME)
        assert(logFile.isFile) { "Log file: $logFile does not exist" }

        val actualNormalized = dumpBuildLog(parseTestBuildLog(logFile)).replace("\r\n", "\n").trim()
        val expectedFile = File(testDir, EXPECTED_PARSED_LOG_FILE_NAME)

        if (!expectedFile.isFile) {
            expectedFile.createNewFile()
            expectedFile.writeText(actualNormalized)

            throw AssertionError("Expected file log did not exist, created: $expectedFile")
        }

        val expectedNormalized = expectedFile.readText().replace("\r\n", "\n").trim()
        Assert.assertEquals("Parsed content was unexpected: ", expectedNormalized, actualNormalized)

        // parse expected, dump again and compare (to check that dumped log can be parsed again)
        val reparsedActualNormalized = dumpBuildLog(parseTestBuildLog(expectedFile)).trim()
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
            return directories.map { arrayOf(it.name) }.toList()
        }
    }
}