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

package org.jetbrains.kotlin.idea.refactoring.copy

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.copy.CopyHandlerDelegateBase
import org.jetbrains.kotlin.psi.KtFile

class CopyKotlinFileHandler : CopyHandlerDelegateBase() {
    private val delegate = CopyFilesOrDirectoriesHandler()

    private fun adjustElements(elements: Array<out PsiElement>): Array<PsiElement>? {
        return elements
                .map { (if (it.isValid) it.containingFile as? KtFile else null) ?: return null }
                .toTypedArray()
    }

    override fun canCopy(elements: Array<out PsiElement>, fromUpdate: Boolean): Boolean {
        return delegate.canCopy(adjustElements(elements) ?: return false, fromUpdate)
    }

    override fun doCopy(elements: Array<out PsiElement>, defaultTargetDirectory: PsiDirectory?) {
        return delegate.doCopy(adjustElements(elements) ?: return, defaultTargetDirectory)
    }

    override fun doClone(element: PsiElement?) = delegate.doClone(element)
}

