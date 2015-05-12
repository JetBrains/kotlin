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
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.expressionComparedToNull
import org.jetbrains.kotlin.idea.util.isNothing
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.ArrayList

public class IfNullToElvisInspection : IntentionBasedInspection<JetIfExpression>(IfNullToElvisIntention())

public class IfNullToElvisIntention : JetSelfTargetingRangeIntention<JetIfExpression>(javaClass(), "Replace 'if' with elvis operator"){
    override fun applicabilityRange(element: JetIfExpression): TextRange? {
        val data = calcData(element) ?: return null

        val type = data.ifNullExpression.analyze().getType(data.ifNullExpression) ?: return null
        if (!type.isNothing()) return null

        val rParen = element.getRightParenthesis() ?: return null
        return TextRange(element.startOffset, rParen.endOffset)
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val newElvis = applyTo(element)
        editor.getCaretModel().moveToOffset(newElvis.getRight()!!.getTextOffset())
    }

    public fun applyTo(element: JetIfExpression): JetBinaryExpression {
        val (initializer, declaration, ifNullExpr) = calcData(element)!!
        val factory = JetPsiFactory(element)

        val explicitTypeToAdd = if (declaration.isVar() && declaration.getTypeReference() == null)
            initializer.analyze().getType(initializer)
        else
            null

        // do not loose any comments!
        val comments = element.extractComments(ifNullExpr)

        for (comment in comments) {
            declaration.add(factory.createWhiteSpace())
            declaration.add(comment)
        }

        val elvis = factory.createExpressionByPattern("$0 ?: $1", initializer, ifNullExpr) as JetBinaryExpression
        val newElvis = initializer.replaced(elvis)
        element.delete()

        if (explicitTypeToAdd != null && !explicitTypeToAdd.isError()) {
            declaration.setType(explicitTypeToAdd)
        }

        return newElvis
    }

    private data class Data(
            val initializer: JetExpression,
            val declaration: JetVariableDeclaration,
            val ifNullExpression: JetExpression
    )

    private fun calcData(ifExpression: JetIfExpression): Data? {
        if (ifExpression.getElse() != null) return null

        val binaryExpression = ifExpression.getCondition() as? JetBinaryExpression ?: return null
        if (binaryExpression.getOperationToken() != JetTokens.EQEQ) return null
        val value = binaryExpression.expressionComparedToNull() as? JetSimpleNameExpression ?: return null

        if (ifExpression.getParent() !is JetBlockExpression) return null
        val prevStatement = ifExpression.siblings(forward = false, withItself = false)
                                    .firstIsInstanceOrNull<JetExpression>() ?: return null
        if (prevStatement !is JetVariableDeclaration) return null
        if (prevStatement.getNameAsName() != value.getReferencedNameAsName()) return null
        val initializer = prevStatement.getInitializer() ?: return null
        val then = ifExpression.getThen() ?: return null

        if (then is JetBlockExpression) {
            val statement = then.getStatements().singleOrNull() as? JetExpression ?: return null
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