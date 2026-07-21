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

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class BuildLogParserParametrizedTest {

    @ParameterizedTest
    @MethodSource("data")
    fun testParser(testDirName: String) {
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
        assertEquals(expectedNormalized, actualNormalized, "Parsed content was unexpected: ")

        // parse expected, dump again and compare (to check that dumped log can be parsed again)
        val reparsedActualNormalized = dumpBuildLog(parseTestBuildLog(expectedFile)).normalizeSeparators()
        assertEquals(expectedNormalized, reparsedActualNormalized, "Reparsed content was unexpected: ")
    }

    companion object {
        private val TEST_ROOT = ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/buildLogsParserData")
        private val LOG_FILE_NAME = "build.log"
        private val EXPECTED_PARSED_LOG_FILE_NAME = "expected.txt"

        @JvmStatic
        fun data(): List<String> {
            val directories = TEST_ROOT.listFiles().orEmpty().filter { it.isDirectory }
            return directories.map { it.name }.sorted()
        }
    }
}
