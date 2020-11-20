/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.LINKED_TESTS_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTS_MAP_FILENAME
import org.jetbrains.kotlin.spec.utils.models.LinkedSpecTest
import org.jetbrains.kotlin.spec.utils.models.SpecPlace
import org.jetbrains.kotlin.spec.utils.parsers.CommonParser
import org.jetbrains.kotlin.spec.utils.parsers.LinkedSpecTestPatterns
import java.io.File

object TestsJsonMapGenerator {

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

    enum class LinkType {
        MAIN,
        PRIMARY,
        SECONDARY;

        override fun toString(): String {
            return name.toLowerCase()
        }
    }

    private fun getTestInfo(test: LinkedSpecTest, testFile: File? = null, linkType: LinkType = LinkType.MAIN) =
        JsonObject().apply {
            addProperty("specVersion", test.specVersion)
            addProperty("casesNumber", test.cases.byNumbers.size)
            addProperty("description", test.description)
            addProperty("path", testFile?.path)
            addProperty(
                "unexpectedBehaviour",
                test.unexpectedBehavior || test.cases.byNumbers.any { it.value.unexpectedBehavior }
            )
            addProperty("linkType", linkType.toString())
            test.helpers?.run { addProperty("helpers", test.helpers.joinToString()) }
        }


    private fun collectInfoFromTests(
        testsMap: JsonObject,
        testOrigin: TestOrigin,
    ) {
        val isImplementationTest = testOrigin == TestOrigin.IMPLEMENTATION
        TestArea.values().forEach { testArea ->
            File(testOrigin.getFilePath(testArea)).walkTopDown()
                .forEach testFiles@{ file ->
                    if (!file.isFile || file.extension != "kt" || file.name.endsWith(".fir.kt")) return@testFiles
                    if (isImplementationTest && !LinkedSpecTestPatterns.testInfoPattern.matcher(file.readText()).find())
                        return@testFiles

                    val (specTest, _) = CommonParser.parseSpecTest(
                        file.canonicalPath,
                        mapOf("main.kt" to file.readText()),
                        isImplementationTest
                    )
                    if (specTest is LinkedSpecTest) {
                        collectInfoFromTest(testsMap, specTest, file)
                    }
                }
        }
    }

    private fun collectInfoFromTest(
        testsMap: JsonObject, specTest: LinkedSpecTest, file: File
    ) {

        if (specTest.mainLink != null)
            testsMap.getOrCreateSpecTestObject(specTest.mainLink, specTest.testArea, specTest.testType)
                .add(getTestInfo(specTest, file, LinkType.MAIN))
        specTest.primaryLinks?.forEach {
            testsMap.getOrCreateSpecTestObject(it, specTest.testArea, specTest.testType).add(getTestInfo(specTest, file, LinkType.PRIMARY))
        }
        specTest.secondaryLinks?.forEach {
            testsMap.getOrCreateSpecTestObject(it, specTest.testArea, specTest.testType)
                .add(getTestInfo(specTest, file, LinkType.SECONDARY))
        }
    }

    fun buildTestsMapPerSection() {
        val testsMap = JsonObject().apply {
            collectInfoFromTests(this, TestOrigin.SPEC)
            collectInfoFromTests(this, TestOrigin.IMPLEMENTATION)
        }

        val gson = GsonBuilder().setPrettyPrinting().create()

        testsMap.keySet().forEach { testPath ->
            val testMapFolder = "${GeneralConfiguration.SPEC_TESTDATA_PATH}/$testPath"

            File(testMapFolder).mkdirs()
            File("$testMapFolder/$TESTS_MAP_FILENAME").writeText(gson.toJson(testsMap.get(testPath)))
            SectionsJsonMapGenerator.buildSectionsMap(testPath)
        }
    }
}
