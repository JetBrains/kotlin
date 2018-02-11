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

package org.jetbrains.kotlin.resolve.calls.results;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tower.KotlinToResolvedCallTransformerKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

public class OverloadResolutionResultsUtil {
    @NotNull
    public static <D extends CallableDescriptor> OverloadResolutionResults<D> ambiguity(OverloadResolutionResults<D> results1, OverloadResolutionResults<D> results2) {
        Collection<MutableResolvedCall<D>> resultingCalls = Lists.newArrayList();
        resultingCalls.addAll((Collection<MutableResolvedCall<D>>) results1.getResultingCalls());
        resultingCalls.addAll((Collection<MutableResolvedCall<D>>) results2.getResultingCalls());
        return OverloadResolutionResultsImpl.ambiguity(resultingCalls);
    }

    @Nullable
    public static <D extends CallableDescriptor> KotlinType getResultingType(
            @NotNull OverloadResolutionResults<D> results,
            @NotNull ResolutionContext<?> context
    ) {
        ResolvedCall<D> resultingCall = getResultingCall(results, context);
        return resultingCall != null ? resultingCall.getResultingDescriptor().getReturnType() : null;
    }

    @Nullable
    public static <D extends CallableDescriptor> ResolvedCall<D> getResultingCall(
            @NotNull OverloadResolutionResults<D> results,
            @NotNull ResolutionContext<?> context
    ) {
        if (results.isSingleResult() && context.contextDependency == ContextDependency.INDEPENDENT) {
            ResolvedCall<D> resultingCall = results.getResultingCall();
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
                if (!((MutableResolvedCall<D>) resultingCall).hasInferredReturnType()) {
                    return null;
                }
            }
            else {
                if (KotlinToResolvedCallTransformerKt.isNewNotCompleted(resultingCall)) {
                    return null;
                }
            }
        }
        return results.isSingleResult() ? results.getResultingCall() : null;
    }
}
