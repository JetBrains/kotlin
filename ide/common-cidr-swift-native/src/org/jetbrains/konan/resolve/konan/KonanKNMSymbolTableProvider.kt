package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.kotlin.ide.konan.decompiler.KotlinNativeMetaFileType

class KonanKNMSymbolTableProvider : SymbolTableProvider() {
    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean = false

    override fun calcTableUsingPSI(psiFile: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable = empty(virtualFile)

    private fun empty(virtualFile: VirtualFile) = FileSymbolTable(virtualFile, ContextSignature())

    override fun isSource(psiFile: PsiFile): Boolean = psiFile.virtualFile.fileType is KotlinNativeMetaFileType

    override fun isSource(project: Project, virtualFile: VirtualFile): Boolean =
        FileTypeManager.getInstance().isFileOfType(virtualFile, KotlinNativeMetaFileType)

    override fun isSource(project: Project, virtualFile: VirtualFile, inclusionContext: OCInclusionContext): Boolean =
        isSource(project, virtualFile)

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable = empty(virtualFile)
}