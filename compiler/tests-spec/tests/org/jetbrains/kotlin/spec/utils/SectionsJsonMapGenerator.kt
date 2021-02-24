/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.LINKED_TESTS_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SECTIONS_TESTS_MAP_FILENAME
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.SPEC_TESTDATA_PATH
import java.io.File

object SectionsJsonMapGenerator {

    val sectionsMapsByTestArea: MutableMap<TestArea, JsonObject> = mutableMapOf()

    fun writeSectionsMapJsons() {
        val gson = GsonBuilder().setPrettyPrinting().create()
        sectionsMapsByTestArea.forEach { (testArea, json) ->
            val sectionsMapFolder = "$SPEC_TESTDATA_PATH/${testArea.testDataPath}/$LINKED_TESTS_PATH"
            File(sectionsMapFolder).mkdirs()
            val sectionsMapFile = File("$sectionsMapFolder/$SECTIONS_TESTS_MAP_FILENAME")
            sectionsMapFile.createNewFile()
            sectionsMapFile.writeText(gson.toJson(json))
        }
    }

    fun buildSectionsMap(testsMapPath: String) {
        val sectionInfo = SectionInfo.parsePath(testsMapPath)
        val testArea = sectionInfo.testArea
        val testAreaSectionsMap = sectionsMapsByTestArea[testArea] ?: JsonObject()
        addPathToTestAreaSectionsMap(testAreaSectionsMap, sectionInfo)
        sectionsMapsByTestArea[testArea] = testAreaSectionsMap
    }

    private fun addPathToTestAreaSectionsMap(testAreaSectionsMap: JsonObject, sectionInfo: SectionInfo) {
        if (!testAreaSectionsMap.has(sectionInfo.mainSection)) {
            val subsectionsPathArray = JsonArray()
            subsectionsPathArray.add(sectionInfo.subsectionsPath)
            testAreaSectionsMap.add(sectionInfo.mainSection, subsectionsPathArray)
        } else {
            val subsectionsPathArray = testAreaSectionsMap.get(sectionInfo.mainSection) as? JsonArray
                ?: throw Exception("json element doesn't exist")
            subsectionsPathArray.add(sectionInfo.subsectionsPath)
            testAreaSectionsMap.add(sectionInfo.mainSection, subsectionsPathArray)
        }
    }

    private class SectionInfo(
        val testArea: TestArea,
        val mainSection: String,
        val subsectionsPath: String
    ) {
        companion object {
            private fun identifyTestArea(path: String): TestArea {
                TestArea.values().forEach {
                    if (path.startsWith(it.testDataPath)) return it
                }
                throw IllegalArgumentException("testsMap path doesn't contain test area path")
            }

            fun parsePath(path: String): SectionInfo {
                val testArea = identifyTestArea(path)
                val fullSectionsPathList = path.subSequence(testArea.testDataPath.length + 1, path.length).toString().split("/")
                if (fullSectionsPathList.first() != LINKED_TESTS_PATH)
                    throw IllegalArgumentException("testsMap path doesn't contain linked directory")
                return SectionInfo(
                    testArea = testArea,
                    mainSection = fullSectionsPathList[1],
                    subsectionsPath = fullSectionsPathList.subList(2, fullSectionsPathList.size).joinToString("/")
                )
            }
        }
    }
}