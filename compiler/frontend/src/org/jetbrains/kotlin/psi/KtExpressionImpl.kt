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
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.psi.psiUtil.canPlaceAfterSimpleNameEntry

public abstract class KtExpressionImpl(node: ASTNode) : KtElementImpl(node), KtExpression {

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: KtNodeType): KtExpression? {
        val containerNode = findChildByType<KtContainerNode>(type) ?: return null
        return containerNode.findChildByClass<KtExpression>(javaClass())
    }

    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement, { super<KtElementImpl>.replace(it) })
    }

    companion object {
        fun replaceExpression(expression: KtExpression, newElement: PsiElement, rawReplaceHandler: (PsiElement) -> PsiElement): PsiElement {
            val parent = expression.getParent()

            if (newElement is KtExpression) {
                when {
                    parent is KtStringTemplateEntryWithExpression &&
                    newElement is KtStringTemplateExpression &&
                    // Do not mix raw and non-raw templates
                    parent.parent.firstChild.text == newElement.firstChild.text -> {
                        val entriesToAdd = newElement.entries
                        val templateExpression = parent.parent as KtStringTemplateExpression
                        if (entriesToAdd.size > 0) {
                            templateExpression.addRangeBefore(entriesToAdd.first(), entriesToAdd.last(), parent)
                            val lastNewEntry = parent.prevSibling
                            val nextElement = parent.nextSibling
                            if (lastNewEntry is KtSimpleNameStringTemplateEntry &&
                                lastNewEntry.expression != null &&
                                !canPlaceAfterSimpleNameEntry(nextElement)) {
                                lastNewEntry.replace(KtPsiFactory(expression).createBlockStringTemplateEntry(lastNewEntry.expression!!))
                            }
                        }
                        parent.delete()
                    }

                    parent is KtExpression || parent is KtValueArgument -> {
                        if (KtPsiUtil.areParenthesesNecessary(newElement, expression, parent as KtElement)) {
                            return rawReplaceHandler(KtPsiFactory(expression).createExpressionByPattern("($0)", newElement))
                        }
                    }

                    parent is KtSimpleNameStringTemplateEntry -> {
                        if (newElement !is KtSimpleNameExpression) {
                            val newEntry = parent.replace(KtPsiFactory(expression).createBlockStringTemplateEntry(newElement)) as KtBlockStringTemplateEntry
                            return newEntry.getExpression()!!
                        }
                    }
                }
            }

            return rawReplaceHandler(newElement)
        }
    }
}
