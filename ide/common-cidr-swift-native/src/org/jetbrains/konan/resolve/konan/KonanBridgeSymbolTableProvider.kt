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
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.cpp.OCMacroSymbol
import com.jetbrains.cidr.lang.symbols.symtable.ContextSignature
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCSymbolTablesBuildingActivity
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import com.jetbrains.cidr.lang.util.OCImmutableList
import org.jetbrains.konan.resolve.konan.KonanTarget.Companion.PRODUCT_MODULE_NAME_KEY
import org.jetbrains.konan.resolve.translation.KtFileTranslator
import org.jetbrains.konan.resolve.translation.KtFrameworkTranslator

class KonanBridgeSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KonanBridgePsiFile
    override fun isSource(project: Project, virtualFile: VirtualFile): Boolean = virtualFile is KonanBridgeVirtualFile
    override fun isSource(project: Project, virtualFile: VirtualFile, inclusionContext: OCInclusionContext): Boolean =
        isSource(project, virtualFile) && KtFileTranslator.isSupported(inclusionContext)

    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean = false

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable =
        throw UnsupportedOperationException()

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        virtualFile as KonanBridgeVirtualFile

        val signature = ContextSignature(
            context.languageKind,
            emptyMap(), emptySet(),
            context.currentNamespace,
            context.isSurrogate,
            null, false
        )
        val table = FileSymbolTable(virtualFile, signature)
        if (context.isSurrogate) return table // don't build surrogate tables

        table.append(OCMacroSymbol(null, 0, PRODUCT_MODULE_NAME_KEY, OCImmutableList.emptyList(), virtualFile.target.productModuleName))

        val fileTranslator = KtFileTranslator.createTranslator(context)
        KtFrameworkTranslator.translateModule(context.project, virtualFile, fileTranslator).forEach { table.append(it) }

        return table
    }

    override fun getItemProviderAndWorkerForAdditionalSymbolLoading(
        project: Project,
        indicator: ProgressIndicator,
        allFiles: Collection<VirtualFile>
    ): OCSymbolTablesBuildingActivity.AdditionalTaskProvider<*>? {
        return null
    }
}