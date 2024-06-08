/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
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

            val actualString = generateSequence(ktSymbol) { it.containingSymbol }
                .filterIsInstance<KaDeclarationSymbol>()
                .joinToString("\n") { render(it) }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualString)
        }
    }

    private fun render(symbol: KaSymbol): String {
        val qualifiedName = when (symbol) {
            is KaCallableSymbol -> symbol.callableId?.toString()
            is KaClassLikeSymbol -> symbol.classId?.toString()
            else -> null
        }

        return qualifiedName ?: (symbol as? KaNamedSymbol)?.name?.asString() ?: "Unnamed"
    }
}