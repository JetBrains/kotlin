/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.konan.resolve.translation.KtFileTranslator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile

class KtSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KtFile

    override fun isSource(file: VirtualFile, cachedFileType: Lazy<FileType>): Boolean {
        //todo[medvedev] check if the source is from common or ios module
        return cachedFileType.value == KotlinFileType.INSTANCE
    }

    override fun onOutOfCodeBlockModification(project: Project, file: PsiFile?) {
        if (file != null && isSource(file)) {
            KtModificationCount.getInstance(project).inc()
        }
    }

    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean {
        //todo[medvedev] proper check for out of code block modification
        return true
    }

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        CidrLog.LOG.error("should not be called for this file: " + file.name)
        return FileSymbolTable(virtualFile, ContextSignature())
    }

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        val signature = ContextSignature(CLanguageKind.OBJ_C, emptyMap(), emptySet(), emptyList(), false)
        val table = FileSymbolTable(virtualFile, signature)
        val project = context.project
        val psi = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile ?: return table
        val translator = KtFileTranslator(project)

        translator.translate(psi).forEach { symbol -> table.append(symbol) }

        return table
    }
}