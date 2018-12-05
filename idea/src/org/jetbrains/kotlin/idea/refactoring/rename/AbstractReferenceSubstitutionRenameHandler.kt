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

import com.intellij.internal.statistic.service.fus.collectors.FUSApplicationUsageTrigger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.statistics.KotlinIdeRefactoringTrigger

abstract class AbstractReferenceSubstitutionRenameHandler(
        private val delegateHandler: RenameHandler = MemberInplaceRenameHandler()
) : PsiElementRenameHandler() {
    companion object {
        fun getReferenceExpression(file: PsiFile, offset: Int): KtSimpleNameExpression? {
            var elementAtCaret = file.findElementAt(offset) ?: return null
            if (elementAtCaret.node?.elementType == KtTokens.AT) return null
            if (elementAtCaret is PsiWhiteSpace) {
                elementAtCaret = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceAfter(file, offset) ?: return null
                if (offset != elementAtCaret.endOffset) return null
            }
            return elementAtCaret.getNonStrictParentOfType<KtSimpleNameExpression>()
        }

        fun getReferenceExpression(dataContext: DataContext): KtSimpleNameExpression? {
            val caret = CommonDataKeys.CARET.getData(dataContext) ?: return null
            val ktFile = CommonDataKeys.PSI_FILE.getData(dataContext) as? KtFile ?: return null
            return getReferenceExpression(ktFile, caret.offset)
        }
    }

    protected abstract fun getElementToRename(dataContext: DataContext): PsiElement?

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        return CommonDataKeys.EDITOR.getData(dataContext) != null && getElementToRename(dataContext) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        val elementToRename = getElementToRename(dataContext) ?: return

        FUSApplicationUsageTrigger.getInstance().trigger(KotlinIdeRefactoringTrigger::class.java, this::class.java.name)

        val wrappingContext = DataContext { id ->
            if (CommonDataKeys.PSI_ELEMENT.`is`(id)) return@DataContext elementToRename
            dataContext.getData(id)
        }
        // Can't provide new name for inplace refactoring in unit test mode
        if (!ApplicationManager.getApplication().isUnitTestMode && delegateHandler.isAvailableOnDataContext(wrappingContext)) {
            delegateHandler.invoke(project, editor, file, wrappingContext)
        } else {
            super.invoke(project, editor, file, wrappingContext)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        // Can't be invoked outside of a text editor
        FUSApplicationUsageTrigger.getInstance().trigger(KotlinIdeRefactoringTrigger::class.java, this::class.java.name)
    }
}