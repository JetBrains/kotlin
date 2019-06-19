/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.conversion.copy.ConvertJavaCopyPasteProcessor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPerformanceJavaToKotlinCopyPasteConversionTest(private val newJ2K: Boolean = false) :
    AbstractJavaToKotlinCopyPasteConversionTest() {

    private val stats: Stats = Stats("-${j2kPrefix()}-j2k")

    companion object {
        @JvmStatic
        var warmedUp: Array<Boolean> = arrayOf(false, false)

    }

    override fun setUp() {
        super.setUp()

        Registry.get("kotlin.use.new.j2k").setValue(newJ2K)

        val index = if (newJ2K) 1 else 0
        if (!warmedUp[index]) {
            doWarmUpPerfTest()
            warmedUp[index] = true
        }
    }

    private fun doWarmUpPerfTest() {
        val prefix = j2kPrefix()
        with(myFixture) {
            configureByText(JavaFileType.INSTANCE, "<selection>public class Foo {\nprivate String value;\n}</selection>")
            performEditorAction(IdeActions.ACTION_CUT)
            configureByText(KotlinFileType.INSTANCE, "<caret>")
            ConvertJavaCopyPasteProcessor.conversionPerformed = false
            tcSimplePerfTest("", "warm-up ${prefix} java2kotlin conversion", stats) {
                performEditorAction(IdeActions.ACTION_PASTE)
            }
        }

        kotlin.test.assertFalse(!ConvertJavaCopyPasteProcessor.conversionPerformed, "No conversion to Kotlin suggested")
        assertEquals("class Foo {\n    private val value: String? = null\n}", myFixture.file.text)
    }

    private fun j2kPrefix(): String {
        return if (newJ2K) "new" else "old"
    }

    fun doPerfTest(path: String) {
        myFixture.testDataPath = testDataPath
        val testName = getTestName(false)

        myFixture.configureByFiles("$testName.java")

        val fileText = myFixture.editor.document.text
        val noConversionExpected = InTextDirectivesUtils.findListWithPrefixes(fileText, "// NO_CONVERSION_EXPECTED").isNotEmpty()

        myFixture.performEditorAction(IdeActions.ACTION_COPY)

        configureByDependencyIfExists("$testName.dependency.kt")
        configureByDependencyIfExists("$testName.dependency.java")

        configureTargetFile("$testName.to.kt")

        ConvertJavaCopyPasteProcessor.conversionPerformed = false

        val prefix = j2kPrefix()

        attempts {
            tcSimplePerfTest(testName, "${prefix} java2kotlin conversion$it: $testName", stats) {
                myFixture.performEditorAction(IdeActions.ACTION_PASTE)
            }

            validate(path, noConversionExpected)

            myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        }

    }

    open fun validate(path: String, noConversionExpected: Boolean) {
        kotlin.test.assertEquals(
            noConversionExpected, !ConvertJavaCopyPasteProcessor.conversionPerformed,
            if (noConversionExpected) "Conversion to Kotlin should not be suggested" else "No conversion to Kotlin suggested"
        )
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".java", ".expected.kt")), myFixture.file.text)
    }
}