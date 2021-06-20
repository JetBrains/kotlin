/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByPsiTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): List<KtSymbol> {
        return ktFile.collectDescendantsOfType<KtDeclaration>().map { declaration ->
            declaration.getSymbol()
        }
    }
}