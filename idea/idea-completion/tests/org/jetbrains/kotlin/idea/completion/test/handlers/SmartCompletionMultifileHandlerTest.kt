/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import java.io.File

class SmartCompletionMultifileHandlerTest : KotlinCompletionTestCase() {
    fun testImportExtensionFunction() { doTest() }

    fun testImportExtensionProperty() { doTest() }

    fun testAnonymousObjectGenericJava() { doTest() }

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
