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
import org.jetbrains.kotlin.idea.JetLanguage
import kotlin.properties.Delegates

public class KotlinJavascriptMetaFileViewProvider (
        manager: PsiManager,
        val file: VirtualFile,
        physical: Boolean,
        val isInternal: Boolean) : SingleRootFileViewProvider(manager, file, physical, JetLanguage.INSTANCE) {

    val jetJsMetaFile by Delegates.blockingLazy(this) {
        //TODO: check index that file is library file, as in ClassFileViewProvider
        if (!isInternal) KotlinJavascriptMetaFile(this) else null
    }

    override fun getContents(): CharSequence {
        return jetJsMetaFile?.getText() ?: ""
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? = jetJsMetaFile

    override fun createCopy(copy: VirtualFile): SingleRootFileViewProvider {
        return KotlinJavascriptMetaFileViewProvider(getManager(), copy, false, isInternal)
    }
}
