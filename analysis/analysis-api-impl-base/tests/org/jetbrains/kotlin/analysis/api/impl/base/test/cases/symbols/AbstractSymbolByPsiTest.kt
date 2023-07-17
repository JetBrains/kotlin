/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractSymbolByPsiTest : AbstractSymbolTest() {
    override val defaultRendererOption: PrettyRendererOption get() = PrettyRendererOption.BODY_WITH_MEMBERS

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val allDeclarationSymbols = ktFile.collectDescendantsOfType<KtDeclaration> { it.isValidForSymbolCreation }.map { declaration ->
            declaration.getSymbol()
        }

        return SymbolsData(
            allDeclarationSymbols,
            listOf(ktFile.getFileSymbol()),
        )
    }
}
