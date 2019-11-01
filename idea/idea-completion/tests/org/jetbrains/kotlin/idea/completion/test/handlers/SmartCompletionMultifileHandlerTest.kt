/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class SmartCompletionMultifileHandlerTest : KotlinCompletionTestCase() {
    fun testImportExtensionFunction() { doTest() }

    fun testImportExtensionProperty() { doTest() }

    fun testAnonymousObjectGenericJava() { doTest() }

    fun testImportAnonymousObject() { doTest() }

    fun testNestedSamAdapter() { doTest(lookupString = "Nested") }

    override fun setUp() {
        setType(CompletionType.SMART)
        super.setUp()
    }

    private fun doTest(lookupString: String? = null, itemText: String? = null) {
        val fileName = getTestName(false)

        val fileNames = listOf(fileName + "-1.kt", fileName + "-2.kt", fileName + ".java")

        configureByFiles(null, *fileNames.filter { File(testDataPath + it).exists() }.toTypedArray())

        complete(1)
        if (myItems != null) {
            fun isMatching(lookupElement: LookupElement): Boolean {
                if (lookupString != null && lookupElement.lookupString != lookupString) return false

                val presentation = LookupElementPresentation()
                lookupElement.renderElement(presentation)
                if (itemText != null && presentation.itemText != itemText) return false

                return true
            }

            val items = myItems.filter(::isMatching)
            when (items.size) {
                0 -> fail("No matching items found")
                1 -> selectItem(myItems[0])
                else -> fail("Multiple matching items found")
            }
        }

        checkResultByFile(fileName + ".kt.after")
    }

    override fun getTestDataPath() = File(COMPLETION_TEST_DATA_BASE_PATH, "/handlers/multifile/smart/").path + File.separator
}
