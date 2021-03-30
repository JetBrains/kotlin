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
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractReturnExpressionTargetTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    protected fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val ktReturnExpressionAtCaret = ktFile.findElementAt(myFixture.caretOffset)?.parentOfType<KtReturnExpression>()
            ?: error("No element was found at caret or no <caret> is present in the test file")

        val expectedReturnTarget = ktFile.getExpectedReturnTarget()

        val actualReturnTargetPsi: KtDeclaration? = executeOnPooledThreadInReadAction {
            analyse(ktFile) {
                val actualReturnTargetSymbol = ktReturnExpressionAtCaret.getReturnTargetSymbol() ?: return@analyse null
                actualReturnTargetSymbol.psi as KtDeclaration
            }
        }

        Assert.assertEquals(expectedReturnTarget?.text, actualReturnTargetPsi?.text)
    }

    private fun KtFile.getExpectedReturnTarget(): KtDeclaration? {
        var declaration: KtDeclaration? = null
        forEachDescendantOfType<PsiComment> { comment ->
            if (comment.text == EXPECTED_RETURN_TARGET_COMMENT) {
                if (declaration != null) {
                    error("More than one $EXPECTED_RETURN_TARGET_COMMENT found")
                }
                declaration = comment.parentOfType()
            }
        }
        val noDeclarationExpected = InTextDirectivesUtils.findStringWithPrefixes(text, NO_TARGET_EXPECTED_PREFIX) != null
        return when {
            noDeclarationExpected && declaration != null -> {
                error("$noDeclarationExpected was present together with $EXPECTED_RETURN_TARGET_COMMENT")
            }
            !noDeclarationExpected && declaration == null -> {
                error("No $EXPECTED_RETURN_TARGET_COMMENT present, but $NO_TARGET_EXPECTED_PREFIX is not provided")
            }
            else -> declaration
        }
    }

    companion object {
        private const val EXPECTED_RETURN_TARGET_COMMENT = "/* EXPECTED_TARGET */"
        private const val NO_TARGET_EXPECTED_PREFIX = "// NO_TARGET_EXPECTED"
    }
}