/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
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
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaKotlinCallArgument,
        isSuspend: Boolean,
        receiverType: UnwrappedType?,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType? // null means, that return type is not proper i.e. it depends on some type variables
    ): List<KotlinCallArgument>

    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)

    fun createReceiverWithSmartCastInfo(resolvedAtom: ResolvedCallAtom): ReceiverValueWithSmartCastInfo?

    fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean

    val inferenceSession: InferenceSession
}