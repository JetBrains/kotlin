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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

class IntroduceVariableIntention : SelfTargetingRangeIntention<PsiElement>(
    PsiElement::class.java, CodeInsightBundle.message("intention.introduce.variable.text")
), HighPriorityAction {
    private fun getExpressionToProcess(element: PsiElement): KtExpression? {
        if (element is PsiFileSystemItem) return null
        val startElement = PsiTreeUtil.skipSiblingsBackward(element, PsiWhiteSpace::class.java) ?: element
        return startElement.parentsWithSelf
            .filterIsInstance<KtExpression>()
            .takeWhile { it !is KtDeclarationWithBody }
            .firstOrNull {
                val parent = it.parent
                parent is KtBlockExpression || parent is KtDeclarationWithBody && !parent.hasBlockBody() && parent.bodyExpression == it
            }
    }

    override fun applicabilityRange(element: PsiElement): TextRange? {
        val expression = getExpressionToProcess(element) ?: return null
        val type = expression.analyze().getType(expression)
        if (type == null || type.isUnit() || type.isNothing()) return null
        return element.textRange
    }

    override fun applyTo(element: PsiElement, editor: Editor?) {
        val expression = getExpressionToProcess(element) ?: return
        KotlinIntroduceVariableHandler.doRefactoring(
            element.project, editor, expression, isVar = false, occurrencesToReplace = null, onNonInteractiveFinish = null
        )
    }
}