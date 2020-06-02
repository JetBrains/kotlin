package org.jetbrains.konan.resolve.symbols

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.symbols.DeepEqual
import com.jetbrains.cidr.lang.symbols.OCSymbol.UNNAMED
import com.jetbrains.cidr.lang.symbols.OCSymbolKind
import gnu.trove.TObjectLongHashMap

internal class KtDependencyMarker(private val containingFile: VirtualFile, val dependencies: TObjectLongHashMap<VirtualFile>) : KtSymbol {
    override fun getKind(): OCSymbolKind = OCSymbolKind.FOREIGN_ELEMENT
    override fun getContainingFile(): VirtualFile = containingFile
    override fun getName(): String = UNNAMED
    override fun locateDefinition(project: Project): PsiElement? = null
    override fun isGlobal(): Boolean = false
    override fun getComplexOffset(): Long = 0
    override fun hashCodeExcludingOffset(): Int = containingFile.hashCode()
    override fun deepEqualStep(c: DeepEqual.Comparator, first: Any, second: Any): Boolean = first === second
}