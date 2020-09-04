package org.jetbrains.konan.findUsages

import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.cidr.lang.CLanguageKind
import org.jetbrains.konan.resolve.findFileClassSymbols
import org.jetbrains.konan.resolve.symbols.KtOCSymbolPsiWrapper
import org.jetbrains.konan.resolve.symbols.KtSymbolPsiWrapper
import org.jetbrains.konan.resolve.symbols.objc.KtOCClassSymbol
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast

class KotlinFileClassOCUsageSearcher : KotlinUsageSearcher<KtOCClassSymbol<*, *>, KtFile>() {
    override fun ReferencesSearch.SearchParameters.getTarget(): KtFile? = getUnwrappedTarget() as? KtFile
    override fun KtFile.toLightSymbols(): List<KtOCClassSymbol<*, *>> = findFileClassSymbols(CLanguageKind.OBJ_C).cast()
    override fun createWrapper(target: KtFile, symbol: KtOCClassSymbol<*, *>): KtSymbolPsiWrapper = KtOCSymbolPsiWrapper(target, symbol)
    override val KtOCClassSymbol<*, *>.word: String? get() = name
}