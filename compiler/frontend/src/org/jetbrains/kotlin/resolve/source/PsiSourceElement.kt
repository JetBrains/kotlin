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

package org.jetbrains.kotlin.resolve.source

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile

interface PsiSourceElement : SourceElement {
    val psi: PsiElement?

    override fun getContainingFile(): SourceFile = psi?.containingFile?.let { PsiSourceFile(it) } ?: SourceFile.NO_SOURCE_FILE
}

class PsiSourceFile(val psiFile: PsiFile): SourceFile {
    override fun equals(other: Any?): Boolean = other is PsiSourceFile && psiFile == other.psiFile
    override fun hashCode(): Int = psiFile.hashCode()
}
