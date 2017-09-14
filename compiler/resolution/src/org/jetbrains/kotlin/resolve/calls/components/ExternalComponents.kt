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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.types.UnwrappedType

// stateless component
interface KotlinResolutionStatelessCallbacks {
    fun isDescriptorFromSource(descriptor: CallableDescriptor): Boolean
    fun isInfixCall(kotlinCall: KotlinCall): Boolean
    fun isOperatorCall(kotlinCall: KotlinCall): Boolean
    fun isSuperOrDelegatingConstructorCall(kotlinCall: KotlinCall): Boolean
    fun isHiddenInResolution(descriptor: DeclarationDescriptor, kotlinCall: KotlinCall): Boolean
    fun isSuperExpression(receiver: SimpleKotlinCallArgument?): Boolean
    fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceKotlinCallArgument): ImplicitScopeTower
    fun getVariableCandidateIfInvoke(functionCall: KotlinCall): KotlinResolutionCandidate?
}

// This components hold state (trace). Work with this carefully.
interface KotlinResolutionCallbacks {
    fun analyzeAndGetLambdaResultArguments(
            lambdaArgument: LambdaKotlinCallArgument,
            isSuspend: Boolean,
            receiverType: UnwrappedType?,
            parameters: List<UnwrappedType>,
            expectedReturnType: UnwrappedType? // null means, that return type is not proper i.e. it depends on some type variables
    ): List<SimpleKotlinCallArgument>

    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)
}