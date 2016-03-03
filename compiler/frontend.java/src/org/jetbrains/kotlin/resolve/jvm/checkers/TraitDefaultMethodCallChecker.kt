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

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getSuperCallExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class TraitDefaultMethodCallChecker : CallChecker {

    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (getSuperCallExpression(resolvedCall.getCall()) == null) return

        val targetDescriptor = resolvedCall.resultingDescriptor.original
        val containerDescriptor = targetDescriptor.containingDeclaration

        if (containerDescriptor is JavaClassDescriptor && DescriptorUtils.isInterface(containerDescriptor)) {
            //is java interface default method called from trait
            val classifier = DescriptorUtils.getParentOfType(context.scope.ownerDescriptor, ClassifierDescriptor::class.java)

            if (classifier != null && DescriptorUtils.isInterface(classifier)) {
                context.trace.report(
                        ErrorsJvm.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER.on(
                                PsiTreeUtil.getParentOfType(resolvedCall.call.callElement, KtExpression::class.java)!!
                        )
                )
            }
        }

    }
}
