// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

class KotlinDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (KotlinDecompiledFileViewProvider) -> KtDecompiledFile?
) : SingleRootFileViewProvider(manager, file, physical, KotlinLanguage.INSTANCE) {
    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, KotlinFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }

    override fun createCopy(copy: VirtualFile) = KotlinDecompiledFileViewProvider(manager, copy, false, factory)

    override fun getContents() = content.get()
}