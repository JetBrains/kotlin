/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractSingleSymbolByPsiTest : AbstractSymbolTest() {
    override val suppressPsiBasedFilePointerCheck: Boolean get() = false

    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            ktFile, testServices.moduleStructure.modules.first(),
            defaultType = KtDeclaration::class
        )

        val symbol = when (declaration) {
            is KtDeclaration -> declaration.symbol
            is KtFile -> declaration.symbol
            else -> error("Selected element type should be a declaration or a file")
        }

        return SymbolsData(listOf(symbol))
    }
}
