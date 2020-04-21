package org.jetbrains.konan.resolve

import com.intellij.psi.ResolveState
import com.intellij.util.CommonProcessors
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCMembersContainer
import com.jetbrains.swift.codeinsight.resolve.processor.CollectingSymbolProcessor
import com.jetbrains.swift.codeinsight.resolve.processor.SwiftAbstractSymbolProcessor.ALL_DECLARATION_KINDS
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.konan.resolve.konan.KonanConsumer
import org.jetbrains.konan.resolve.konan.KonanTarget.Companion.PRODUCT_MODULE_NAME_KEY
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

fun KtNamedDeclaration.findSymbols(kind: OCLanguageKind): List<OCSymbol> {
    val containingFile = containingKtFile
    if (containingFile.isCompiled) return emptyList()

    val offset = textOffset // we have to use same offset as in `org.jetbrains.konan.resolve.symbols.KtSymbolUtilKt.getOffset`
    if (kind == SwiftLanguageKind && this is KtClassOrObject) {
        // In swift, nested classes more than 2 levels deep are all children of the topmost class
        var topParent: KtClassOrObject? = containingClassOrObject
        while (true) {
            topParent = topParent?.containingClassOrObject ?: break
        }
        topParent?.findMemberSymbols(offset, kind)?.let { return it }
    }
    if (kind == SwiftLanguageKind || this !is KtClassOrObject || this is KtEnumEntry) {
        containingClassOrObject?.findMemberSymbols(offset, kind)?.let { return it }
    }
    return findGlobalSymbols(containingFile, offset, kind)
}

private fun KtClassOrObject.findMemberSymbols(startOffset: Int, kind: OCLanguageKind): List<OCSymbol> =
    findSymbols(kind)
        .flatMap { classSymbol ->
            when (classSymbol) {
                is OCMembersContainer<*> -> classSymbol.findMemberSymbols(startOffset)
                is SwiftTypeSymbol -> classSymbol.findMemberSymbols(startOffset)
                else -> emptyList()
            }
        }

private fun OCMembersContainer<*>.findMemberSymbols(offset: Int): Collection<OCSymbol> =
    object : CommonProcessors.CollectProcessor<OCSymbol>() {
        override fun accept(member: OCSymbol): Boolean = member.offset == offset
    }.also { processMembers(null, it) }.results

private fun SwiftTypeSymbol.findMemberSymbols(offset: Int): List<SwiftSymbol> =
    CollectingSymbolProcessor<SwiftSymbol>(null, null, ALL_DECLARATION_KINDS) {
        it.offset == offset
    }.also { processMembers(it, ResolveState.initial()) }.collectedSymbols

private fun findGlobalSymbols(containingFile: KtFile, offset: Int, kind: OCLanguageKind): List<OCSymbol> {
    val tables = hashSetOf<FileSymbolTable>()
    val symbols = hashSetOf<OCSymbol>()
    val results = mutableListOf<OCSymbol>()
    val project = containingFile.project
    for (konanTarget in KonanConsumer.getAllReferencedKonanTargets(project)) {
        val context = OCInclusionContext.empty(kind, containingFile)
        context.define(PRODUCT_MODULE_NAME_KEY, konanTarget.productModuleName)

        val tableContents = FileSymbolTable.forFile(containingFile, context)?.takeIf { tables.add(it) }?.contents ?: emptyList()
        for (symbol in tableContents) {
            if (symbol.offset == offset && symbols.add(symbol)) results.add(symbol)
        }
    }
    return results
}
