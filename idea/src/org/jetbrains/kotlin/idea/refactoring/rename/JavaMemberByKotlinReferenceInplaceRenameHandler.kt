/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

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
        KotlinFUSLogger.log(FUSEventGroups.Refactoring, this::class.java.name)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        super.invoke(project, editor, file, dataContext)
        KotlinFUSLogger.log(FUSEventGroups.Refactoring, this::class.java.name)
    }

}
