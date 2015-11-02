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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.source.PsiFileImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

abstract class KotlinClassFileViewProviderBase(
        manager: PsiManager,
        file: VirtualFile,
        physical: Boolean) : SingleRootFileViewProvider(manager, file, physical, KotlinLanguage.INSTANCE) {
    val content : LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.getProject(), file, KotlinFileType.INSTANCE)
        val text = psiFile?.getText() ?: ""

        (psiFile as? PsiFileImpl)?.markInvalidated()

        text
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        // Workaround for KT-8344
        return super.createFile(project, file, fileType)
    }

    override fun getContents() = content.get()
}

public class KotlinClassFileViewProvider(
        manager: PsiManager,
        file: VirtualFile,
        physical: Boolean,
        val isInternal: Boolean) : KotlinClassFileViewProviderBase(manager, file, physical) {

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        val fileIndex = ServiceManager.getService(project, javaClass<FileIndexFacade>())
        if (!fileIndex.isInLibraryClasses(file) && fileIndex.isInSource(file)) {
            return null
        }

        if (isInternal) return null

        return KtClsFile(this)
    }

    override fun createCopy(copy: VirtualFile) = KotlinClassFileViewProvider(getManager(), copy, false, isInternal)
}

public class KotlinJavascriptMetaFileViewProvider (
        manager: PsiManager,
        val file: VirtualFile,
        physical: Boolean,
        val isInternal: Boolean) : KotlinClassFileViewProviderBase(manager, file, physical) {

    //TODO: check index that file is library file, as in ClassFileViewProvider
    override fun createFile(project: Project, file: VirtualFile, fileType: FileType) =
        if (!isInternal) KotlinJavascriptMetaFile(this) else null

    override fun createCopy(copy: VirtualFile) = KotlinJavascriptMetaFileViewProvider(getManager(), copy, false, isInternal)
}