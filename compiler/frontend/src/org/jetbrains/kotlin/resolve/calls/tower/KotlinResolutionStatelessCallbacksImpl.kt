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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.resolve.DeprecationResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isConventionCall
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInfixCall
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isSuperOrDelegatingConstructorCall
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionStatelessCallbacks
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCall
import org.jetbrains.kotlin.resolve.calls.model.SimpleKotlinCallArgument
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KotlinResolutionStatelessCallbacksImpl(
        private val deprecationResolver: DeprecationResolver
) : KotlinResolutionStatelessCallbacks {
    override fun isDescriptorFromSource(descriptor: CallableDescriptor) =
            DescriptorToSourceUtils.descriptorToDeclaration(descriptor) != null

    override fun isInfixCall(kotlinCall: KotlinCall) =
            kotlinCall is PSIKotlinCallImpl && isInfixCall(kotlinCall.psiCall)

    override fun isOperatorCall(kotlinCall: KotlinCall) =
            (kotlinCall is PSIKotlinCallForInvoke) ||
            (kotlinCall is PSIKotlinCallImpl && isConventionCall(kotlinCall.psiCall))

    override fun isSuperOrDelegatingConstructorCall(kotlinCall: KotlinCall) =
            kotlinCall is PSIKotlinCallImpl && isSuperOrDelegatingConstructorCall(kotlinCall.psiCall)

    override fun isHiddenInResolution(descriptor: DeclarationDescriptor, kotlinCall: KotlinCall) =
            deprecationResolver.isHiddenInResolution(descriptor, isSuperOrDelegatingConstructorCall(kotlinCall))

    override fun isSuperExpression(receiver: SimpleKotlinCallArgument?): Boolean =
            receiver?.psiExpression is KtSuperExpression

    override fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceKotlinCallArgument): ImplicitScopeTower =
            (argument as CallableReferenceKotlinCallArgumentImpl).scopeTowerForResolution

    override fun getVariableCandidateIfInvoke(functionCall: KotlinCall) =
            functionCall.safeAs<PSIKotlinCallForInvoke>()?.variableCall
}
