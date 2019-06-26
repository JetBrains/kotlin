package org.jetbrains.konan.resolve

import com.intellij.psi.PsiFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCClassSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedDeclaration

fun KtNamedDeclaration.findSymbols(): List<OCSymbol> {
    if (!(this is KtClassOrObject || this is KtFunction)) return emptyList()

    val offset = textRange.startOffset
    return containingClass?.findMemberSymbols(offset) ?: findGlobalSymbols(offset)
}

private val KtNamedDeclaration.containingClass: KtClassOrObject?
    get() = (parent as? KtClassBody)?.parent as? KtClassOrObject

private fun KtClassOrObject.findMemberSymbols(startOffset: Int): List<OCSymbol> {
    val symbols = findSymbols()
    return symbols.asSequence()
        .filterIsInstance<OCClassSymbol>()
        .flatMap { classSymbol -> classSymbol.findMemberSymbols(startOffset) }
        .toList()
}

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

private fun KtNamedDeclaration.findGlobalSymbols(offset: Int): List<OCSymbol> {
    val table = containingFile?.getSymbolTable() ?: return emptyList()
    return table.contents.filter { symbol -> symbol.offset == offset }
}

private fun PsiFile.getSymbolTable(): FileSymbolTable? = FileSymbolTable.forFile(this, objcInclusionContext(this))

private fun objcInclusionContext(file: PsiFile): OCInclusionContext = OCInclusionContext.empty(CLanguageKind.OBJ_C, file)
