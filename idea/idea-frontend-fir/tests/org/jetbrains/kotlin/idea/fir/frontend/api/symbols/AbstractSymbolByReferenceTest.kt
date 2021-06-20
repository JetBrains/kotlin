/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import org.jetbrains.kotlin.idea.fir.test.framework.expressionMarkerProvider
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByReferenceTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): List<KtSymbol> {
        val referenceExpression = testServices.expressionMarkerProvider.getElementOfTypAtCaret<KtNameReferenceExpression>(ktFile)
        return listOfNotNull(referenceExpression.mainReference.resolveToSymbol())
    }
}