/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.callResolver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCallCandidateInfo
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compareCalls
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveCandidatesTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val expression = testServices.expressionMarkerProvider.getSelectedElementOfType<KtElement>(mainFile)

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                val candidates = collectCallCandidates(expression)
                if (candidates.isEmpty()) {
                    "NO_CANDIDATES"
                } else {
                    val sortedCandidates = candidates.sortedWith { candidate1, candidate2 ->
                        compareCalls(candidate1.candidate, candidate2.candidate)
                    }
                    sortedCandidates.joinToString("\n\n") { stringRepresentation(it) }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KtAnalysisSession.collectCallCandidates(element: PsiElement): List<KtCallCandidateInfo> = when (element) {
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
