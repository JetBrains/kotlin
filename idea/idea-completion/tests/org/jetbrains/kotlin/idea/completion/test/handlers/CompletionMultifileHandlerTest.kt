/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinFixtureCompletionBaseTestCase
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class CompletionMultiFileHandlerTest : KotlinFixtureCompletionBaseTestCase() {
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

    fun testStaticMethodFromGrandParent() {
        doTest('\n', "StaticMethodFromGrandParent-1.java", "StaticMethodFromGrandParent-2.java")
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

    fun testPropertyFunctionConflict() {
        doTest()
    }

    fun testPropertyFunctionConflict2() {
        doTest(tailText = " { Int, Int -> ... } (i: (Int, Int) -> Unit) (a.b)")
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

    fun doTest(completionChar: Char = '\n', vararg extraFileNames: String, tailText: String? = null) {
        val fileName = getTestName(false)

        val defaultFiles = listOf("$fileName-1.kt", "$fileName-2.kt")
        val filteredFiles = defaultFiles.filter { File(testDataPath, it).exists() }

        require(filteredFiles.isNotEmpty()) { "At least one of $defaultFiles should exist!" }

        myFixture.configureByFiles(*extraFileNames)
        myFixture.configureByFiles(*filteredFiles.toTypedArray())
        val items = complete(CompletionType.BASIC, 2)
        if (items != null) {
            val item = if (tailText == null)
                items.singleOrNull() ?: error("Multiple items in completion")
            else {
                val presentation = LookupElementPresentation()
                items.first {
                    it.renderElement(presentation)
                    presentation.tailText == tailText
                }
            }

            CompletionHandlerTestBase.selectItem(myFixture, item, completionChar)
        }
        myFixture.checkResultByFile("$fileName.kt.after")
    }

    override fun getTestDataPath() = File(COMPLETION_TEST_DATA_BASE_PATH, "/handlers/multifile/").path + File.separator

    override fun defaultCompletionType(): CompletionType = CompletionType.BASIC
    override fun getPlatform(): TargetPlatform = JvmPlatforms.unspecifiedJvmPlatform
    override fun getProjectDescriptor() = LightJavaCodeInsightFixtureTestCase.JAVA_LATEST
}
