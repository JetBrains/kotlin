/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KotlinSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KtFile

    override fun isSource(file: VirtualFile, cachedFileType: Lazy<FileType>): Boolean = cachedFileType.value == KotlinFileType.INSTANCE

    override fun onOutOfCodeBlockModification(project: Project, file: PsiFile?) {
        if (file != null && isSource(file)) {
            KotlinModificationCount.getInstance(project).inc()
        }
    }

    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean {
        //proper check for out of code block modification
        return true
    }

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        return emptyTable(virtualFile)
    }

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        return emptyTable(virtualFile)
    }

    private fun emptyTable(virtualFile: VirtualFile): FileSymbolTable {
        return FileSymbolTable(virtualFile, ContextSignature())
    }
}