/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.util.parentOfType
import junit.framework.Assert
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.uitls.IgnoreTests
import java.io.File

abstract class AbstractExpectedExpressionTypeTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    protected fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val expressionAtCaret = ktFile.findElementAt(myFixture.caretOffset)?.parentOfType<KtExpression>()
            ?: error("No element was found at caret or no <caret> is present in the test file")

        val actualExpectedTypeText: String? = executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                expressionAtCaret.getExpectedType()?.asStringForDebugging()
            }
        }

        IgnoreTests.runTestWithFixMeSupport(testDataFile.toPath()) {
            KotlinTestUtils.assertEqualsToFile(File(path), testDataFile.getTextWithActualType(actualExpectedTypeText))
        }
    }

    private fun File.getTextWithActualType(actualType: String?) : String {
        val text = FileUtil.loadFile(this)
        val textWithoutTypeDirective = text.split('\n')
            .filterNot { it.startsWith(EXPECTED_TYPE_TEXT_DIRECTIVE) }
            .joinToString(separator = "\n")
        return "$textWithoutTypeDirective\n$EXPECTED_TYPE_TEXT_DIRECTIVE $actualType"
    }

    companion object {
        private const val EXPECTED_TYPE_TEXT_DIRECTIVE = "// EXPECTED_TYPE:"
    }
}