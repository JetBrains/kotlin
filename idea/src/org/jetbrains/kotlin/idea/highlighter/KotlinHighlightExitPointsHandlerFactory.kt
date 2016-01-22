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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class KotlinHighlightExitPointsHandlerFactory: HighlightUsagesHandlerFactoryBase() {
    companion object {
        private val RETURN_AND_THROW = TokenSet.create(KtTokens.RETURN_KEYWORD, KtTokens.THROW_KEYWORD)
    }

    override fun createHighlightUsagesHandler(editor: Editor, file: PsiFile, target: PsiElement): HighlightUsagesHandlerBase<*>? {
        if (target is LeafPsiElement && (target.elementType in RETURN_AND_THROW)) {
            val returnOrThrow = PsiTreeUtil.getParentOfType<KtExpression>(
                    target,
                    KtReturnExpression::class.java,
                    KtThrowExpression::class.java
            ) ?: return null

            return MyHandler(editor, file, returnOrThrow)
        }
        return null
    }

    private class MyHandler(editor: Editor, file: PsiFile, val target: KtExpression) : HighlightUsagesHandlerBase<PsiElement>(editor, file) {
        override fun getTargets() = listOf(target)

        override fun selectTargets(targets: MutableList<PsiElement>, selectionConsumer: Consumer<MutableList<PsiElement>>) {
            selectionConsumer.consume(targets)
        }

        override fun computeUsages(targets: MutableList<PsiElement>?) {
            val relevantFunction = target.getRelevantFunction()
            relevantFunction?.accept(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    element.acceptChildren(this)
                }

                private fun visitReturnOrThrow(expression: KtExpression) {
                    if (expression.getRelevantFunction() == relevantFunction) {
                        addOccurrence(expression)
                    }
                }

                override fun visitReturnExpression(expression: KtReturnExpression) {
                    visitReturnOrThrow(expression)
                }

                override fun visitThrowExpression(expression: KtThrowExpression) {
                    visitReturnOrThrow(expression)
                }
            })
        }
    }
}

private fun KtExpression.getRelevantFunction(): KtFunction? {
    if (this is KtReturnExpression) {
        (this.getTargetLabel()?.mainReference?.resolve() as? KtFunction)?.let { return it }
    }
    for (parent in parents) {
        if (InlineUtil.canBeInlineArgument(parent) && !InlineUtil.isInlinedArgument(parent as KtFunction, parent.analyze(), false)) {
            return parent
        }
    }
    return null
}