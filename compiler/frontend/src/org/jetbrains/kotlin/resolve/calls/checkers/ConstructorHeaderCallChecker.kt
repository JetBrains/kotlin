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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtInstanceExpressionWithLabel
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf

object ConstructorHeaderCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val dispatchReceiverClass = resolvedCall.dispatchReceiver.classDescriptorForImplicitReceiver
        val extensionReceiverClass = resolvedCall.extensionReceiver.classDescriptorForImplicitReceiver

        val callElement = resolvedCall.call.callElement
        val labelReferenceClass =
                if (callElement is KtInstanceExpressionWithLabel) {
                    context.trace.get(BindingContext.REFERENCE_TARGET, callElement.instanceReference) as? ClassDescriptor
                }
                else null

        if (dispatchReceiverClass == null && extensionReceiverClass == null && labelReferenceClass == null) return

        val classes = setOf(dispatchReceiverClass, extensionReceiverClass, labelReferenceClass)

        if (context.scope.parentsWithSelf.any { scope ->
            scope is LexicalScope && scope.kind == LexicalScopeKind.CONSTRUCTOR_HEADER &&
            (scope.ownerDescriptor as ClassConstructorDescriptor).containingDeclaration in classes
        }) {
            context.trace.report(Errors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.on(reportOn, resolvedCall.resultingDescriptor))
        }
    }
}

private val Receiver?.classDescriptorForImplicitReceiver: ClassDescriptor?
    get() = (this as? ImplicitReceiver)?.declarationDescriptor as? ClassDescriptor
