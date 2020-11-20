/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.scopes

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.SymbolData
import org.jetbrains.kotlin.idea.frontend.api.symbols.AbstractSymbolsByFqNameBuildingTest
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

abstract class AbstractMemberScopeByFqNameTest : AbstractSymbolsByFqNameBuildingTest() {
    override fun KtAnalysisSession.createSymbols(symbolData: SymbolData): List<KtSymbol> {
        val symbols = with(symbolData) { toSymbols() }
        val classSymbol = symbols.singleOrNull() as? KtClassOrObjectSymbol ?: error("Should be a single class symbol, but $symbols found")
        return classSymbol.getMemberScope().getAllSymbols().toList()
    }
}