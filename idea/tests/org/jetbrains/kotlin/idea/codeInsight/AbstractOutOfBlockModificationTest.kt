/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.trackers.outOfBlockModificationCount
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractOutOfBlockModificationTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(unused: String?) {
        val ktFile = myFixture.configureByFile(fileName()) as KtFile
        val expectedOutOfBlock = expectedOutOfBlockResult
        val isSkipCheckDefined = InTextDirectivesUtils.isDirectiveDefined(
            ktFile.text,
            SKIP_ANALYZE_CHECK_DIRECTIVE
        )
        assertTrue(
            "It's allowed to skip check with analyze only for tests where out-of-block is expected",
            !isSkipCheckDefined || expectedOutOfBlock
        )
        val tracker =
            PsiManager.getInstance(myFixture.project).modificationTracker as PsiModificationTrackerImpl
        val element = ktFile.findElementAt(myFixture.caretOffset)
        assertNotNull("Should be valid element", element)
        val oobBeforeType = ktFile.outOfBlockModificationCount
        val modificationCountBeforeType = tracker.modificationCount

        // have to analyze file before any change to support incremental analysis
        ktFile.analyzeWithAllCompilerChecks()

        myFixture.type(stringToType)
        PsiDocumentManager.getInstance(myFixture.project).commitDocument(myFixture.getDocument(myFixture.file))
        val oobAfterCount = ktFile.outOfBlockModificationCount
        val modificationCountAfterType = tracker.modificationCount
        assertTrue(
            "Modification tracker should always be changed after type",
            modificationCountBeforeType != modificationCountAfterType
        )
        assertEquals(
            "Result for out of block test is differs from expected on element in file:\n"
                    + FileUtil.loadFile(testDataFile()),
            expectedOutOfBlock, oobBeforeType != oobAfterCount
        )
        checkForUnexpectedErrors(ktFile)

        if (!isSkipCheckDefined) {
            checkOOBWithDescriptorsResolve(expectedOutOfBlock)
        }
    }

    private fun checkForUnexpectedErrors(ktFile: KtFile) {
        val diagnosticsProvider: (KtFile) -> Diagnostics = { it.analyzeWithAllCompilerChecks().bindingContext.diagnostics }
        DirectiveBasedActionUtils.checkForUnexpectedWarnings(ktFile, diagnosticsProvider)
        DirectiveBasedActionUtils.checkForUnexpectedErrors(ktFile, diagnosticsProvider)
    }

    private fun checkOOBWithDescriptorsResolve(expectedOutOfBlock: Boolean) {
        ApplicationManager.getApplication().runReadAction {
            (PsiManager.getInstance(myFixture.project).modificationTracker as PsiModificationTrackerImpl)
                .incOutOfCodeBlockModificationCounter()
        }
        val updateElement = myFixture.file.findElementAt(myFixture.caretOffset - 1)
        val kDoc: KDoc? = PsiTreeUtil.getParentOfType(updateElement, KDoc::class.java, false)
        val ktExpression: KtExpression? = PsiTreeUtil.getParentOfType(updateElement, KtExpression::class.java, false)
        val ktDeclaration: KtDeclaration? = PsiTreeUtil.getParentOfType(updateElement, KtDeclaration::class.java, false)
        val ktElement = ktExpression ?: ktDeclaration ?: return
        val facade = ktElement.containingKtFile.getResolutionFacade()
        val session = facade.getFrontendService(ResolveSession::class.java)

        session.forceResolveAll()
        val context = session.bindingContext
        if (ktExpression != null && ktExpression !== ktDeclaration) {
            val expression = if (ktExpression is KtFunctionLiteral) ktExpression.getParent() as KtLambdaExpression else ktExpression
            val processed = context.get(BindingContext.PROCESSED, expression)
            val expressionProcessed = processed === java.lang.Boolean.TRUE
            assertEquals(
                "Expected out-of-block should result expression analyzed and vise versa", expectedOutOfBlock,
                expressionProcessed
            )
        } else if (updateElement !is PsiComment && kDoc == null) { // comments could be ignored from analyze
            val declarationProcessed =
                context.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    ktDeclaration
                ) != null
            assertEquals(
                "Expected out-of-block should result declaration analyzed and vise versa", expectedOutOfBlock,
                declarationProcessed
            )
        }
    }

    private val stringToType: String
        get() {
            val text = myFixture.getDocument(myFixture.file).text
            val typeDirectives =
                InTextDirectivesUtils.findStringWithPrefixes(text, TYPE_DIRECTIVE)
            return if (typeDirectives != null) StringUtil.unescapeStringCharacters(typeDirectives) else "a"
        }

    private val expectedOutOfBlockResult: Boolean
        get() {
            val text = myFixture.getDocument(myFixture.file).text
            val outOfCodeBlockDirective = InTextDirectivesUtils.findStringWithPrefixes(
                text,
                OUT_OF_CODE_BLOCK_DIRECTIVE
            )
            assertNotNull(
                "${fileName()}: Expectation of code block result test should be configured with " +
                        "\"// " + OUT_OF_CODE_BLOCK_DIRECTIVE + " TRUE\" or " +
                        "\"// " + OUT_OF_CODE_BLOCK_DIRECTIVE + " FALSE\" directive in the file",
                outOfCodeBlockDirective
            )
            return outOfCodeBlockDirective?.toBoolean() ?: false
        }

    companion object {
        const val OUT_OF_CODE_BLOCK_DIRECTIVE = "OUT_OF_CODE_BLOCK:"
        const val SKIP_ANALYZE_CHECK_DIRECTIVE = "SKIP_ANALYZE_CHECK"
        const val TYPE_DIRECTIVE = "TYPE:"
    }
}