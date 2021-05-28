/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.fir.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

abstract class AbstractSymbolByReferenceTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(fileStructure: TestFileStructure): List<KtSymbol> {
        val referenceExpression = getElementOfTypeAtCaret<KtNameReferenceExpression>()
        return listOfNotNull(referenceExpression.mainReference.resolveToSymbol())
    }
}