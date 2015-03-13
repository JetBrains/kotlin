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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameHandler
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.psi.JetSecondaryConstructor
import org.jetbrains.kotlin.psi.JetValueArgumentList


public class RenameOnSecondaryConstructorHandler : RenameHandler {
    override fun isAvailableOnDataContext(dataContext: DataContext?): Boolean {
        if (dataContext == null) return false

        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)

        val element = PsiTreeUtil.findElementOfClassAtOffsetWithStopSet(
                file, editor.getCaretModel().getOffset(), javaClass<JetSecondaryConstructor>(), false,
                javaClass<JetBlockExpression>(), javaClass<JetValueArgumentList>(), javaClass<JetParameterList>()
        )
        return element != null;
    }

    override fun isRenaming(dataContext: DataContext?): Boolean = isAvailableOnDataContext(dataContext)

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        CodeInsightUtils.showErrorHint(project, editor, "Rename is not applicable to secondary constructors", "Rename", null);
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // Do nothing: this method is called not from editor
    }
}
