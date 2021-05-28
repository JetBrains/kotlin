/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.fir.test.framework.TestFileStructure
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

abstract class AbstractSymbolByPsiTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(fileStructure: TestFileStructure): List<KtSymbol> =
        fileStructure.mainKtFile.collectDescendantsOfType<KtDeclaration>().map { declaration ->
            declaration.getSymbol()
        }
}