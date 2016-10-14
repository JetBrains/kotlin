/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.createDeprecationDiagnostic
import org.jetbrains.kotlin.resolve.getDeprecation

object DeprecatedCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        check(resolvedCall.resultingDescriptor, context.trace, reportOn)
    }

    private fun check(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        // Objects will be checked by DeprecatedClassifierUsageChecker
        if (targetDescriptor is FakeCallableDescriptorForObject) return

        val deprecation = targetDescriptor.getDeprecation()

        // avoid duplicating diagnostic when deprecation for property effectively deprecates setter
        if (targetDescriptor is PropertySetterDescriptor && targetDescriptor.correspondingProperty.getDeprecation() == deprecation) return

        if (deprecation != null) {
            trace.report(createDeprecationDiagnostic(element, deprecation))
        }
        else if (targetDescriptor is PropertyDescriptor && shouldCheckPropertyGetter(element)) {
            targetDescriptor.getter?.let { check(it, trace, element) }
        }
    }

    private val PROPERTY_SET_OPERATIONS = TokenSet.create(*KtTokens.ALL_ASSIGNMENTS.types, KtTokens.PLUSPLUS, KtTokens.MINUSMINUS)

    internal fun shouldCheckPropertyGetter(expression: PsiElement): Boolean {
        // property getters do not come as callable yet, so we analyse surroundings to check for deprecation annotation on getter
        val binaryExpression = PsiTreeUtil.getParentOfType<KtBinaryExpression>(expression, KtBinaryExpression::class.java)
        if (binaryExpression != null) {
            val left = binaryExpression.left
            if (left == expression && binaryExpression.operationToken in PROPERTY_SET_OPERATIONS) return false

            val referenceExpressions = PsiTreeUtil.getChildrenOfType<KtReferenceExpression>(left, KtReferenceExpression::class.java)
            if (referenceExpressions != null) {
                for (expr in referenceExpressions) {
                    // skip binary set operations
                    if (expr == expression && binaryExpression.operationToken in PROPERTY_SET_OPERATIONS) return false
                }
            }
        }

        val unaryExpression = PsiTreeUtil.getParentOfType(expression, KtUnaryExpression::class.java)
        // skip unary set operations
        if (unaryExpression?.operationReference?.getReferencedNameElementType() in PROPERTY_SET_OPERATIONS) return false

        val callableExpression = PsiTreeUtil.getParentOfType(expression, KtCallableReferenceExpression::class.java)
        // skip Type::property
        if (callableExpression != null && callableExpression.callableReference == expression) return false

        return true
    }
}
