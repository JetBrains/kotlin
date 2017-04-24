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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.expressionComparedToNull
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class FoldInitializerAndIfToElvisInspection : IntentionBasedInspection<KtIfExpression>(FoldInitializerAndIfToElvisIntention::class)

class FoldInitializerAndIfToElvisIntention : SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' with elvis operator"){
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        val data = calcData(element) ?: return null

        val type = data.ifNullExpression.analyze().getType(data.ifNullExpression) ?: return null
        if (!type.isNothing()) return null

        val rParen = element.rightParenthesis ?: return null
        return TextRange(element.startOffset, rParen.endOffset)
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val (initializer, declaration, ifNullExpr, typeReference) = calcData(element)!!
        val factory = KtPsiFactory(element)

        val explicitTypeToSet = when {
            // for var with no explicit type, add it so that the actual change won't change
            declaration.isVar && declaration.typeReference == null -> initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)

            // for val with explicit type, change it to non-nullable
            !declaration.isVar && declaration.typeReference != null -> initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)?.makeNotNullable()

            else -> null
        }

        val childRangeBefore = PsiChildRange(declaration, element)
        val commentSaver = CommentSaver(childRangeBefore)
        val childRangeAfter = childRangeBefore.withoutLastStatement()

        val elvis = factory.createExpressionByPattern("$0 ?: $1", initializer, ifNullExpr) as KtBinaryExpression
        if (typeReference != null) {
            elvis.left!!.replace(factory.createExpressionByPattern("$0 as? $1", initializer, typeReference))
        }
        val newElvis = initializer.replaced(elvis)
        element.delete()

        if (explicitTypeToSet != null && !explicitTypeToSet.isError) {
            declaration.setType(explicitTypeToSet)
        }

        commentSaver.restore(childRangeAfter)

        editor?.caretModel?.moveToOffset(newElvis.right!!.textOffset)
    }

    private data class Data(
            val initializer: KtExpression,
            val declaration: KtVariableDeclaration,
            val ifNullExpression: KtExpression,
            val typeChecked: KtTypeReference? = null
    )

    private fun calcData(ifExpression: KtIfExpression): Data? {
        if (ifExpression.`else` != null) return null

        val operationExpression = ifExpression.condition as? KtOperationExpression ?: return null
        val value = when (operationExpression) {
            is KtBinaryExpression -> {
                if (operationExpression.operationToken != KtTokens.EQEQ) return null
                operationExpression.expressionComparedToNull()
            }
            is KtIsExpression -> {
                if (!operationExpression.isNegated) return null
                operationExpression.leftHandSide
            }
            else -> return null
        } as? KtNameReferenceExpression ?: return null

        if (ifExpression.parent !is KtBlockExpression) return null
        val prevStatement = (ifExpression.siblings(forward = false, withItself = false)
                                         .firstIsInstanceOrNull<KtExpression>() ?: return null) as? KtVariableDeclaration
        prevStatement ?: return null
        if (prevStatement.nameAsName != value.getReferencedNameAsName()) return null
        val initializer = prevStatement.initializer ?: return null
        val then = ifExpression.then ?: return null
        val typeReference = (operationExpression as? KtIsExpression)?.typeReference

        val statement = if (then is KtBlockExpression) then.statements.singleOrNull() else then
        statement ?: return null

        if (ReferencesSearch.search(prevStatement, LocalSearchScope(statement)).findFirst() != null) {
            return null
        }

        return Data(initializer, prevStatement, statement, typeReference)
    }

    private fun PsiChildRange.withoutLastStatement(): PsiChildRange {
        val newLast = last!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
        return PsiChildRange(first, newLast)
    }
}