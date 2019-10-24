/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTest
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTest.Companion.getInstanceForImplementationTest
import org.jetbrains.kotlin.spec.utils.models.SpecPlace
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser.parseImplementationTest
import org.jetbrains.kotlin.spec.utils.parsers.ImplementationTestPatterns
import java.io.File

object TestsJsonMapGenerator {
    private const val LINKED_TESTS_PATH = "linked"
    const val TESTS_MAP_FILENAME = "testsMap.json"

    private inline fun <reified T : JsonElement> JsonObject.getOrCreate(key: String): T {
        if (!has(key)) {
            add(key, T::class.java.newInstance())
        }
        return get(key) as T
    }

    private fun JsonObject.getOrCreateSpecTestObject(specPlace: SpecPlace, testArea: TestArea, testType: TestType): JsonArray {
        val sections = "${testArea.testDataPath}/$LINKED_TESTS_PATH/${specPlace.sections.joinToString("/")}"
        val testsBySection = getOrCreate<JsonObject>(sections)
        val testsByParagraph = testsBySection.getOrCreate<JsonObject>(specPlace.paragraphNumber.toString())
        val testsByType = testsByParagraph.getOrCreate<JsonObject>(testType.type)

        return testsByType.getOrCreate(specPlace.sentenceNumber.toString())
    }

    private fun getTestInfo(test: LinkedSpecTest, testFile: File? = null) =
        JsonObject().apply {
            addProperty("specVersion", test.specVersion)
            addProperty("casesNumber", test.cases.byNumbers.size)
            addProperty("description", test.description)
            addProperty("path", testFile?.path)
            addProperty(
                "unexpectedBehaviour",
                test.unexpectedBehavior || test.cases.byNumbers.any { it.value.unexpectedBehavior }
            )
        }

    private fun collectInfoFromSpecTests(): JsonObject {
        val testsMap = JsonObject()

        TestArea.values().forEach { testArea ->
            File("${GeneralConfiguration.SPEC_TESTDATA_PATH}/${testArea.testDataPath}/$LINKED_TESTS_PATH").walkTopDown()
                .forEach testFiles@{ file ->
                    if (!file.isFile || file.extension != "kt") return@testFiles

                    val (specTest, _) = CommonParser.parseSpecTest(file.canonicalPath, mapOf("main.kt" to file.readText()))

                    if (specTest is LinkedSpecTest) {
                        val testInfo = getTestInfo(specTest)
                        val testInfoWithFilePath = getTestInfo(specTest, file)

                        testsMap.getOrCreateSpecTestObject(specTest.place, specTest.testArea, specTest.testType).add(testInfo)

                        specTest.relevantPlaces?.forEach {
                            testsMap.getOrCreateSpecTestObject(it, specTest.testArea, specTest.testType).add(testInfoWithFilePath)
                        }
                    }
                }
        }

        return testsMap
    }

    private fun collectInfoFromImplementationTests(): JsonObject {
        val testsMap = JsonObject()

        TestArea.values().forEach { testArea ->
            val files = File("${GeneralConfiguration.TESTDATA_PATH}/${testArea.testDataPath}").walkTopDown()

            for (file in files) {
                if (!file.isFile || file.extension != "kt") continue

                val parserImplementationTest = parseImplementationTest(file, testArea)
                val relevantPlaces = parserImplementationTest.relevantPlaces ?: listOf()

                (relevantPlaces + parserImplementationTest.place).forEach { specPlace ->
                    testsMap.getOrCreateSpecTestObject(specPlace, testArea, parserImplementationTest.testType).add(
                        getTestInfo(parseImplementationTest(file, testArea), file)
                    )
                }
            }
        }

        return testsMap
    }

    operator fun JsonObject.plus(other: JsonObject) =
        JsonObject().also { new ->
            this.keySet().forEach { new.add(it, this.get(it)) }
            other.keySet().forEach { new.add(it, other.get(it)) }
        }

    fun buildTestsMapPerSection() {
        val testsMap = collectInfoFromSpecTests() + collectInfoFromImplementationTests()
        val gson = GsonBuilder().setPrettyPrinting().create()

        testsMap.keySet().forEach { testPath ->
            val testMapFolder = "${GeneralConfiguration.SPEC_TESTDATA_PATH}/$testPath"

            File(testMapFolder).mkdirs()
            File("$testMapFolder/$TESTS_MAP_FILENAME").writeText(gson.toJson(testsMap.get(testPath)))
        }
    }
}
