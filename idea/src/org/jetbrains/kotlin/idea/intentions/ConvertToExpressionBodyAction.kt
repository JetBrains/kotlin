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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

public class ConvertToExpressionBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.expression.body.action.family.name")

    public fun isAvailable(element: PsiElement): Boolean {
        val data = calcData(element)
        return data != null && !containsReturn(data.value)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.expression.body.action.name"))
        return isAvailable(element)
    }

    public fun invoke(element: PsiElement, editor: Editor? = null) {
        val (declaration, value) = calcData(element)!!

        if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = expressionType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                specifyTypeExplicitly(declaration, "Unit")
            }
        }

        val omitType = declaration.hasDeclaredReturnType() && declaration is JetCallableDeclaration && canOmitType(declaration, value)

        val body = declaration.getBodyExpression()!!
        declaration.addBefore(JetPsiFactory(declaration).createEQ(), body)
        body.replace(value)

        if (omitType) {
            declaration as JetCallableDeclaration

            val typeRef = declaration.getTypeReference()!!
            val colon = declaration.getColon()!!
            if (editor != null) {
                val range = TextRange(colon.getTextRange().getStartOffset(), typeRef.getTextRange().getEndOffset())
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
                editor.getCaretModel().moveToOffset(range.getEndOffset())
            }
            else {
                (declaration : PsiElement).deleteChildRange(colon, typeRef)
            }
        }
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        invoke(element, editor)
    }

    private fun canOmitType(declaration: JetCallableDeclaration, expression: JetExpression): Boolean {
        if (declaration.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false) return true

        val descriptor = declaration.resolveToDescriptor()
        if ((descriptor as? DeclarationDescriptorWithVisibility)?.getVisibility()?.isPublicAPI() ?: false) return false

        // Workaround for anonymous objects and similar expressions without resolution scope
        // TODO: This should probably be fixed in front-end so that resolution scope is recorded for anonymous objects as well
        val scopeExpression = ((declaration as? JetDeclarationWithBody)?.getBodyExpression() as? JetBlockExpression)
                                 ?.getStatements()?.singleOrNull() as? JetExpression
                         ?: return false

        val declaredType = (descriptor as? CallableDescriptor)?.getReturnType() ?: return false
        val scope = scopeExpression.analyze()[BindingContext.RESOLUTION_SCOPE, scopeExpression] ?: return false
        val expressionType = expression.analyzeInContext(scope)[BindingContext.EXPRESSION_TYPE, expression] ?: return false
        return expressionType.isSubtypeOf(declaredType)
    }

    private data class Data(val declaration: JetDeclarationWithBody, val value: JetExpression)

    private fun calcData(element: PsiElement): Data? {
        val declaration = element.getStrictParentOfType<JetDeclarationWithBody>()
        if (declaration == null || declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statements = body.getStatements()
        if (statements.size != 1) return null
        val statement = statements[0]
        return when(statement) {
            is JetReturnExpression -> {
                val value = statement.getReturnedExpression()
                if (value != null) Data(declaration, value) else null
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration -> null // is JetExpression but does not have value
            is JetLoopExpression -> null // is JetExpression but does not have value

            is JetExpression -> {
                if (statement is JetBinaryExpression && statement.getOperationToken() == JetTokens.EQ) return null // assignment does not have value

                val expressionType = expressionType(statement)
                if (expressionType != null &&
                      (KotlinBuiltIns.isUnit(expressionType) || KotlinBuiltIns.isNothing(expressionType)))
                    Data(declaration, statement)
                else
                    null
            }

            else -> null
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is JetReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is JetNamedFunction || element is JetPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child!!)) return true
            child = child!!.getNextSibling()
        }

        return false
    }
}
