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
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.lang.psi.JetExpression
import java.util.HashMap
import org.jetbrains.jet.lang.psi.Call

class ResolutionResultsCacheImpl : ResolutionResultsCache {
    private class CachedData(
            var results: OverloadResolutionResultsImpl<*>? = null,
            var resolutionTrace: DelegatingBindingTrace? = null,
            var deferredComputation: CallCandidateResolutionContext<*>? = null
    )

    private val data = HashMap<Call, CachedData>()

    private fun getOrCreateCachedInfo(call: Call) = data.getOrPut(call, { CachedData() })

    override fun <D : CallableDescriptor?> recordResolutionResults(call: Call, results: OverloadResolutionResultsImpl<D>) {
        getOrCreateCachedInfo(call).results = results
    }

    override fun <D : CallableDescriptor?> getResolutionResults(call: Call): OverloadResolutionResultsImpl<D>? {
        return data[call]?.results as OverloadResolutionResultsImpl<D>?
    }

    override fun recordResolutionTrace(call: Call, delegatingTrace: DelegatingBindingTrace) {
        getOrCreateCachedInfo(call).resolutionTrace = delegatingTrace
    }

    override fun getResolutionTrace(call: Call): DelegatingBindingTrace? {
        return data[call]?.resolutionTrace
    }

    override fun <D : CallableDescriptor?> recordDeferredComputationForCall(call: Call, deferredComputation: CallCandidateResolutionContext<D>) {
        getOrCreateCachedInfo(call).deferredComputation = deferredComputation
    }

    override fun getDeferredComputation(call: Call?): CallCandidateResolutionContext<out CallableDescriptor?>? {
        if (call == null) return null

        return data[call]?.deferredComputation
    }

    fun addData(cache: ResolutionResultsCacheImpl) {
        data.putAll(cache.data)
    }
}