package org.jetbrains.konan.findUsages

import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftInitializerSymbol
import org.jetbrains.konan.resolve.findSymbols
import org.jetbrains.konan.resolve.symbols.KtSwiftSymbolPsiWrapper
import org.jetbrains.kotlin.psi.KtConstructor

class KotlinInitializerSwiftUsageSearcher : KotlinUsageSearcher<SwiftInitializerSymbol, KtConstructor<*>>() {
    override fun SearchParameters.getTarget(): KtConstructor<*>? = getUnwrappedTarget() as? KtConstructor<*>
    override fun KtConstructor<*>.toLightSymbols(): List<SwiftInitializerSymbol> =
        findSymbols(SwiftLanguageKind).filterIsInstance<SwiftInitializerSymbol>()

    override fun createWrapper(target: KtConstructor<*>, symbol: SwiftInitializerSymbol) = KtSwiftSymbolPsiWrapper(target, symbol)
    override val SwiftInitializerSymbol.word: String? get() = this.targetClass?.name
}