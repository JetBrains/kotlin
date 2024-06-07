/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractExitPointSnapshotTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val textRange = testServices.expressionMarkerProvider.getSelectedRange(mainFile)
        val statements = findStatements(mainFile, textRange)

        @OptIn(KaAnalysisNonPublicApi::class)
        val actualText = analyseForTest(mainFile) {
            val snapshot = computeExitPointSnapshot(statements)
            stringRepresentation(snapshot)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
    }

    private fun findStatements(mainFile: KtFile, textRange: TextRange): List<KtExpression> {
        var candidate = PsiTreeUtil.findElementOfClassAtOffset(mainFile, textRange.startOffset, KtExpression::class.java, true)
            ?: error("Cannot find a starting element in range $textRange")

        while (true) {
            val parent = candidate.parent
            if (parent is KtExpression && parent.textRange in textRange && parent.startOffset == candidate.startOffset) {
                candidate = parent
            } else {
                break
            }
        }

        return generateSequence<PsiElement>(candidate) { it.nextSibling }
            .filterIsInstance<KtExpression>()
            .filter { it.textRange in textRange }
            .toList()
    }
}