/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledTextIndexer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

open class KtDecompiledFile(
    private val provider: KotlinDecompiledFileViewProvider,
    buildDecompiledText: (VirtualFile) -> DecompiledText
) : KtFile(provider, true) {

    private val decompiledText = LockedClearableLazyValue(Any()) {
        buildDecompiledText(provider.virtualFile)
    }

    override fun getText(): String? {
        return decompiledText.get().text
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }

    fun <T : Any> getDeclaration(indexer: DecompiledTextIndexer<T>, key: T): KtDeclaration? {
        val range = decompiledText.get().index.getRange(indexer, key) ?: return null
        return PsiTreeUtil.findElementOfClassAtRange(this@KtDecompiledFile, range.startOffset, range.endOffset, KtDeclaration::class.java)
    }

    fun <T : Any> hasDeclarationWithKey(indexer: DecompiledTextIndexer<T>, key: T): Boolean {
        return decompiledText.get().index.getRange(indexer, key) != null
    }
}
