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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

private val INAPPLICABLE_STATUSES = setOf(
    ResolutionCandidateApplicability.INAPPLICABLE,
    ResolutionCandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
)

val ResolutionCandidateApplicability.isSuccess: Boolean
    get() = this <= ResolutionCandidateApplicability.RESOLVED_WITH_ERROR

val CallableDescriptor.isSynthesized: Boolean
    get() = (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED)

val CandidateWithBoundDispatchReceiver.requiresExtensionReceiver: Boolean
    get() = descriptor.extensionReceiverParameter != null

val ResolutionCandidateApplicability.isInapplicable: Boolean
    get() = this in INAPPLICABLE_STATUSES

fun <C : Candidate> C.forceResolution(): C {
    resultingApplicability
    return this
}