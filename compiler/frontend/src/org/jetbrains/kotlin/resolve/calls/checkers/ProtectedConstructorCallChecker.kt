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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf

object ProtectedConstructorCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return
        val constructorOwner = descriptor.containingDeclaration.original
        val scopeOwner = context.scope.ownerDescriptor

        val actualConstructor = (descriptor as? TypeAliasConstructorDescriptor)?.underlyingConstructorDescriptor ?: descriptor

        if (actualConstructor.visibility.normalize() != Visibilities.PROTECTED) return
        // Error already reported
        if (!Visibilities.isVisibleWithAnyReceiver(descriptor, scopeOwner)) return

        val calleeExpression = resolvedCall.call.calleeExpression

        // Permit constructor super-calls
        when (calleeExpression) {
            is KtConstructorCalleeExpression -> if (calleeExpression.parent is KtSuperTypeCallEntry) return
            is KtConstructorDelegationReferenceExpression -> return
        }

        // Permit calls within class
        if (scopeOwner.parentsWithSelf.any { it.original === constructorOwner }) return

        // Using FALSE_IF_PROTECTED helps us to check that descriptor doesn't meet conditions of java package/static-protected
        // (i.e. being in the same package)
        // And without ProtectedConstructorCallChecker such calls would be allowed only because they are performed within subclass
        // of constructor owner
        @Suppress("DEPRECATION")
        if (Visibilities.findInvisibleMember(Visibilities.FALSE_IF_PROTECTED, descriptor, scopeOwner) == actualConstructor.original) {
            context.trace.report(Errors.PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL.on(reportOn, descriptor))
        }
    }
}
