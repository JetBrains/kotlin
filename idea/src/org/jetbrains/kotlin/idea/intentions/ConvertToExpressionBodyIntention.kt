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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsStatement
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import kotlin.platform.platformName

public class ConvertToExpressionBodyIntention : JetSelfTargetingOffsetIndependentIntention<JetDeclarationWithBody>(
        javaClass(), "Convert to expression body", firstElementOfTypeOnly = true
) {
    override fun isApplicableTo(element: JetDeclarationWithBody): Boolean {
        val value = calcValue(element)
        return value != null && !containsReturn(value)
    }

    override fun applyTo(element: JetDeclarationWithBody, editor: Editor) {
        applyToInternal(element, editor)
    }

    public fun applyTo(declaration: JetDeclarationWithBody) {
        applyToInternal(declaration, null)
    }

    private fun applyToInternal(declaration: JetDeclarationWithBody, editor: Editor?) {
        val value = calcValue(declaration)!!

        if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = value.analyze().getType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                declaration.setType(KotlinBuiltIns.getInstance().getUnitType())
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
                val range = TextRange(colon.startOffset, typeRef.endOffset)
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
                editor.getCaretModel().moveToOffset(range.getEndOffset())
            }
            else {
                (declaration as PsiElement).deleteChildRange(colon, typeRef)
            }
        }
    }

    private fun canOmitType(declaration: JetCallableDeclaration, expression: JetExpression): Boolean {
        if (!declaration.canRemoveTypeSpecificationByVisibility()) return false

        // Workaround for anonymous objects and similar expressions without resolution scope
        // TODO: This should probably be fixed in front-end so that resolution scope is recorded for anonymous objects as well
        val scopeExpression = ((declaration as? JetDeclarationWithBody)?.getBodyExpression() as? JetBlockExpression)
                                 ?.getStatements()?.singleOrNull() as? JetExpression
                         ?: return false

        val declaredType = (declaration.resolveToDescriptor() as? CallableDescriptor)?.getReturnType() ?: return false
        val scope = scopeExpression.analyze()[BindingContext.RESOLUTION_SCOPE, scopeExpression] ?: return false
        val expressionType = expression.analyzeInContext(scope).getType(expression) ?: return false
        return expressionType.isSubtypeOf(declaredType)
    }

    private fun calcValue(declaration: JetDeclarationWithBody): JetExpression? {
        if (declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statement = body.getStatements().singleOrNull() ?: return null
        when(statement) {
            is JetReturnExpression -> {
                return statement.getReturnedExpression()
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration, is JetLoopExpression -> return null // is JetExpression but does not have value

            is JetExpression -> {
                if (statement is JetBinaryExpression && statement.getOperationToken() == JetTokens.EQ) return null // assignment does not have value
                val expressionType = statement.analyze().getType(statement) ?: return null
                if (!KotlinBuiltIns.isUnit(expressionType) && !KotlinBuiltIns.isNothing(expressionType)) return null
                return statement
            }

            else -> return null
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is JetReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is JetNamedFunction || element is JetPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child)) return true
            child = child.getNextSibling()
        }

        return false
    }
}
