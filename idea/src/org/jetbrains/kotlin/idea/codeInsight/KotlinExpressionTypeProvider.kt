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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.actions.ShowExpressionTypeAction
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtStatementExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class KotlinExpressionTypeProvider : ExpressionTypeProvider<KtExpression>() {
    override fun getExpressionsAt(elementAt: PsiElement): List<KtExpression> =
            elementAt.parentsWithSelf.filterIsInstance<KtExpression>().filterNot { it.shouldSkip() }.toArrayList()

    private fun KtExpression.shouldSkip(): Boolean {
        return this is KtStatementExpression && this !is KtFunction
    }

    override fun getInformationHint(element: KtExpression): String {
        val type = ShowExpressionTypeAction.typeByExpression(element) ?: return "Type is unknown"
        return ShowExpressionTypeAction.renderTypeHint(type)
    }

    override fun getErrorHint(): String = "No expression found"
}
