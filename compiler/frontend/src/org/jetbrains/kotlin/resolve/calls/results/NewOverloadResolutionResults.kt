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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults.Code

abstract class AbstractOverloadResolutionResults<D : CallableDescriptor> : OverloadResolutionResults<D> {
    override fun isSuccess() = resultCode.isSuccess
    override fun isSingleResult() = resultingCalls.size == 1 && resultCode != OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER
    override fun isNothing() = resultCode == OverloadResolutionResults.Code.NAME_NOT_FOUND
    override fun isAmbiguity() = resultCode == OverloadResolutionResults.Code.AMBIGUITY
    override fun isIncomplete() = resultCode == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE
}

class SingleOverloadResolutionResult<D: CallableDescriptor>(val result: ResolvedCall<D>) : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = listOf(result)
    override fun getResultingCall() = result

    override fun getResultingDescriptor(): D = result.resultingDescriptor

    override fun getResultCode(): Code = when (result.status) {
        ResolutionStatus.SUCCESS -> Code.SUCCESS
        ResolutionStatus.RECEIVER_TYPE_ERROR -> Code.CANDIDATES_WITH_WRONG_RECEIVER
        ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> Code.INCOMPLETE_TYPE_INFERENCE
        else -> Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH
    }
}

open class NameNotFoundResolutionResult<D : CallableDescriptor> : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = emptyList()
    override fun getResultingCall() = error("No candidates")
    override fun getResultingDescriptor() = error("No candidates")
    override fun getResultCode() = Code.NAME_NOT_FOUND
}

class ManyCandidates<D : CallableDescriptor>(
        val candidates: Collection<ResolvedCall<D>>
) : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = candidates
    override fun getResultingCall() = error("Many candidates")
    override fun getResultingDescriptor() = error("Many candidates")
    override fun getResultCode() =
            when(candidates.first().status) {
                ResolutionStatus.RECEIVER_TYPE_ERROR -> Code.CANDIDATES_WITH_WRONG_RECEIVER
                ResolutionStatus.SUCCESS -> Code.AMBIGUITY
                ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> Code.INCOMPLETE_TYPE_INFERENCE
                else -> Code.MANY_FAILED_CANDIDATES
            }
}



class AllCandidates<D :  CallableDescriptor>(private val allCandidates: Collection<ResolvedCall<D>>): NameNotFoundResolutionResult<D>() {
    override fun getAllCandidates() = allCandidates
}