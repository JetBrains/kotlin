package org.jetbrains.konan.findUsages

import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftSymbol
import org.jetbrains.konan.resolve.findSymbols
import org.jetbrains.konan.resolve.symbols.KtSwiftSymbolPsiWrapper
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinSwiftUsageSearcher : KotlinUsageSearcher<SwiftSymbol, KtNamedDeclaration>() {
    override fun SearchParameters.getTarget(): KtNamedDeclaration? = getUnwrappedTarget() as? KtNamedDeclaration
    override fun KtNamedDeclaration.toLightSymbols(): List<SwiftSymbol> = findSymbols(SwiftLanguageKind).cast()
    override fun createWrapper(target: KtNamedDeclaration, symbol: SwiftSymbol) = KtSwiftSymbolPsiWrapper(target, symbol)
    override val SwiftSymbol.word: String get() = name
}