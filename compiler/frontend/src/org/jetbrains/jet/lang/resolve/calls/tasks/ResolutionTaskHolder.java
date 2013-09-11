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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;

import java.util.Collection;
import java.util.List;

public class ResolutionTaskHolder<D extends CallableDescriptor, F extends D> {
    private final JetReferenceExpression reference;
    private final BasicCallResolutionContext basicCallResolutionContext;
    private final PriorityProvider<ResolutionCandidate<D>> priorityProvider;
    private final TracingStrategy tracing;
    private final boolean isSafeCall;

    private final Collection<Collection<ResolutionCandidate<D>>> candidatesList = Lists.newArrayList();

    private List<ResolutionTask<D, F>> tasks = null;

    public ResolutionTaskHolder(@NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext basicCallResolutionContext,
            @NotNull PriorityProvider<ResolutionCandidate<D>> priorityProvider,
            @Nullable TracingStrategy tracing
    ) {
        this.reference = reference;
        this.basicCallResolutionContext = basicCallResolutionContext;
        this.priorityProvider = priorityProvider;
        this.tracing = tracing;
        this.isSafeCall = JetPsiUtil.isSafeCall(basicCallResolutionContext.call);
    }

    public Collection<ResolutionCandidate<D>> setIsSafeCall(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        for (ResolutionCandidate<D> candidate : candidates) {
            candidate.setSafeCall(isSafeCall);
        }
        return candidates;
    }

    public void addCandidates(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        if (!candidates.isEmpty()) {
            candidatesList.add(setIsSafeCall(candidates));
        }
    }

    public void addCandidates(@NotNull List<Collection<ResolutionCandidate<D>>> candidatesList) {
        for (Collection<ResolutionCandidate<D>> candidates : candidatesList) {
            addCandidates(candidates);
        }
    }

    public List<ResolutionTask<D, F>> getTasks() {
        if (tasks == null) {
            tasks = Lists.newArrayList();

            for (int priority = priorityProvider.getMaxPriority(); priority >= 0; priority--) {
                final int finalPriority = priority;
                for (Collection<ResolutionCandidate<D>> candidates : candidatesList) {
                    Collection<ResolutionCandidate<D>> filteredCandidates = Collections2.filter(candidates, new Predicate<ResolutionCandidate<D>>() {
                        @Override
                        public boolean apply(@Nullable ResolutionCandidate<D> input) {
                            return finalPriority == priorityProvider.getPriority(input);
                        }
                    });
                    if (!filteredCandidates.isEmpty()) {
                        tasks.add(new ResolutionTask<D, F>(filteredCandidates, reference, basicCallResolutionContext, tracing));
                    }
                }
            }
        }
        return tasks;
    }

    public interface PriorityProvider<D> {
        int getPriority(D candidate);

        int getMaxPriority();
    }
}
