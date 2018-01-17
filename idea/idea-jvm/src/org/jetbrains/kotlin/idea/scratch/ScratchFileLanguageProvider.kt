/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandler

abstract class ScratchFileLanguageProvider {
    abstract fun createFile(psiFile: PsiFile): ScratchFile?
    abstract fun createReplExecutor(file: ScratchFile): ScratchExecutor?

    abstract fun getOutputHandler(): ScratchOutputHandler

    companion object {
        private val EXTENSION = LanguageExtension<ScratchFileLanguageProvider>("org.jetbrains.kotlin.scratchFileLanguageProvider")

        fun get(language: Language): ScratchFileLanguageProvider? {
            return ScratchFileLanguageProvider.EXTENSION.forLanguage(language)
        }

        fun get(fileType: FileType): ScratchFileLanguageProvider? {
            return (fileType as? LanguageFileType)?.language?.let { get(it) }
        }

        fun createFile(psiFile: PsiFile): ScratchFile? {
            return get(psiFile.language)?.createFile(psiFile)
        }

    }
}