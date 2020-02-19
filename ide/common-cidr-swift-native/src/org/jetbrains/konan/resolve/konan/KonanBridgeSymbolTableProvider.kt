/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.jetbrains.cidr.CidrLog
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCSymbolTablesBuildingActivity
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.konan.resolve.translation.KtFileTranslator
import org.jetbrains.konan.resolve.translation.KtFrameworkTranslator

class KonanBridgeSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KonanBridgePsiFile
    override fun isSource(project: Project, virtualFile: VirtualFile): Boolean = virtualFile is KonanBridgeVirtualFile
    override fun isSource(project: Project, virtualFile: VirtualFile, inclusionContext: OCInclusionContext): Boolean =
        KtFileTranslator.isSupported(inclusionContext) && isSource(project, virtualFile)

    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean = false

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        CidrLog.LOG.error("should not be called for this file: " + file.name)
        return FileSymbolTable(virtualFile, ContextSignature())
    }

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        virtualFile as KonanBridgeVirtualFile

        val signature = ContextSignature(context.languageKind, emptyMap(), emptySet(), emptyList(), false, null, false)
        val result = FileSymbolTable(virtualFile, signature)

        val fileTranslator = KtFileTranslator.createTranslator(context)
        KtFrameworkTranslator.translateModule(context.project, virtualFile, fileTranslator).forEach { result.append(it) }

        return result
    }

    override fun getItemProviderAndWorkerForAdditionalSymbolLoading(
        project: Project,
        indicator: ProgressIndicator,
        allFiles: Collection<VirtualFile>
    ): OCSymbolTablesBuildingActivity.AdditionalTaskProvider<*>? {
        return null
    }
}