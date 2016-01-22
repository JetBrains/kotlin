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

package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.Deprecation
import org.jetbrains.kotlin.resolve.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.getDeprecation

class DeprecatedSymbolValidator : SymbolUsageValidator {

    override fun validateCall(resolvedCall: ResolvedCall<*>?, targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        val deprecation = targetDescriptor.getDeprecation()

        // avoid duplicating diagnostic when deprecation for property effectively deprecates setter
        if (targetDescriptor is PropertySetterDescriptor && targetDescriptor.correspondingProperty.getDeprecation() == deprecation) return

        if (deprecation != null) {
            trace.report(createDeprecationDiagnostic(element, deprecation))
        }
        else if (targetDescriptor is PropertyDescriptor) {
            propertyGetterWorkaround(resolvedCall, targetDescriptor, trace, element)
        }
    }

    override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
        // Do not check types in annotation entries to prevent cycles in resolve, rely on call message
        val annotationEntry = KtStubbedPsiUtil.getPsiOrStubParent(element, KtAnnotationEntry::class.java, true)
        if (annotationEntry != null && annotationEntry.getCalleeExpression()!!.getConstructorReferenceExpression() == element)
            return

        // Do not check types in calls to super constructor in extends list, rely on call message
        val superExpression = KtStubbedPsiUtil.getPsiOrStubParent(element, KtSuperTypeCallEntry::class.java, true)
        if (superExpression != null && superExpression.getCalleeExpression().getConstructorReferenceExpression() == element)
            return

        val deprecation = targetDescriptor.getDeprecation()
        if (deprecation != null) {
            trace.report(createDeprecationDiagnostic(element, deprecation))
        }
    }

    private fun createDeprecationDiagnostic(element: PsiElement, deprecation: Deprecation): Diagnostic {
        val targetOriginal = deprecation.target.original
        if (deprecation.deprecationLevel == DeprecationLevelValue.ERROR) {
            return Errors.DEPRECATION_ERROR.on(element, targetOriginal, deprecation.message)
        }

        return Errors.DEPRECATION.on(element, targetOriginal, deprecation.message)
    }

    private val PROPERTY_SET_OPERATIONS = TokenSet.create(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ,
                                                          KtTokens.DIVEQ, KtTokens.PERCEQ, KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

    fun propertyGetterWorkaround(
            resolvedCall: ResolvedCall<*>?,
            propertyDescriptor: PropertyDescriptor,
            trace: BindingTrace,
            expression: PsiElement
    ) {
        // property getters do not come as callable yet, so we analyse surroundings to check for deprecation annotation on getter
        val binaryExpression = PsiTreeUtil.getParentOfType<KtBinaryExpression>(expression, KtBinaryExpression::class.java)
        if (binaryExpression != null) {
            val left = binaryExpression.left
            if (left == expression) {
                val operation = binaryExpression.operationToken
                if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                    return
            }

            val jetReferenceExpressions = PsiTreeUtil.getChildrenOfType<KtReferenceExpression>(left, KtReferenceExpression::class.java)
            if (jetReferenceExpressions != null) {
                for (expr in jetReferenceExpressions) {
                    if (expr == expression) {
                        val operation = binaryExpression.operationToken
                        if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                            return // skip binary set operations
                    }
                }
            }
        }

        val unaryExpression = PsiTreeUtil.getParentOfType(expression, KtUnaryExpression::class.java)
        if (unaryExpression != null) {
            val operation = unaryExpression.operationReference.getReferencedNameElementType()
            if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                return // skip unary set operations

        }

        val callableExpression = PsiTreeUtil.getParentOfType(expression, KtCallableReferenceExpression::class.java)
        if (callableExpression != null && callableExpression.getCallableReference() == expression) {
            return // skip Type::property
        }

        propertyDescriptor.getter?.let { validateCall(resolvedCall, it, trace, expression) }
    }
}