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

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class AbstractReferenceSubstitutionRenameHandler<D : DeclarationDescriptor> : PsiElementRenameHandler() {
    private val inplaceRenameHandler = MemberInplaceRenameHandler()

    protected fun getReferenceExpression(dataContext: DataContext): KtSimpleNameExpression? {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return null
        val ktFile = CommonDataKeys.PSI_FILE.getData(dataContext) as? KtFile ?: return null
        var elementAtCaret = ktFile.findElementAt(caret.offset)
        if (elementAtCaret is PsiWhiteSpace) {
            elementAtCaret = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceAfter(ktFile, caret.offset)
        }
        return elementAtCaret?.getNonStrictParentOfType<KtSimpleNameExpression>()
    }

    protected abstract fun getTargetDescriptor(dataContext: DataContext): D?

    protected open fun getElementToRename(project: Project, descriptor: D): PsiElement? {
        return DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
    }

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        return CommonDataKeys.EDITOR.getData(dataContext) != null && getTargetDescriptor(dataContext) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        val descriptor = getTargetDescriptor(dataContext) ?: return
        val wrappingContext = DataContext { id ->
            if (CommonDataKeys.PSI_ELEMENT.`is`(id)) return@DataContext getElementToRename(project, descriptor)
            dataContext.getData(id)
        }
        // Can't provide new name for inplace refactoring in unit test mode
        if (!ApplicationManager.getApplication().isUnitTestMode && inplaceRenameHandler.isAvailableOnDataContext(wrappingContext)) {
            inplaceRenameHandler.invoke(project, editor, file, wrappingContext)
        }
        else {
            super.invoke(project, editor, file, wrappingContext)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        // Can't be invoked outside of a text editor
    }
}