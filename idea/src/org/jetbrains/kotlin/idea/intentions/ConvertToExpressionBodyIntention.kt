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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.canOmitDeclaredType
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class ConvertToExpressionBodyIntention(
        val convertEmptyToUnit: Boolean = true
) : SelfTargetingOffsetIndependentIntention<KtDeclarationWithBody>(
        KtDeclarationWithBody::class.java, "Convert to expression body"
) {
    override fun isApplicableTo(element: KtDeclarationWithBody): Boolean {
        if (element is KtConstructor<*>) return false
        val value = calcValue(element) ?: return false
        return !value.anyDescendantOfType<KtReturnExpression>(
                canGoInside = { it !is KtFunctionLiteral && it !is KtNamedFunction && it !is KtPropertyAccessor }
        )
    }

    override fun allowCaretInsideElement(element: PsiElement) = element !is KtDeclaration

    override fun applyTo(element: KtDeclarationWithBody, editor: Editor?) {
        applyTo(element) {
            val typeRef = it.typeReference!!
            val colon = it.colon!!
            editor?.apply {
                selectionModel.setSelection(colon.startOffset, typeRef.endOffset)
                caretModel.moveToOffset(typeRef.endOffset)
            }
        }
    }

    fun applyTo(declaration: KtDeclarationWithBody, canDeleteTypeRef: Boolean) {
        val deleteTypeHandler: (KtCallableDeclaration) -> Unit = {
            it.deleteChildRange(it.colon!!, it.typeReference!!)
        }
        applyTo(declaration, deleteTypeHandler.takeIf { canDeleteTypeRef })
    }

    private fun applyTo(declaration: KtDeclarationWithBody, deleteTypeHandler: ((KtCallableDeclaration) -> Unit)?) {
        val block = declaration.blockExpression() ?: return
        val value = calcValue(block)!!

        if (!declaration.hasDeclaredReturnType() && declaration is KtNamedFunction && block.statements.isNotEmpty()) {
            val valueType = value.analyze().getType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                declaration.setType(KotlinBuiltIns.FQ_NAMES.unit.asString(), shortenReferences = true)
            }
        }

        val body = declaration.bodyExpression!!

        val commentSaver = CommentSaver(body)

        declaration.addBefore(KtPsiFactory(declaration).createEQ(), body)
        val newBody = body.replaced(value)

        commentSaver.restore(newBody)

        if (deleteTypeHandler != null && declaration is KtCallableDeclaration) {
            if (declaration.hasDeclaredReturnType() && declaration.canOmitDeclaredType(newBody, canChangeTypeToSubtype = true)) {
                deleteTypeHandler(declaration)
            }
        }
    }

    private fun KtDeclarationWithBody.blockExpression() = when (this) {
        is KtFunctionLiteral -> null
        else -> {
            val body = bodyExpression
            if (!hasBlockBody() || body !is KtBlockExpression) null else body
        }
    }

    private fun calcValue(declaration: KtDeclarationWithBody): KtExpression? {
        val body = declaration.blockExpression() ?: return null
        return calcValue(body)
    }

    private fun calcValue(body: KtBlockExpression): KtExpression? {
        val bodyStatements = body.statements
        if (bodyStatements.isEmpty()) {
            return if (convertEmptyToUnit) KtPsiFactory(body).createExpression("Unit") else null
        }
        val statement = bodyStatements.singleOrNull() ?: return null
        when (statement) {
            is KtReturnExpression -> {
                return statement.returnedExpression
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is KtDeclaration, is KtLoopExpression -> return null // is JetExpression but does not have value

            else -> {
                if (statement is KtBinaryExpression && statement.operationToken in KtTokens.ALL_ASSIGNMENTS) return null // assignment does not have value

                val context = statement.analyze()
                val expressionType = context.getType(statement) ?: return null
                val isUnit = KotlinBuiltIns.isUnit(expressionType)
                if (!isUnit && !KotlinBuiltIns.isNothing(expressionType)) return null
                if (isUnit) {
                    if (statement.hasResultingIfWithoutElse()) {
                        return null
                    }
                    val resultingWhens = statement.resultingWhens()
                    if (resultingWhens.any { it.elseExpression == null && context.get(BindingContext.EXHAUSTIVE_WHEN, it) != true }) {
                        return null
                    }
                }
                return statement
            }
        }
    }
}
