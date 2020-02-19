/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.konan.resolve.konan.KonanBridgeVirtualFile
import org.jetbrains.konan.resolve.translation.KtFileTranslator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KtSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KtFile

    //todo[medvedev] check if the source is from common or ios module
    override fun isSource(project: Project, file: VirtualFile): Boolean =
        FileTypeManager.getInstance().isFileOfType(file, KotlinFileType.INSTANCE)

    //todo[medvedev] check if the source is from common or ios module
    override fun isSource(project: Project, file: VirtualFile, inclusionContext: OCInclusionContext): Boolean =
        KtFileTranslator.isSupported(inclusionContext) && getBridgeFile(inclusionContext) != null && isSource(project, file)

    //todo[medvedev] proper check for out of code block modification
    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean = true

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        CidrLog.LOG.error("should not be called for this file: " + file.name)
        return FileSymbolTable(virtualFile, ContextSignature())
    }

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        val target = checkNotNull(getBridgeFile(context)).target

        val signature = ContextSignature(context.languageKind, emptyMap(), emptySet(), emptyList(), false, null, false)
        val table = FileSymbolTable(virtualFile, signature)
        val project = context.project
        val psi = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile ?: return table

        val fileTranslator = KtFileTranslator.createTranslator(context)
        fileTranslator.translate(psi, target).forEach { symbol -> table.append(symbol) }

        return table
    }

    private fun getBridgeFile(context: OCInclusionContext): KonanBridgeVirtualFile? = context.processedFiles.firstIsInstanceOrNull()
}