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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.descriptorUtil.hasLowPriorityInOverloadResolution
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

@Deprecated("Temporary error")
internal class PreviousResolutionError(candidateLevel: ResolutionCandidateApplicability): ResolutionDiagnostic(candidateLevel)

@Deprecated("Temporary error")
internal fun createPreviousResolveError(status: ResolutionStatus): PreviousResolutionError? {
    val level = when (status) {
        ResolutionStatus.SUCCESS, ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> return null
        ResolutionStatus.UNSAFE_CALL_ERROR -> ResolutionCandidateApplicability.MAY_THROW_RUNTIME_ERROR
        else -> ResolutionCandidateApplicability.INAPPLICABLE
    }
    return PreviousResolutionError(level)
}

internal val ResolutionCandidateApplicability.isSuccess: Boolean
    get() = this == ResolutionCandidateApplicability.RESOLVED || this == ResolutionCandidateApplicability.RESOLVED_SYNTHESIZED

internal val CallableDescriptor.isSynthesized: Boolean // todo dynamics calls
    get() = (this is CallableMemberDescriptor && isOrOverridesSynthesized(this))
            || hasLowPriorityInOverloadResolution()

internal val CandidateWithBoundDispatchReceiver<*>.requiresExtensionReceiver: Boolean
    get() = descriptor.extensionReceiverParameter != null

internal fun DataFlowDecorator.getAllPossibleTypes(receiver: ReceiverValue) = getSmartCastTypes(receiver) + receiver.type