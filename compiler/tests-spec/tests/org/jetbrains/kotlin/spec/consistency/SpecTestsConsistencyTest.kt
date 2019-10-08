/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.consistency

import com.intellij.testFramework.TestDataPath
import junit.framework.TestCase
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.SpecTestLinkedType
import org.jetbrains.kotlin.spec.utils.TestArea
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.parseLinkedSpecTest
import org.jetbrains.kotlin.spec.utils.spec.SpecSentencesStorage
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.io.walkTopDown

@TestDataPath("\$PROJECT_ROOT/compiler/tests-spec/testData/")
@RunWith(com.intellij.testFramework.Parameterized::class)
class SpecTestsConsistencyTest : TestCase() {
    @org.junit.runners.Parameterized.Parameter
    lateinit var testFilePath: String

    companion object {
        private val specSentencesStorage = SpecSentencesStorage()

        @org.junit.runners.Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun getTestFiles() = emptyList<Array<Any>>()

        @com.intellij.testFramework.Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun getTestFiles(klass: Class<*>): List<Array<String>> {
            val testFiles = mutableListOf<Array<String>>()

            TestArea.values().forEach { testArea ->
                val testDataPath =
                    "${GeneralConfiguration.TESTDATA_PATH}/${testArea.testDataPath}/${SpecTestLinkedType.LINKED.testDataPath}"

                testFiles += File(testDataPath).let { testsDir ->
                    testsDir.walkTopDown().filter { it.extension == "kt" }.map {
                        arrayOf(it.relativeTo(File(GeneralConfiguration.TESTDATA_PATH)).path.replace("/", "$"))
                    }.toList()
                }
            }

            return testFiles
        }
    }

    @Test
    fun doTest() {
        val file = File("${GeneralConfiguration.TESTDATA_PATH}/${testFilePath.replace("$", "/")}")
        val specSentences = specSentencesStorage.getLatest() ?: return
        val test = parseLinkedSpecTest(file.canonicalPath, mapOf("main" to file.readText()))
        val sectionsPath = setOf(*test.place.sections.toTypedArray(), test.place.paragraphNumber).joinToString()
        val sentenceNumber = test.place.sentenceNumber
        val paragraphSentences = specSentences[sectionsPath]

        if (paragraphSentences != null && paragraphSentences.size >= sentenceNumber) {
            val specSentencesForCurrentTest =
                specSentencesStorage[test.specVersion] ?: throw Exception("spec ${test.specVersion} not found")
            val paragraphForTestSentences =
                specSentencesForCurrentTest[sectionsPath] ?: throw Exception("$sectionsPath not found")
            if (paragraphForTestSentences.size < sentenceNumber) {
                throw Exception("$sentenceNumber not found")
            }
            val expectedSentence = paragraphForTestSentences[sentenceNumber - 1]
            val actualSentence = paragraphSentences[sentenceNumber - 1]
            val locationSentenceText = "$sectionsPath paragraph, $sentenceNumber sentence"

            println("Comparing versions: ${test.specVersion} (for expected) and ${specSentencesStorage.latestSpecVersion} (for actual)")
            println("Expected location: $locationSentenceText")

            assertEquals(expectedSentence, actualSentence)
        }
    }
}
