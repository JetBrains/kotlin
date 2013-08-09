/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.results;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.calls.context.ContextDependency;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

public class OverloadResolutionResultsUtil {
    @NotNull
    public static <D extends CallableDescriptor> OverloadResolutionResults<D> ambiguity(OverloadResolutionResults<D> results1, OverloadResolutionResults<D> results2) {
        Collection<ResolvedCallWithTrace<D>> resultingCalls = Lists.newArrayList();
        resultingCalls.addAll((Collection<ResolvedCallWithTrace<D>>) results1.getResultingCalls());
        resultingCalls.addAll((Collection<ResolvedCallWithTrace<D>>) results2.getResultingCalls());
        return OverloadResolutionResultsImpl.ambiguity(resultingCalls);
    }

    @Nullable
    public static <D extends CallableDescriptor> JetType getResultingType(
            @NotNull OverloadResolutionResults<D> results,
            @NotNull ContextDependency contextDependency
    ) {
        ResolvedCall<D> resultingCall = getResultingCall((OverloadResolutionResultsImpl<D>) results, contextDependency);
        return resultingCall != null ? resultingCall.getResultingDescriptor().getReturnType() : null;
    }

    @Nullable
    public static <D extends CallableDescriptor> ResolvedCallWithTrace<D> getResultingCall(
            @NotNull OverloadResolutionResultsImpl<D> results,
            @NotNull ContextDependency contextDependency
    ) {
        if (results.isSingleResult() && contextDependency == ContextDependency.INDEPENDENT) {
            if (!CallResolverUtil.hasInferredReturnType(results.getResultingCall())) {
                return null;
            }
        }
        return results.isSingleResult() ? results.getResultingCall() : null;
    }
}
