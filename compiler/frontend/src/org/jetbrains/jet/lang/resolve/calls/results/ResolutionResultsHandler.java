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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.context.CheckValueArgumentsMode;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl.MAP_TO_CANDIDATE;
import static org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl.MAP_TO_RESULT;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;

public class ResolutionResultsHandler {
    public static ResolutionResultsHandler INSTANCE = new ResolutionResultsHandler();

    private ResolutionResultsHandler() {}

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull ResolutionTask task,
            @NotNull Collection<MutableResolvedCall<D>> candidates
    ) {
        Set<MutableResolvedCall<D>> successfulCandidates = Sets.newLinkedHashSet();
        Set<MutableResolvedCall<D>> failedCandidates = Sets.newLinkedHashSet();
        Set<MutableResolvedCall<D>> incompleteCandidates = Sets.newLinkedHashSet();
        Set<MutableResolvedCall<D>> candidatesWithWrongReceiver = Sets.newLinkedHashSet();
        for (MutableResolvedCall<D> candidateCall : candidates) {
            ResolutionStatus status = candidateCall.getStatus();
            assert status != UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
            if (status.isSuccess()) {
                successfulCandidates.add(candidateCall);
            }
            else if (status == INCOMPLETE_TYPE_INFERENCE) {
                incompleteCandidates.add(candidateCall);
            }
            else if (candidateCall.getStatus() == RECEIVER_TYPE_ERROR) {
                candidatesWithWrongReceiver.add(candidateCall);
            }
            else if (candidateCall.getStatus() != RECEIVER_PRESENCE_ERROR) {
                failedCandidates.add(candidateCall);
            }
        }
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

        if (!successfulCandidates.isEmpty() || !incompleteCandidates.isEmpty()) {
            return computeSuccessfulResult(task, successfulCandidates, incompleteCandidates);
        }
        else if (!failedCandidates.isEmpty()) {
            return computeFailedResult(task, failedCandidates);
        }
        if (!candidatesWithWrongReceiver.isEmpty()) {
            task.tracing.unresolvedReferenceWrongReceiver(task.trace, candidatesWithWrongReceiver);
            return OverloadResolutionResultsImpl.candidatesWithWrongReceiver(candidatesWithWrongReceiver);
        }
        task.tracing.unresolvedReference(task.trace);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeSuccessfulResult(
            @NotNull ResolutionTask task,
            @NotNull Set<MutableResolvedCall<D>> successfulCandidates,
            @NotNull Set<MutableResolvedCall<D>> incompleteCandidates
    ) {
        Set<MutableResolvedCall<D>> successfulAndIncomplete = Sets.newLinkedHashSet();
        successfulAndIncomplete.addAll(successfulCandidates);
        successfulAndIncomplete.addAll(incompleteCandidates);
        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(successfulAndIncomplete, true);
        if (results.isSingleResult()) {
            MutableResolvedCall<D> resultingCall = results.getResultingCall();
            resultingCall.getTrace().moveAllMyDataTo(task.trace);
            if (resultingCall.getStatus() == INCOMPLETE_TYPE_INFERENCE) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(resultingCall);
            }
        }
        if (results.isAmbiguity()) {
            task.tracing.recordAmbiguity(task.trace, results.getResultingCalls());
            boolean allCandidatesIncomplete = allIncomplete(results.getResultingCalls());
            // This check is needed for the following case:
            //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
            if (task.checkArguments == CheckValueArgumentsMode.DISABLED ||
                    !BindingContextUtilPackage.hasUnresolvedArguments(task.call, task.trace.getBindingContext())) {
                if (allCandidatesIncomplete) {
                    task.tracing.cannotCompleteResolve(task.trace, results.getResultingCalls());
                }
                else {
                    task.tracing.ambiguity(task.trace, results.getResultingCalls());
                }
            }
            if (allCandidatesIncomplete) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(results.getResultingCalls());
            }
        }
        return results;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeFailedResult(
            @NotNull ResolutionTask task,
            @NotNull Set<MutableResolvedCall<D>> failedCandidates
    ) {
        if (failedCandidates.size() != 1) {
            // This is needed when there are several overloads some of which are OK but for nullability of the receiver,
            // and some are not OK at all. In this case we'd like to say "unsafe call" rather than "none applicable"
            // Used to be: weak errors. Generalized for future extensions
            for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
                Set<MutableResolvedCall<D>> thisLevel = Sets.newLinkedHashSet();
                for (MutableResolvedCall<D> candidate : failedCandidates) {
                    if (severityLevel.contains(candidate.getStatus())) {
                        thisLevel.add(candidate);
                    }
                }
                if (!thisLevel.isEmpty()) {
                    OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false);
                    if (results.isSingleResult()) {
                        results.getResultingCall().getTrace().moveAllMyDataTo(task.trace);
                        return OverloadResolutionResultsImpl.singleFailedCandidate(results.getResultingCall());
                    }

                    task.tracing.noneApplicable(task.trace, results.getResultingCalls());
                    task.tracing.recordAmbiguity(task.trace, results.getResultingCalls());
                    return OverloadResolutionResultsImpl.manyFailedCandidates(results.getResultingCalls());
                }
            }

            assert false : "Should not be reachable, cause every status must belong to some level";

            Set<MutableResolvedCall<D>> noOverrides = OverrideResolver.filterOutOverridden(failedCandidates, MAP_TO_CANDIDATE);
            if (noOverrides.size() != 1) {
                task.tracing.noneApplicable(task.trace, noOverrides);
                task.tracing.recordAmbiguity(task.trace, noOverrides);
                return OverloadResolutionResultsImpl.manyFailedCandidates(noOverrides);
            }

            failedCandidates = noOverrides;
        }

        MutableResolvedCall<D> failed = failedCandidates.iterator().next();
        failed.getTrace().moveAllMyDataTo(task.trace);
        return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
    }

    private static <D extends CallableDescriptor> boolean allIncomplete(@NotNull Collection<MutableResolvedCall<D>> results) {
        for (MutableResolvedCall<D> result : results) {
            if (result.getStatus() != INCOMPLETE_TYPE_INFERENCE) return false;
        }
        return true;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenerics
    ) {
        if (candidates.size() == 1) {
            return OverloadResolutionResultsImpl.success(candidates.iterator().next());
        }

        MutableResolvedCall<D> maximallySpecific = OverloadingConflictResolver.INSTANCE.findMaximallySpecific(candidates, false);
        if (maximallySpecific != null) {
            return OverloadResolutionResultsImpl.success(maximallySpecific);
        }

        if (discriminateGenerics) {
            MutableResolvedCall<D> maximallySpecificGenericsDiscriminated = OverloadingConflictResolver.INSTANCE.findMaximallySpecific(
                    candidates, true);
            if (maximallySpecificGenericsDiscriminated != null) {
                return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
            }
        }

        Set<MutableResolvedCall<D>> noOverrides = OverrideResolver.filterOutOverridden(candidates, MAP_TO_RESULT);
        if (noOverrides.size() == 1) {
                return OverloadResolutionResultsImpl.success(noOverrides.iterator().next());
        }

        return OverloadResolutionResultsImpl.ambiguity(noOverrides);
    }


}
