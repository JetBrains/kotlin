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
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanConsumer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

fun KtNamedDeclaration.findSymbols(kind: OCLanguageKind): List<OCSymbol> {
    val containingFile = containingKtFile
    if (containingFile.isCompiled) return emptyList()

    val offset = textOffset // we have to use same offset as in `org.jetbrains.konan.resolve.symbols.KtSymbolUtilKt.getOffset`
    if (this !is KtClassOrObject) {
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
        val bridgeFile = konanTarget.let { target ->
            KonanBridgeFileManager.getInstance(project).forTarget(target, target.productModuleName.let { "$it/$it.h" })
        }

        val context = OCInclusionContext.empty(kind, containingFile)
        context.addProcessedFile(bridgeFile)

        val tableContents = FileSymbolTable.forFile(containingFile, context)?.takeIf { tables.add(it) }?.contents ?: emptyList()
        for (symbol in tableContents) {
            if (symbol.offset == offset && symbols.add(symbol)) results.add(symbol)
        }
    }
    return results
}
