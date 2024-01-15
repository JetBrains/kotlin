/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.completionCandidateChecker

import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.getSymbolOfType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompletionCandidateExtensionChecker : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val selectedExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtSimpleNameExpression>(mainFile)
        val receiverExpression = when (val parent = selectedExpression.parent) {
            is KtCallableReferenceExpression -> parent.receiverExpression
            else -> selectedExpression.getReceiverExpression()
        }

        val extensionsToCheck = mainFile.collectDescendantsOfType<KtCallableDeclaration> { it.isExtensionDeclaration() }

        val actual = analyseForTest(mainFile) {
            val extensionSymbolsToCheck = extensionsToCheck.map { it.getSymbolOfType<KtCallableSymbol>() }

            extensionsToCheck.zip(extensionSymbolsToCheck).map { (extension, extensionSymbol) ->
                val applicabilityResult = extensionSymbol.checkExtensionIsSuitable(mainFile, selectedExpression, receiverExpression)
                val substitutor = (applicabilityResult as? KtExtensionApplicabilityResult.Applicable)?.substitutor
                "${extension.name}: ${applicabilityResult::class.java.simpleName}\nsubstitutor: ${renderSubstitutor(substitutor)}"
            }.joinToString("\n\n")
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun renderSubstitutor(substitutor: KtSubstitutor?): String = when (substitutor) {
        is KtSubstitutor.Empty -> "empty"
        else -> substitutor.toString()
    }
}