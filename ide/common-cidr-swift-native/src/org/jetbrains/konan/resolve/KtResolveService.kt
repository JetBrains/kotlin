package org.jetbrains.konan.resolve

import com.intellij.psi.ResolveState
import com.intellij.util.CommonProcessors
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCMembersContainer
import com.jetbrains.swift.codeinsight.resolve.processor.CollectingSymbolProcessor
import com.jetbrains.swift.codeinsight.resolve.processor.SwiftAbstractSymbolProcessor.ALL_DECLARATION_KINDS
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.psi.types.SwiftPlace
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.SwiftTypeSymbol
import org.jetbrains.konan.resolve.konan.KonanConsumer
import org.jetbrains.konan.resolve.konan.KonanTarget.Companion.PRODUCT_MODULE_NAME_KEY
import org.jetbrains.konan.resolve.symbols.objc.KtOCClassSymbol
import org.jetbrains.konan.resolve.symbols.swift.KtSwiftClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.abbreviate
import org.jetbrains.kotlin.backend.konan.objcexport.createNamer
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.source.PsiSourceFile

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

    if (parent is KtFile && this !is KtClassOrObject) {
        return (parent as KtFile).findFileClassMemberSymbols(offset, kind)
    }

    return findGlobalSymbols(containingFile, kind) { symbols, _ ->
        symbols.filter { it.offset == offset }
    }
}

private fun KtFile.findFileClassMemberSymbols(startOffset: Int, kind: OCLanguageKind): List<OCSymbol> =
    findFileClassSymbols(kind).flatMap { it.findMemberSymbols(startOffset) }

private fun KtClassOrObject.findMemberSymbols(startOffset: Int, kind: OCLanguageKind): List<OCSymbol> =
    findSymbols(kind).flatMap { it.findMemberSymbols(startOffset) }

private fun OCSymbol.findMemberSymbols(startOffset: Int): Collection<OCSymbol> =
    when (this) {
        is OCMembersContainer<*> -> findMemberSymbols(offset = startOffset)
        is SwiftTypeSymbol -> findMemberSymbols(startOffset)
        else -> emptyList()
    }

private fun OCMembersContainer<*>.findMemberSymbols(offset: Int): Collection<OCSymbol> =
    object : CommonProcessors.CollectProcessor<OCSymbol>() {
        override fun accept(member: OCSymbol): Boolean = member.offset == offset
    }.also { processMembers(null, it) }.results

private fun SwiftTypeSymbol.findMemberSymbols(offset: Int): List<SwiftSymbol> =
    CollectingSymbolProcessor<SwiftSymbol>(null, SwiftPlace.Companion.of(project), ALL_DECLARATION_KINDS) {
        it.offset == offset
    }.also { processMembers(it, ResolveState.initial()) }.collectedSymbols

internal fun KtFile.findFileClassSymbols(kind: OCLanguageKind): List<OCSymbol> {
    val descriptor = module?.toDescriptor() ?: return emptyList()
    return findGlobalSymbols(this, kind) { symbols, productModuleName ->
        val namer = createNamer(descriptor, abbreviate(productModuleName))
        val fileClassName = namer.getFileClassName(PsiSourceFile(this))
        val name = when (kind) {
            is SwiftLanguageKind -> fileClassName.swiftName
            else -> fileClassName.objCName
        }
        return@findGlobalSymbols listOfNotNull(symbols.firstOrNull { it.name == name })
    }
}

private fun findGlobalSymbols(
    containingFile: KtFile,
    kind: OCLanguageKind,
    matcher: (List<OCSymbol>, String) -> List<OCSymbol>
): List<OCSymbol> {
    val tables = hashSetOf<FileSymbolTable>()
    val symbols = hashSetOf<OCSymbol>()
    val results = mutableListOf<OCSymbol>()
    val project = containingFile.project
    for (konanTarget in KonanConsumer.getAllReferencedKonanTargets(project)) {
        val context = OCInclusionContext.empty(kind, containingFile)
        context.define(PRODUCT_MODULE_NAME_KEY, konanTarget.productModuleName)

        val tableContents = FileSymbolTable.forFile(containingFile, context)?.takeIf { tables.add(it) }?.contents ?: emptyList()
        matcher(tableContents, konanTarget.productModuleName).filter { symbols.add(it) }.forEach { results.add(it) }
    }
    return results
}
