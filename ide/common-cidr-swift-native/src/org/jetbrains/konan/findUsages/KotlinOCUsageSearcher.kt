package org.jetbrains.konan.findUsages

import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.symbols.OCSymbol
import org.jetbrains.konan.resolve.findSymbols
import org.jetbrains.konan.resolve.symbols.KtOCSymbolPsiWrapper
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KotlinOCUsageSearcher : KotlinUsageSearcher<OCSymbol, KtNamedDeclaration>() {
    override fun SearchParameters.getTarget(): KtNamedDeclaration? = getUnwrappedTarget() as? KtNamedDeclaration
    override fun KtNamedDeclaration.toLightSymbols(): List<OCSymbol> = findSymbols(CLanguageKind.OBJ_C)
    override fun createWrapper(target: KtNamedDeclaration, symbol: OCSymbol) = KtOCSymbolPsiWrapper(target, symbol)
    override val OCSymbol.word: String get() = name
}