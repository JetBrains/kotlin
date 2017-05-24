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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateStatus
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType

sealed class ResolvedKotlinCall {
    class CompletedResolvedKotlinCall(
            val completedCall: CompletedKotlinCall,
            val allInnerCalls: Collection<CompletedKotlinCall>,
            val lambdaArguments: List<ResolvedLambdaArgument>
    ): ResolvedKotlinCall()

    class OnlyResolvedKotlinCall(
            val candidate: KotlinResolutionCandidate
    ) : ResolvedKotlinCall() {
        val currentReturnType: UnwrappedType = candidate.lastCall.descriptorWithFreshTypes.returnTypeOrNothing
    }
}

sealed class CompletedKotlinCall {
    abstract val resolutionStatus: ResolutionCandidateStatus

    class Simple(
            val kotlinCall: KotlinCall,
            val candidateDescriptor: CallableDescriptor,
            val resultingDescriptor: CallableDescriptor,
            override val resolutionStatus: ResolutionCandidateStatus,
            val explicitReceiverKind: ExplicitReceiverKind,
            val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
            val extensionReceiver: ReceiverValueWithSmartCastInfo?,
            val typeArguments: List<UnwrappedType>,
            val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    ): CompletedKotlinCall()

    class VariableAsFunction(
            val kotlinCall: KotlinCall,
            val variableCall: Simple,
            val invokeCall: Simple
    ): CompletedKotlinCall() {

        override val resolutionStatus: ResolutionCandidateStatus =
                ResolutionCandidateStatus(variableCall.resolutionStatus.diagnostics + invokeCall.resolutionStatus.diagnostics)
    }
}

sealed class ResolvedCallArgument {
    abstract val arguments: List<KotlinCallArgument>

    object DefaultArgument : ResolvedCallArgument() {
        override val arguments: List<KotlinCallArgument>
            get() = emptyList()

    }

    class SimpleArgument(val callArgument: KotlinCallArgument): ResolvedCallArgument() {
        override val arguments: List<KotlinCallArgument>
            get() = listOf(callArgument)

    }

    class VarargArgument(override val arguments: List<KotlinCallArgument>): ResolvedCallArgument()
}