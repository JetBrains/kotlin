/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCandidatesTest : AbstractHLApiSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.first()
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile)

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                val candidates = collectCallCandidates(expression)
                if (candidates.isEmpty()) {
                    "NO_CANDIDATES"
                } else {
                    candidates.joinToString("\n\n") { stringRepresentation(it) }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KtAnalysisSession.collectCallCandidates(element: PsiElement): List<KtCallInfo> = when (element) {
        is KtValueArgument -> this@collectCallCandidates.collectCallCandidates(element.getArgumentExpression()!!)
        is KtDeclarationModifierList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.collectCallCandidates()
        }
        is KtFileAnnotationList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.collectCallCandidates()
        }
        is KtElement -> element.collectCallCandidates()
        else -> error("Selected element type (${element::class.simpleName}) is not supported for resolveCandidates()")
    }

}
