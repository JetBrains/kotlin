/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}
