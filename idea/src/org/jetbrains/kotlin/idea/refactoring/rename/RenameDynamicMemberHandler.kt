/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic

class RenameDynamicMemberHandler : KotlinVariableInplaceRenameHandler() {
    override fun isAvailable(element: PsiElement?, editor: Editor, file: PsiFile): Boolean {
        val callee = PsiTreeUtil.findElementOfClassAtOffset(
            file, editor.caretModel.offset, KtSimpleNameExpression::class.java, false
        ) ?: return false
        val calleeDescriptor = callee.resolveToCall()?.resultingDescriptor ?: return false
        return calleeDescriptor.isDynamic()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        super.invoke(project, editor, file, dataContext)
        editor?.let {
            CodeInsightUtils.showErrorHint(
                project,
                it,
                KotlinBundle.message("text.rename.not.applicable.to.dynamically.invoked.methods"),
                RefactoringBundle.message("rename.title"),
                null
            )
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        // Do nothing: this method is called not from editor
    }
}
