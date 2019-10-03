/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.core.util.getLineEndOffset
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.debugger.breakpoints.*
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils

abstract class AbstractBreakpointApplicabilityTest : KotlinLightCodeInsightFixtureTestCase() {
    private companion object {
        private const val COMMENT = "///"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    protected fun doTest(unused: String) {
        val ktFile = myFixture.configureByFile(fileName()) as KtFile

        val actualContents = checkBreakpoints(ktFile, BreakpointChecker())
        KotlinTestUtils.assertEqualsToFile(testDataFile(), actualContents)
    }

    private fun checkBreakpoints(file: KtFile, checker: BreakpointChecker): String {
        val lineCount = file.getLineCount()
        return (0..lineCount).joinToString("\n") { line -> checkLine(file, line, checker) }
    }

    private fun checkLine(file: KtFile, line: Int, checker: BreakpointChecker): String {
        val lineText = file.getLine(line)
        val expectedBreakpointTypes = lineText.substringAfterLast(COMMENT).trim().split(",").map { it.trim() }.toSortedSet()
        val actualBreakpointTypes = checker.check(file, line).map { it.prefix }.distinct().toSortedSet()

        return if (expectedBreakpointTypes != actualBreakpointTypes) {
            val lineWithoutComments = lineText.substringBeforeLast(COMMENT).trimEnd()
            if (actualBreakpointTypes.isNotEmpty()) {
                "$lineWithoutComments $COMMENT " + actualBreakpointTypes.joinToString()
            } else {
                lineWithoutComments
            }
        } else {
            lineText
        }
    }

    private fun PsiFile.getLine(line: Int): String {
        val start = getLineStartOffset(line, skipWhitespace = false) ?: error("Cannot find start for line $line")
        val end = getLineEndOffset(line) ?: error("Cannot find end for line $line")
        if (start >= end) {
            return ""
        }

        return text.substring(start, end)
    }

    private fun getPath(path: String): String {
        return path.substringAfter(PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE.drop(1), path)
    }
}