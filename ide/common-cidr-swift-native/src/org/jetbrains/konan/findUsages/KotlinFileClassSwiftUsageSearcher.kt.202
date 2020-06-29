/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.findUsages

import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import org.jetbrains.konan.resolve.findFileClassSymbols
import org.jetbrains.konan.resolve.symbols.KtSwiftSymbolPsiWrapper
import org.jetbrains.konan.resolve.symbols.KtSymbolPsiWrapper
import org.jetbrains.konan.resolve.symbols.swift.KtSwiftClassSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinFileClassSwiftUsageSearcher : KotlinUsageSearcher<KtSwiftClassSymbol, KtFile>() {
    override fun SearchParameters.getTarget(): KtFile? = getUnwrappedTarget() as? KtFile
    override fun KtFile.toLightSymbols(): List<KtSwiftClassSymbol> = findFileClassSymbols(SwiftLanguageKind).cast()
    override fun createWrapper(target: KtFile, symbol: KtSwiftClassSymbol): KtSymbolPsiWrapper = KtSwiftSymbolPsiWrapper(target, symbol)
    override val KtSwiftClassSymbol.word: String? get() = name
}