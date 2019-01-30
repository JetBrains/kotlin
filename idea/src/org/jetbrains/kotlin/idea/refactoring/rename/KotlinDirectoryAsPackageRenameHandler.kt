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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.DirectoryAsPackageRenameHandler
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger

class KotlinDirectoryAsPackageRenameHandler : DirectoryAsPackageRenameHandler() {
    override fun isIdentifier(name: String, project: Project): Boolean = name.quoteIfNeeded().isIdentifier()
    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        super.invoke(project, elements, dataContext)
        KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeRefactoringTrigger, this.javaClass.simpleName)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        super.invoke(project, editor, file, dataContext)
        KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeRefactoringTrigger, this.javaClass.simpleName)
    }

}