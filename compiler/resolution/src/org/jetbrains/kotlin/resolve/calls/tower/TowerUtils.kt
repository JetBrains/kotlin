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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

val ResolutionCandidateApplicability.isSuccess: Boolean
    get() = this <= ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY

val CallableDescriptor.isSynthesized: Boolean
    get() = (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED)

val CandidateWithBoundDispatchReceiver<*>.requiresExtensionReceiver: Boolean
    get() = descriptor.extensionReceiverParameter != null

internal class CandidateWithBoundDispatchReceiverImpl<out D : CallableDescriptor>(
        override val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        override val descriptor: D,
        override val diagnostics: List<ResolutionDiagnostic>
) : CandidateWithBoundDispatchReceiver<D> {
    override fun copy(newDescriptor: @UnsafeVariance D) =
            CandidateWithBoundDispatchReceiverImpl(dispatchReceiver, newDescriptor, diagnostics)
}
