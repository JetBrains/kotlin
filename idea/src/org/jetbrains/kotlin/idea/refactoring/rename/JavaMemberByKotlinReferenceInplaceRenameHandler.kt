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
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger

class JavaMemberByKotlinReferenceInplaceRenameHandler : MemberInplaceRenameHandler() {
    class Renamer(
            elementToRename: PsiNameIdentifierOwner,
            element: PsiElement,
            editor: Editor
    ) : MemberInplaceRenamer(elementToRename, element, editor) {
        override fun performRefactoringRename(newName: String, markAction: StartMarkAction) {
            super.performRefactoringRename(KtPsiUtil.unquoteIdentifier(newName), markAction)
        }
    }

    override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
        if (!super.isAvailable(element, editor, file)) return false
        if (element?.unwrapped !is PsiMember) return false
        val refExpr = file.findElementAt(editor.caretModel.offset)?.getNonStrictParentOfType<KtSimpleNameExpression>() ?: return false
        if (refExpr.references.any { (it as? SyntheticPropertyAccessorReference)?.resolve() != null }) return false
        if (refExpr.mainReference.getImportAlias() != null) return false
        return true
    }

    override fun createMemberRenamer(element: PsiElement, elementToRename: PsiNameIdentifierOwner, editor: Editor): MemberInplaceRenamer {
        return Renamer(elementToRename, element, editor)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        super.invoke(project, elements, dataContext)
        KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeRefactoringTrigger, this::class.java.name)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        super.invoke(project, editor, file, dataContext)
        KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeRefactoringTrigger, this::class.java.name)
    }

}
