/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.SymbolByFqName
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractDelegateMemberScopeTest : AbstractSymbolByFqNameTest() {

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val symbolData = SymbolByFqName.getSymbolDataFromFile(testDataPath)
        val symbols = with(symbolData) { toSymbols() }
        val classSymbol = symbols.singleOrNull() as? KtClassOrObjectSymbol
            ?: error("Should be a single class symbol, but $symbols found")
        return SymbolsData(classSymbol.getDelegatedMemberScope().getCallableSymbols().toList())
    }
}
