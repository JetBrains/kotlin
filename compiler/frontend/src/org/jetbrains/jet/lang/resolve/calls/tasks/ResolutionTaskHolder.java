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
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.calls.BasicCallResolutionContext;

import java.util.Collection;
import java.util.List;

public class ResolutionTaskHolder<D extends CallableDescriptor, F extends D> {
    private final JetReferenceExpression reference;
    private final BasicCallResolutionContext basicCallResolutionContext;
    private final Predicate<ResolutionCandidate<D>> visibleStrategy;
    private final boolean isSafeCall;

    private final Collection<Collection<ResolutionCandidate<D>>> localExtensions = Sets.newLinkedHashSet();
    private final Collection<Collection<ResolutionCandidate<D>>> members = Sets.newLinkedHashSet();
    private final Collection<Collection<ResolutionCandidate<D>>> nonLocalExtensions = Sets.newLinkedHashSet();

    private List<ResolutionTask<D, F>> tasks = null;

    public ResolutionTaskHolder(@NotNull JetReferenceExpression reference,
            @NotNull BasicCallResolutionContext basicCallResolutionContext,
            @NotNull Predicate<ResolutionCandidate<D>> visibleStrategy) {
        this.reference = reference;
        this.basicCallResolutionContext = basicCallResolutionContext;
        this.visibleStrategy = visibleStrategy;
        this.isSafeCall = JetPsiUtil.isSafeCall(basicCallResolutionContext.call);
    }

    public Collection<ResolutionCandidate<D>> setIsSafeCall(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        for (ResolutionCandidate<D> candidate : candidates) {
            candidate.setSafeCall(isSafeCall);
        }
        return candidates;
    }

    public void addLocalExtensions(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        if (!candidates.isEmpty()) {
            localExtensions.add(setIsSafeCall(candidates));
        }
    }

    public void addMembers(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        if (!candidates.isEmpty()) {
            members.add(setIsSafeCall(candidates));
        }
    }

    public void addNonLocalExtensions(@NotNull Collection<ResolutionCandidate<D>> candidates) {
        if (!candidates.isEmpty()) {
            nonLocalExtensions.add(setIsSafeCall(candidates));
        }
    }

    public List<ResolutionTask<D, F>> getTasks() {
        if (tasks == null) {
            tasks = Lists.newArrayList();
            List<Collection<ResolutionCandidate<D>>> candidateList = Lists.newArrayList();
            // If the call is of the form super.foo(), it can actually be only a member
            // But  if there's no appropriate member, we would like to report that super cannot be a receiver for an extension
            // Thus, put members first
            if (TaskPrioritizer.getReceiverSuper(basicCallResolutionContext.call.getExplicitReceiver()) != null) {
                candidateList.addAll(members);
                candidateList.addAll(localExtensions);
            }
            else {
                candidateList.addAll(localExtensions);
                candidateList.addAll(members);
            }
            candidateList.addAll(nonLocalExtensions);

            for (Predicate<ResolutionCandidate<D>> visibilityStrategy : Lists.newArrayList(visibleStrategy, Predicates.not(visibleStrategy))) {
                for (Collection<ResolutionCandidate<D>> candidates : candidateList) {
                    Collection<ResolutionCandidate<D>> filteredCandidates = Collections2.filter(candidates, visibilityStrategy);
                    if (!filteredCandidates.isEmpty()) {
                        tasks.add(new ResolutionTask<D, F>(filteredCandidates, reference, basicCallResolutionContext));
                    }
                }
            }
        }
        return tasks;
    }
}
