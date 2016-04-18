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
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.expressionComparedToNull
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class IfNullToElvisInspection : IntentionBasedInspection<KtIfExpression>(IfNullToElvisIntention())

class IfNullToElvisIntention : SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' with elvis operator"){
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        val data = calcData(element) ?: return null

        val type = data.ifNullExpression.analyze().getType(data.ifNullExpression) ?: return null
        if (!type.isNothing()) return null

        val rParen = element.rightParenthesis ?: return null
        return TextRange(element.startOffset, rParen.endOffset)
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val (initializer, declaration, ifNullExpr) = calcData(element)!!
        val factory = KtPsiFactory(element)

        val explicitTypeToSet = when {
            // for var with no explicit type, add it so that the actual change won't change
            declaration.isVar && declaration.typeReference == null -> initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)

            // for val with explicit type, change it to non-nullable
            !declaration.isVar && declaration.typeReference != null -> initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)?.makeNotNullable()

            else -> null
        }

        // do not loose any comments!
        val comments = element.extractComments(ifNullExpr)

        for (comment in comments) {
            declaration.add(factory.createWhiteSpace())
            declaration.add(comment)
        }

        val elvis = factory.createExpressionByPattern("$0 ?: $1", initializer, ifNullExpr) as KtBinaryExpression
        val newElvis = initializer.replaced(elvis)
        element.delete()

        if (explicitTypeToSet != null && !explicitTypeToSet.isError) {
            declaration.setType(explicitTypeToSet)
        }

        editor?.caretModel?.moveToOffset(newElvis.right!!.textOffset)
    }

    private data class Data(
            val initializer: KtExpression,
            val declaration: KtVariableDeclaration,
            val ifNullExpression: KtExpression
    )

    private fun calcData(ifExpression: KtIfExpression): Data? {
        if (ifExpression.`else` != null) return null

        val binaryExpression = ifExpression.condition as? KtBinaryExpression ?: return null
        if (binaryExpression.operationToken != KtTokens.EQEQ) return null
        val value = binaryExpression.expressionComparedToNull() as? KtNameReferenceExpression ?: return null

        if (ifExpression.parent !is KtBlockExpression) return null
        val prevStatement = ifExpression.siblings(forward = false, withItself = false)
                                    .firstIsInstanceOrNull<KtExpression>() ?: return null
        if (prevStatement !is KtVariableDeclaration) return null
        if (prevStatement.nameAsName != value.getReferencedNameAsName()) return null
        val initializer = prevStatement.initializer ?: return null
        val then = ifExpression.then ?: return null

        if (then is KtBlockExpression) {
            val statement = then.statements.singleOrNull() ?: return null
            return Data(initializer, prevStatement, statement)
        }
        else {
            return Data(initializer, prevStatement, then)
        }
    }

    private fun PsiElement.extractComments(skipElement: PsiElement): List<PsiComment> {
        val comments = ArrayList<PsiComment>()
        accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element == skipElement) return
                super.visitElement(element)
            }

            override fun visitComment(comment: PsiComment) {
                comments.add(comment)
            }
        })
        return comments
    }
}