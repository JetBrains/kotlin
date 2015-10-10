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
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetStatementExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class KotlinExpressionTypeProvider : ExpressionTypeProvider<JetExpression>() {
    override fun getExpressionsAt(elementAt: PsiElement): List<JetExpression> =
            elementAt.parentsWithSelf.filterIsInstance<JetExpression>().filterNot { it.shouldSkip() }.toArrayList()

    private fun JetExpression.shouldSkip(): Boolean {
        return this is JetStatementExpression && this !is JetFunction
    }

    override fun getInformationHint(element: JetExpression): String {
        val type = ShowExpressionTypeAction.typeByExpression(element) ?: return "Type is unknown"
        return ShowExpressionTypeAction.renderTypeHint(type)
    }

    override fun getErrorHint(): String = "No expression found"
}
