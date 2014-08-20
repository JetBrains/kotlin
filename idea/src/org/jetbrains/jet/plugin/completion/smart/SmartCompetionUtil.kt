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

package org.jetbrains.jet.plugin.completion.smart

import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.calls.callUtil.noErrorsInValueArguments
import org.jetbrains.jet.lang.resolve.calls.callUtil.hasUnmappedParameters


fun <D : CallableDescriptor> OverloadResolutionResults<D>.getCandidatesForCompletion(): Collection<ResolvedCall<D>> {
    return getAllCandidates()!!.filter { resolvedCall ->
        resolvedCall.noErrorsInValueArguments() && resolvedCall.hasUnmappedParameters()
    }
}
