package org.jetbrains.konan.resolve

import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import org.jetbrains.konan.forEachKonanFrameworkTarget
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanSwiftModule
import org.jetbrains.konan.resolve.konan.KonanTargetManager
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration

fun KtNamedDeclaration.findSymbols(): Sequence<OCSymbol> {
    if (!(this is KtClassOrObject || this is KtFunction)) return emptySequence()

    val offset = textOffset // we have to use same offset as in `org.jetbrains.konan.resolve.symbols.KtSymbolUtilKt.getOffset`
    return containingClass?.findMemberSymbols(offset) ?: findGlobalSymbols(offset)
}

private val KtNamedDeclaration.containingClass: KtClassOrObject?
    get() = (parent as? KtClassBody)?.parent as? KtClassOrObject

private fun KtClassOrObject.findMemberSymbols(startOffset: Int): Sequence<OCSymbol> =
    findSymbols()
        .filterIsInstance<OCClassSymbol>()
        .flatMap { classSymbol -> classSymbol.findMemberSymbols(startOffset) }

private fun OCClassSymbol.findMemberSymbols(offset: Int): Sequence<OCSymbol> {
    val result = mutableListOf<OCSymbol>()
    processMembers(null) { member ->
        if (member.offset == offset) {
            result += member
        }
        true
    }
    return result.asSequence()
}

private fun KtNamedDeclaration.findGlobalSymbols(offset: Int): Sequence<OCSymbol> = sequence<OCSymbol> {
    val tables = hashSetOf<FileSymbolTable>()
    val symbols = hashSetOf<OCSymbol>()
    forEachKonanFrameworkTarget(project) { moduleId, artifact ->
        val konanTarget = KonanTargetManager.getInstance(project).forArtifact(moduleId, artifact) ?: return@forEachKonanFrameworkTarget
        val bridgeFile = konanTarget.let { target ->
            KonanBridgeFileManager.getInstance(project).forTarget(target, target.productModuleName.let { "$it/$it.h" })
        }

        val context = OCInclusionContext.empty(CLanguageKind.OBJ_C, containingFile)
        context.addProcessedFile(bridgeFile)

        val tableContents = FileSymbolTable.forFile(containingFile, context)?.takeIf { tables.add(it) }?.contents ?: emptyList()
        for (symbol in tableContents) {
            if (symbol.offset == offset && symbols.add(symbol)) yield(symbol)
        }
    }
}
