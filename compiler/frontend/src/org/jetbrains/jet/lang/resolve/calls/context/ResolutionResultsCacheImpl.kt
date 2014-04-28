/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.context

import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.CallKey
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.lang.psi.JetExpression
import java.util.HashMap
import org.jetbrains.jet.lang.psi.Call.CallType.*

class ResolutionResultsCacheImpl : ResolutionResultsCache {
    private class CachedData(
            var results: OverloadResolutionResultsImpl<*>? = null,
            var resolutionTrace: DelegatingBindingTrace? = null,
            var deferredComputation: CallCandidateResolutionContext<*>? = null
    )

    private val data = HashMap<CallKey, CachedData>()

    private fun getOrCreateCachedInfo(callKey: CallKey) = data.getOrPut(callKey, { CachedData() })

    override fun <D : CallableDescriptor?> recordResolutionResults(callKey: CallKey, results: OverloadResolutionResultsImpl<D>) {
        getOrCreateCachedInfo(callKey).results = results
    }

    override fun <D : CallableDescriptor?> getResolutionResults(callKey: CallKey): OverloadResolutionResultsImpl<D>? {
        return data[callKey]?.results as OverloadResolutionResultsImpl<D>?
    }

    override fun recordResolutionTrace(callKey: CallKey, delegatingTrace: DelegatingBindingTrace) {
        getOrCreateCachedInfo(callKey).resolutionTrace = delegatingTrace
    }

    override fun getResolutionTrace(callKey: CallKey): DelegatingBindingTrace? {
        return data[callKey]?.resolutionTrace
    }

    override fun <D : CallableDescriptor?> recordDeferredComputationForCall(callKey: CallKey, deferredComputation: CallCandidateResolutionContext<D>) {
        getOrCreateCachedInfo(callKey).deferredComputation = deferredComputation
    }

    override fun getDeferredComputation(expression: JetExpression?): CallCandidateResolutionContext<out CallableDescriptor?>? {
        if (expression == null) return null

        for (callType in listOf(DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE)) {
            val deferredComputation = data[CallKey.create(callType, expression)]?.deferredComputation
            if (deferredComputation != null) {
                return deferredComputation
            }
        }
        return null
    }

    fun addData(cache: ResolutionResultsCacheImpl) {
        data.putAll(cache.data)
    }
}