/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.CompatibilityWarning
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.Candidate
import org.jetbrains.kotlin.resolve.calls.tower.getResultApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.UnwrappedType

/**
 * Suppose we have class A with staticM, memberM, memberExtM.
 * For A::staticM both receivers will be null
 * For A::memberM dispatchReceiver = UnboundReceiver, extensionReceiver = null
 * For a::memberExtM dispatchReceiver = ExplicitValueReceiver, extensionReceiver = ExplicitValueReceiver
 *
 * For class B with companion object B::companionM dispatchReceiver = BoundValueReference
 */
class CallableReferenceCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val extensionReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    initialDiagnostics: List<KotlinCallDiagnostic>
) : Candidate {
    private val mutableDiagnostics = initialDiagnostics.toMutableList()
    val diagnostics: List<KotlinCallDiagnostic> = mutableDiagnostics

    override val resultingApplicability = getResultApplicability(diagnostics)

    override fun addCompatibilityWarning(other: Candidate) {
        if (this !== other && other is CallableReferenceCandidate) {
            mutableDiagnostics.add(CompatibilityWarning(other.candidate))
        }
    }

    override val isSuccessful get() = resultingApplicability.isSuccess

    var freshSubstitutor: FreshVariableNewTypeSubstitutor? = null
        internal set

    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}