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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class RenameBackingFieldReferenceHandler: KotlinVariableInplaceRenameHandler() {
    override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
        val refExpression = file.findElementForRename<KtSimpleNameExpression>(editor.caretModel.offset) ?: return false
        if (refExpression.text != "field") return false
        return refExpression.resolveToCall()?.resultingDescriptor is SyntheticFieldDescriptor
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        CodeInsightUtils.showErrorHint(project, editor, "Rename is not applicable to backing field reference", "Rename", null)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // Do nothing: this method is called not from editor
    }
}
