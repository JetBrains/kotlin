/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.SymbolData

abstract class AbstractSymbolsByFqNameBuildingTest : AbstractSymbolByFqNameTest() {
    override fun KtAnalysisSession.createSymbols(symbolData: SymbolData): List<KtSymbol> =
        with(symbolData) { toSymbols() }
}