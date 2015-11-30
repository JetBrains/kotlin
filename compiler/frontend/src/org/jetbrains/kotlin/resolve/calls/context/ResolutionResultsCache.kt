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

package org.jetbrains.kotlin.resolve.calls.context

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.calls.context.ResolutionResultsCache.CachedData
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import java.util.*

public interface ResolutionResultsCache {
    public data class CachedData(
            val resolutionResults: OverloadResolutionResultsImpl<*>,
            val deferredComputation: BasicCallResolutionContext,
            val tracing: TracingStrategy,
            val resolutionTrace: DelegatingBindingTrace
    )

    fun record(
            call: Call,
            results: OverloadResolutionResultsImpl<*>,
            deferredComputation: BasicCallResolutionContext,
            tracing: TracingStrategy,
            resolutionTrace: DelegatingBindingTrace
    )

    operator fun get(call: Call): CachedData?
}

class ResolutionResultsCacheImpl : ResolutionResultsCache {
    private val data = HashMap<Call, CachedData>()

    override fun record(
            call: Call,
            results: OverloadResolutionResultsImpl<out CallableDescriptor?>,
            deferredComputation: BasicCallResolutionContext,
            tracing: TracingStrategy,
            resolutionTrace: DelegatingBindingTrace
    ) {
        data[call] = CachedData(results, deferredComputation, tracing, resolutionTrace)
    }

    override fun get(call: Call): CachedData? = data[call]

    fun addData(cache: ResolutionResultsCacheImpl) {
        data.putAll(cache.data)
    }
}

public class TemporaryResolutionResultsCache(private val parentCache: ResolutionResultsCache) : ResolutionResultsCache {
    private val innerCache = ResolutionResultsCacheImpl()

    override fun record(
            call: Call,
            results: OverloadResolutionResultsImpl<out CallableDescriptor?>,
            deferredComputation: BasicCallResolutionContext,
            tracing: TracingStrategy,
            resolutionTrace: DelegatingBindingTrace
    ) {
        innerCache.record(call, results, deferredComputation, tracing, resolutionTrace)
    }

    override fun get(call: Call): CachedData? = innerCache[call] ?: parentCache[call]

    public fun commit() {
        when (parentCache) {
            is ResolutionResultsCacheImpl -> parentCache.addData(innerCache)
            is TemporaryResolutionResultsCache -> parentCache.innerCache.addData(innerCache)
        }
    }
}
