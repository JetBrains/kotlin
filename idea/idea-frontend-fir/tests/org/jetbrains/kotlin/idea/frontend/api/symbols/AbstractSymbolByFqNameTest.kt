/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.SymbolByFqName
import org.jetbrains.kotlin.idea.test.framework.TestFileStructure

abstract class AbstractSymbolByFqNameTest : AbstractSymbolTest() {
    override fun KtAnalysisSession.collectSymbols(fileStructure: TestFileStructure): List<KtSymbol> {
        val symbolData = SymbolByFqName.getSymbolDataFromFile(fileStructure.filePath)
        return with(symbolData) { toSymbols() }
    }
}