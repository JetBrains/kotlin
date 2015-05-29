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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.JetNodeType

public abstract class JetExpressionImpl(node: ASTNode) : JetElementImpl(node), JetExpression {

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: JetNodeType): JetExpression? {
        val containerNode = findChildByType<JetContainerNode>(type) ?: return null
        return containerNode.findChildByClass<JetExpression>(javaClass())
    }

    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement, { super<JetElementImpl>.replace(it) })
    }

    companion object {
        fun replaceExpression(expression: JetExpression, newElement: PsiElement, rawReplaceHandler: (PsiElement) -> PsiElement): PsiElement {
            val parent = expression.getParent()

            if (newElement is JetExpression) {
                when (parent) {
                    is JetExpression -> {
                        if (JetPsiUtil.areParenthesesNecessary(newElement, expression, parent)) {
                            return rawReplaceHandler(JetPsiFactory(expression).createExpressionByPattern("($0)", newElement))
                        }
                    }

                    is JetSimpleNameStringTemplateEntry -> {
                        if (newElement !is JetSimpleNameExpression) {
                            val newEntry = parent.replace(JetPsiFactory(expression).createBlockStringTemplateEntry(newElement)) as JetBlockStringTemplateEntry
                            return newEntry.getExpression()!!
                        }
                    }
                }
            }

            return rawReplaceHandler(newElement)
        }
    }
}
