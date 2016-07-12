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
import org.jetbrains.kotlin.descriptors.*
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

    override fun validateCall(resolvedCall: ResolvedCall<*>, trace: BindingTrace, element: PsiElement) {
        validate(resolvedCall.resultingDescriptor, trace, element)
    }

    override fun validatePropertyCall(targetDescriptor: PropertyAccessorDescriptor, trace: BindingTrace, element: PsiElement) {
        validate(targetDescriptor, trace, element)
    }

    private fun validate(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        val deprecation = targetDescriptor.getDeprecation()

        // avoid duplicating diagnostic when deprecation for property effectively deprecates setter
        if (targetDescriptor is PropertySetterDescriptor && targetDescriptor.correspondingProperty.getDeprecation() == deprecation) return

        if (deprecation != null) {
            trace.report(createDeprecationDiagnostic(element, deprecation))
        }
        else if (targetDescriptor is PropertyDescriptor) {
            propertyGetterWorkaround(targetDescriptor, trace, element)
        }
    }

    override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
        // Do not check types in annotation entries to prevent cycles in resolve, rely on call message
        val annotationEntry = KtStubbedPsiUtil.getPsiOrStubParent(element, KtAnnotationEntry::class.java, true)
        if (annotationEntry != null && annotationEntry.calleeExpression!!.constructorReferenceExpression == element) return

        // Do not check types in calls to super constructor in extends list, rely on call message
        val superExpression = KtStubbedPsiUtil.getPsiOrStubParent(element, KtSuperTypeCallEntry::class.java, true)
        if (superExpression != null && superExpression.calleeExpression.constructorReferenceExpression == element) return

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
            propertyDescriptor: PropertyDescriptor,
            trace: BindingTrace,
            expression: PsiElement
    ) {
        // property getters do not come as callable yet, so we analyse surroundings to check for deprecation annotation on getter
        val binaryExpression = PsiTreeUtil.getParentOfType<KtBinaryExpression>(expression, KtBinaryExpression::class.java)
        if (binaryExpression != null) {
            val left = binaryExpression.left
            if (left == expression && binaryExpression.operationToken in PROPERTY_SET_OPERATIONS) return

            val referenceExpressions = PsiTreeUtil.getChildrenOfType<KtReferenceExpression>(left, KtReferenceExpression::class.java)
            if (referenceExpressions != null) {
                for (expr in referenceExpressions) {
                    // skip binary set operations
                    if (expr == expression && binaryExpression.operationToken in PROPERTY_SET_OPERATIONS) return
                }
            }
        }

        val unaryExpression = PsiTreeUtil.getParentOfType(expression, KtUnaryExpression::class.java)
        // skip unary set operations
        if (unaryExpression?.operationReference?.getReferencedNameElementType() in PROPERTY_SET_OPERATIONS) return

        val callableExpression = PsiTreeUtil.getParentOfType(expression, KtCallableReferenceExpression::class.java)
        // skip Type::property
        if (callableExpression != null && callableExpression.callableReference == expression) return

        propertyDescriptor.getter?.let { validatePropertyCall(it, trace, expression) }
    }
}
