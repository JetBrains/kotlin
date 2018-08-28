/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.jetbrains.kotlin.spec.validators.*
import java.io.File

object TestsJsonMapBuilder {
    private val stringListType = object : TypeToken<List<String>>() {}.type

    private fun addJsonIfNotExist(element: JsonObject, key: Any): JsonObject {
        val stringKey = key.toString()
        if (!element.has(stringKey)) element.add(stringKey, JsonObject())
        return element.get(stringKey).asJsonObject
    }

    fun buildJsonElement(testInfo: LinkedSpecTest, testsMap: JsonObject) {
        val sectionElement = addJsonIfNotExist(testsMap, testInfo.section)
        val paragraphElement = addJsonIfNotExist(sectionElement, testInfo.paragraphNumber)
        val sentenceElement = addJsonIfNotExist(paragraphElement, testInfo.sentenceNumber)
        val testAreaElement = addJsonIfNotExist(sentenceElement, testInfo.testArea.name.toLowerCase())
        val testTypeElement = addJsonIfNotExist(testAreaElement, testInfo.testType.type)
        val testNumberElement = addJsonIfNotExist(testTypeElement, testInfo.testNumber)

        testNumberElement.addProperty("description", testInfo.description)
        testNumberElement.addProperty("caseNumber", testInfo.cases!!.size)

        if (testInfo.unexpectedBehavior!!)
            testNumberElement.addProperty("unexpectedBehavior", testInfo.unexpectedBehavior)

        if (testInfo.issues!!.isNotEmpty())
            testNumberElement.add("issues", Gson().toJsonTree(testInfo.issues, stringListType))
    }
}