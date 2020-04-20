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
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.OCContextSignatureBuilder
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import org.jetbrains.konan.resolve.konan.KonanTarget.Companion.PRODUCT_MODULE_NAME_KEY
import org.jetbrains.konan.resolve.translation.KtFileTranslator.Companion.isKtTranslationSupported
import org.jetbrains.konan.resolve.translation.KtFileTranslator.Companion.ktTranslator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener.Companion.getInsideCodeBlockModificationScope
import org.jetbrains.kotlin.psi.KtFile

class KtSymbolTableProvider : SymbolTableProvider() {
    override fun isSource(file: PsiFile): Boolean = file is KtFile

    //todo[medvedev] check if the source is from common or ios module
    override fun isSource(project: Project, file: VirtualFile): Boolean =
        FileTypeManager.getInstance().isFileOfType(file, KotlinFileType.INSTANCE)

    //todo[medvedev] check if the source is from common or ios module
    override fun isSource(project: Project, file: VirtualFile, inclusionContext: OCInclusionContext): Boolean =
        isSource(project, file) && inclusionContext.isKtTranslationSupported && inclusionContext.isDefined(PRODUCT_MODULE_NAME_KEY)

    override fun isOutOfCodeBlockChange(event: PsiTreeChangeEventImpl): Boolean =
        getInsideCodeBlockModificationScope(event.parent) == null

    override fun calcTableUsingPSI(file: PsiFile, virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable =
        throw UnsupportedOperationException()

    override fun calcTable(virtualFile: VirtualFile, context: OCInclusionContext): FileSymbolTable {
        CidrLog.LOG.assertTrue(!context.isSurrogate, "Surrogate symbol table requested for bridged Kotlin symbols")
        val signatureBuilder = OCContextSignatureBuilder(context.languageKind, context.currentNamespace, context.isSurrogate)
        val derived = context.deriveButDontCopyTypes(false).apply { setSignatureBuilder(signatureBuilder) }
        val frameworkName = requireNotNull(derived.getDefinition(PRODUCT_MODULE_NAME_KEY)).substitution

        val table = FileSymbolTable(virtualFile, signatureBuilder.build())
        val psi = PsiManager.getInstance(derived.project).findFile(virtualFile) as? KtFile ?: return table
        derived.ktTranslator.translate(psi, frameworkName, table.contents)
        return table
    }
}