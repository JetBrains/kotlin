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

import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import java.io.File

class CompletionMultiFileHandlerTest : KotlinCompletionTestCase() {
    fun testExtensionFunctionImport() {
        doTest()
    }

    fun testExtensionPropertyImport() {
        doTest()
    }

    fun testImportAlreadyImportedObject() {
        doTest()
    }

    fun testJetClassCompletionImport() {
        doTest()
    }

    fun testTopLevelFunctionImport() {
        doTest()
    }

    fun testTopLevelFunctionInQualifiedExpr() {
        doTest()
    }

    fun testTopLevelPropertyImport() {
        doTest()
    }

    fun testTopLevelValImportInStringTemplate() {
        doTest()
    }

    fun testNoParenthesisInImports() {
        doTest()
    }

    fun testKeywordExtensionFunctionName() {
        doTest()
    }

    fun testInfixExtensionCallImport() {
        doTest()
    }

    fun testClassWithClassObject() {
        doTest()
    }

    fun testGlobalFunctionImportInLambda() {
        doTest()
    }

    fun testObjectInStringTemplate() {
        doTest()
    }

    fun testExclCharInsertImport() {
        doTest('!')
    }

    fun testPropertyKeysWithPrefixEnter() {
        doTest('\n', "TestBundle.properties")
    }

    fun testPropertyKeysWithPrefixTab() {
        doTest('\t', "TestBundle.properties")
    }

    fun testFileRefInStringLiteralEnter() {
        doTest('\n', "foo.txt", "bar.txt")
    }

    fun testFileRefInStringLiteralTab() {
        doTest('\t', "foo.txt", "bar.txt")
    }

    fun testNotImportedExtension() {
        doTest()
    }

    fun testNotImportedTypeAlias() {
        doTest()
    }

    fun testKT12077() {
        doTest()
    }

    fun doTest(completionChar: Char = '\n', vararg extraFileNames: String) {
        val fileName = getTestName(false)

        configureByFiles(null, *extraFileNames)
        configureByFiles(null, fileName + "-1.kt", fileName + "-2.kt")
        complete(2)
        if (myItems != null) {
            val item = myItems.singleOrNull() ?: error("Multiple items in completion")
            selectItem(item, completionChar)
        }
        checkResultByFile(fileName + ".kt.after")
    }

    override fun getTestDataPath() = File(COMPLETION_TEST_DATA_BASE_PATH, "/handlers/multifile/").path + File.separator
}
