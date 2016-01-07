/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

class KotlinDecompiledFileViewProvider(
        manager: PsiManager,
        file: VirtualFile,
        physical: Boolean,
        private val factory: (KotlinDecompiledFileViewProvider) -> KtDecompiledFile?
) : SingleRootFileViewProvider(manager, file, physical, KotlinLanguage.INSTANCE) {
    val content : LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, KotlinFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        DebugUtil.startPsiModification("Invalidating throw-away copy of file that was used for getting text")
        try {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }
        finally {
            DebugUtil.finishPsiModification()
        }

        text
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }

    override fun createCopy(copy: VirtualFile) = KotlinDecompiledFileViewProvider(manager, copy, false, factory)

    override fun getContents() = content.get()
}