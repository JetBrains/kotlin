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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

object LateinitIntrinsicApplicabilityChecker : CallChecker {
    private val ACCESSIBLE_LATEINIT_PROPERTY_LITERAL = FqName("kotlin.internal.AccessibleLateinitPropertyLiteral")

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        // An optimization
        if (descriptor.name.asString() != "isInitialized") return

        if (descriptor.extensionReceiverParameter?.annotations?.hasAnnotation(ACCESSIBLE_LATEINIT_PROPERTY_LITERAL) != true) return

        val expression = (resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression?.let(KtPsiUtil::safeDeparenthesize)
        if (expression !is KtCallableReferenceExpression) {
            context.trace.report(LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL.on(reportOn))
        } else {
            val propertyReferenceResolvedCall = expression.callableReference.getResolvedCall(context.trace.bindingContext) ?: return
            val referencedProperty = propertyReferenceResolvedCall.resultingDescriptor
            if (referencedProperty !is PropertyDescriptor) {
                error("Lateinit intrinsic is incorrectly resolved not to a property: $referencedProperty")
            }

            if (!referencedProperty.isLateInit) {
                context.trace.report(LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT.on(reportOn))
            } else if (!isBackingFieldAccessible(referencedProperty, context)) {
                context.trace.report(LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY.on(reportOn, referencedProperty))
            } else if ((context.scope.ownerDescriptor as? FunctionDescriptor)?.isInline == true) {
                context.trace.report(LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION.on(reportOn))
            }
        }
    }

    private fun isBackingFieldAccessible(descriptor: PropertyDescriptor, context: CallCheckerContext): Boolean {
        // We can generate direct access to the backing field only if the property is defined in the same source file,
        // and the property is originally declared in a scope that is a parent of the usage scope
        val declaration =
            OverridingUtil.filterOutOverridden(OverridingUtil.getOverriddenDeclarations(descriptor)).singleOrNull() ?: return false
        val declarationSourceFile = DescriptorToSourceUtils.getContainingFile(declaration) ?: return false
        val usageSourceFile = DescriptorToSourceUtils.getContainingFile(context.scope.ownerDescriptor) ?: return false
        if (declarationSourceFile != usageSourceFile) return false

        return declaration.containingDeclaration in
                generateSequence(context.scope.ownerDescriptor, DeclarationDescriptor::getContainingDeclaration)
    }
}
