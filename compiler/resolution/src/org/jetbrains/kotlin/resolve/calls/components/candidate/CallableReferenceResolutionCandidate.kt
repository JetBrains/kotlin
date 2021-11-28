/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components.candidate

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.CallableReceiver
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceAdaptation
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType

/**
 * Suppose we have class A with staticM, memberM, memberExtM.
 * For A::staticM both receivers will be null
 * For A::memberM dispatchReceiver = UnboundReceiver, extensionReceiver = null
 * For a::memberExtM dispatchReceiver = ExplicitValueReceiver, extensionReceiver = ExplicitValueReceiver
 *
 * For class B with companion object B::companionM dispatchReceiver = BoundValueReference
 */
class CallableReferenceResolutionCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val extensionReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    val kotlinCall: CallableReferenceResolutionAtom,
    val expectedType: UnwrappedType?,
    override val callComponents: KotlinCallComponents,
    override val scopeTower: ImplicitScopeTower,
    override val resolutionCallbacks: KotlinResolutionCallbacks,
    override val baseSystem: ConstraintStorage?
) : ResolutionCandidate() {
    override val variableCandidateIfInvoke: ResolutionCandidate? = null
    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null // callable reference's rhs doesn't have type parameters

    override val resolvedCall = ResolvedCallableReferenceCallAtom(
        kotlinCall.call, candidate, explicitReceiverKind,
        if (dispatchReceiver != null) ReceiverExpressionKotlinCallArgument(dispatchReceiver.receiver) else null,
        if (extensionReceiver != null) ReceiverExpressionKotlinCallArgument(extensionReceiver.receiver) else null,
        reflectionCandidateType,
        candidate = this
    )

    override fun addResolvedKtPrimitive(resolvedAtom: ResolvedAtom) {} // there aren't nested resolved primitives for callable references
    override fun getSubResolvedAtoms(): List<ResolvedAtom> = emptyList()

    var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set

    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}