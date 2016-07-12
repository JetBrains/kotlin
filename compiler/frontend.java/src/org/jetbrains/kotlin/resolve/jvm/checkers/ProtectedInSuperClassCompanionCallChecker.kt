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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class ProtectedInSuperClassCompanionCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val targetDescriptor = resolvedCall.resultingDescriptor.original

        // Protected non-JVM static
        if (targetDescriptor.visibility != Visibilities.PROTECTED) return
        if (targetDescriptor.hasJvmStaticAnnotation()) return
        val containerDescriptor = targetDescriptor.containingDeclaration
        // Declared in companion object
        if (containerDescriptor is ClassDescriptor && containerDescriptor.isCompanionObject) {
            val companionDescriptor = containerDescriptor
            val companionOwnerDescriptor = companionDescriptor.containingDeclaration as? ClassDescriptor ?: return
            val parentClassDescriptors = context.scope.ownerDescriptor.parentsWithSelf.filterIsInstance<ClassDescriptor>()
            // Called from within a derived class
            if (!parentClassDescriptors.any { DescriptorUtils.isSubclass(it, companionOwnerDescriptor) }) return
            // Called not within the same companion object or its owner class
            if (companionDescriptor !in parentClassDescriptors && companionOwnerDescriptor !in parentClassDescriptors) {
                context.trace.report(ErrorsJvm.SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC.on(reportOn))
            }
        }
    }
}
