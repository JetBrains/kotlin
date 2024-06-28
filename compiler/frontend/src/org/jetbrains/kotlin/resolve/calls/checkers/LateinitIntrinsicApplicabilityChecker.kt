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
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver

class LateinitIntrinsicApplicabilityChecker(val isWarningInPre19: Boolean) : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor

        // An optimization
        if (descriptor.name.asString() != "isInitialized") return

        if (descriptor.extensionReceiverParameter?.annotations?.hasAnnotation(FqNames.accessibleLateinitPropertyLiteral) != true) return

        val expression = (resolvedCall.extensionReceiver as? ExpressionReceiver)?.expression?.let(KtPsiUtil::safeDeparenthesize)
        fun <T> chooseDiagnostic(ifWarning: T, ifError: T) =
            if (isWarningInPre19 && !context.languageVersionSettings.supportsFeature(LanguageFeature.NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess))
                ifWarning
            else
                ifError
        if (expression !is KtCallableReferenceExpression) {
            val diagnostic = chooseDiagnostic(LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL_WARNING, LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL)
            context.trace.report(diagnostic.on(reportOn))
        } else {
            val propertyReferenceResolvedCall = expression.callableReference.getResolvedCall(context.trace.bindingContext) ?: return
            val referencedProperty = propertyReferenceResolvedCall.resultingDescriptor
            if (referencedProperty !is PropertyDescriptor) {
                error("Lateinit intrinsic is incorrectly resolved not to a property: $referencedProperty")
            }

            if (!referencedProperty.isLateInit) {
                val diagnostic = chooseDiagnostic(LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT_WARNING, LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT)
                context.trace.report(diagnostic.on(reportOn))
            } else if (!isBackingFieldAccessible(referencedProperty, context)) {
                val diagnostic = chooseDiagnostic(
                    LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY_WARNING,
                    LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY
                )
                context.trace.report(diagnostic.on(reportOn, referencedProperty))
            } else if ((context.scope.ownerDescriptor as? FunctionDescriptor)?.isInline == true) {
                val diagnostic = chooseDiagnostic(
                    LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION_WARNING,
                    LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION
                )
                context.trace.report(diagnostic.on(reportOn))
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
