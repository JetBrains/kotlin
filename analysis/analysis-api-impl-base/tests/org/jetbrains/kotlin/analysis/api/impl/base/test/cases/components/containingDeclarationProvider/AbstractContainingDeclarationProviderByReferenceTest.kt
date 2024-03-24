/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderByReferenceTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val referenceExpression = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtReferenceExpression>(mainFile)

        analyseForTest(mainFile) {
            val ktSymbol = referenceExpression.mainReference.resolveToSymbol() ?: error("Reference is not resolved")

            val actualString = generateSequence(ktSymbol) { it.getContainingSymbol() }
                .filterIsInstance<KtDeclarationSymbol>()
                .joinToString("\n") { render(it) }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualString)
        }
    }

    private fun render(symbol: KtSymbol): String {
        val qualifiedName = when (symbol) {
            is KtCallableSymbol -> symbol.callableIdIfNonLocal?.toString()
            is KtClassLikeSymbol -> symbol.classIdIfNonLocal?.toString()
            else -> null
        }

        return qualifiedName ?: (symbol as? KtNamedSymbol)?.name?.asString() ?: "Unnamed"
    }
}