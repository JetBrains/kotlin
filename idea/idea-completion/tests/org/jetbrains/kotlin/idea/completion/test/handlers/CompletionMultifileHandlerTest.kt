/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
