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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.intentions.getLambdaByImplicitItReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class KotlinHighlightImplicitItHandlerFactory : HighlightUsagesHandlerFactoryBase() {
    override fun createHighlightUsagesHandler(editor: Editor, file: PsiFile, target: PsiElement): HighlightUsagesHandlerBase<*>? {
        if (!(target is LeafPsiElement && target.elementType == KtTokens.IDENTIFIER)) return null
        val refExpr = target.parent as? KtNameReferenceExpression ?: return null
        val lambda = getLambdaByImplicitItReference(refExpr) ?: return null
        return object : HighlightUsagesHandlerBase<KtNameReferenceExpression>(editor, file) {
            override fun getTargets() = listOf(refExpr)

            override fun selectTargets(
                targets: MutableList<out KtNameReferenceExpression>,
                selectionConsumer: Consumer<in MutableList<out KtNameReferenceExpression>>
            ) = selectionConsumer.consume(targets)

            override fun computeUsages(targets: MutableList<out KtNameReferenceExpression>) {
                lambda.accept(
                    object : KtTreeVisitorVoid() {
                        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                            if (expression is KtNameReferenceExpression && getLambdaByImplicitItReference(expression) == lambda) {
                                addOccurrence(expression)
                            }
                        }
                    }
                )
            }
        }
    }
}