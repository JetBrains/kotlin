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
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl.MAP_TO_CANDIDATE;
import static org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl.MAP_TO_RESULT;
import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.*;

public class ResolutionResultsHandler {
    public static ResolutionResultsHandler INSTANCE = new ResolutionResultsHandler();

    private ResolutionResultsHandler() {}

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull Set<ResolvedCallWithTrace<D>> candidates
    ) {
        Set<ResolvedCallWithTrace<D>> successfulCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCallWithTrace<D>> failedCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCallWithTrace<D>> incompleteCandidates = Sets.newLinkedHashSet();
        for (ResolvedCallWithTrace<D> candidateCall : candidates) {
            ResolutionStatus status = candidateCall.getStatus();
            assert status != UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
            if (status.isSuccess()) {
                successfulCandidates.add(candidateCall);
            }
            else if (status == INCOMPLETE_TYPE_INFERENCE) {
                incompleteCandidates.add(candidateCall);
            }
            else if (candidateCall.getStatus() != STRONG_ERROR) {
                failedCandidates.add(candidateCall);
            }
        }
        return computeResultAndReportErrors(trace, tracing, successfulCandidates, failedCandidates, incompleteCandidates);
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing,
            @NotNull Set<ResolvedCallWithTrace<D>> successfulCandidates,
            @NotNull Set<ResolvedCallWithTrace<D>> failedCandidates,
            @NotNull Set<ResolvedCallWithTrace<D>> incompleteCandidates
    ) {
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

        if (!successfulCandidates.isEmpty() || !incompleteCandidates.isEmpty()) {
            Set<ResolvedCallWithTrace<D>> successfulAndIncomplete = Sets.newLinkedHashSet();
            successfulAndIncomplete.addAll(successfulCandidates);
            successfulAndIncomplete.addAll(incompleteCandidates);
            OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(successfulAndIncomplete, true);
            if (results.isSingleResult()) {
                ResolvedCallWithTrace<D> resultingCall = results.getResultingCall();
                resultingCall.getTrace().moveAllMyDataTo(trace);
                if (resultingCall.getStatus() == INCOMPLETE_TYPE_INFERENCE) {
                    return OverloadResolutionResultsImpl.incompleteTypeInference(resultingCall);
                }
            }
            if (results.isAmbiguity()) {
                tracing.recordAmbiguity(trace, results.getResultingCalls());
                if (allIncomplete(results.getResultingCalls())) {
                    tracing.cannotCompleteResolve(trace, results.getResultingCalls());
                    return OverloadResolutionResultsImpl.incompleteTypeInference(results.getResultingCalls());
                }
                // This check is needed for the following case:
                //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
                if (allClean(results.getResultingCalls())) {
                    tracing.ambiguity(trace, results.getResultingCalls());
                }
            }
            return results;
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() != 1) {
                // This is needed when there are several overloads some of which are OK but for nullability of the receiver,
                // and some are not OK at all. In this case we'd like to say "unsafe call" rather than "none applicable"
                // Used to be: weak errors. Generalized for future extensions
                for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
                    Set<ResolvedCallWithTrace<D>> thisLevel = Sets.newLinkedHashSet();
                    for (ResolvedCallWithTrace<D> candidate : failedCandidates) {
                        if (severityLevel.contains(candidate.getStatus())) {
                            thisLevel.add(candidate);
                        }
                    }
                    if (!thisLevel.isEmpty()) {
                        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false);
                        if (results.isSingleResult()) {
                            results.getResultingCall().getTrace().moveAllMyDataTo(trace);
                            return OverloadResolutionResultsImpl.singleFailedCandidate(results.getResultingCall());
                        }

                        tracing.noneApplicable(trace, results.getResultingCalls());
                        tracing.recordAmbiguity(trace, results.getResultingCalls());
                        return OverloadResolutionResultsImpl.manyFailedCandidates(results.getResultingCalls());
                    }
                }

                assert false : "Should not be reachable, cause every status must belong to some level";

                Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(failedCandidates, MAP_TO_CANDIDATE);
                if (noOverrides.size() != 1) {
                    tracing.noneApplicable(trace, noOverrides);
                    tracing.recordAmbiguity(trace, noOverrides);
                    return OverloadResolutionResultsImpl.manyFailedCandidates(noOverrides);
                }

                failedCandidates = noOverrides;
            }

            ResolvedCallWithTrace<D> failed = failedCandidates.iterator().next();
            failed.getTrace().moveAllMyDataTo(trace);
            return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
        }
        else {
            tracing.unresolvedReference(trace);
            return OverloadResolutionResultsImpl.nameNotFound();
        }
    }

    private static <D extends CallableDescriptor> boolean allClean(@NotNull Collection<ResolvedCallWithTrace<D>> results) {
        for (ResolvedCallWithTrace<D> result : results) {
            if (result.isDirty()) return false;
        }
        return true;
    }

    private static <D extends CallableDescriptor> boolean allIncomplete(@NotNull Collection<ResolvedCallWithTrace<D>> results) {
        for (ResolvedCallWithTrace<D> result : results) {
            if (result.getStatus() != INCOMPLETE_TYPE_INFERENCE) return false;
        }
        return true;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(
            @NotNull Set<ResolvedCallWithTrace<D>> candidates,
            boolean discriminateGenerics
    ) {
        if (candidates.size() != 1) {
            Set<ResolvedCallWithTrace<D>> cleanCandidates = Sets.newLinkedHashSet(candidates);
            for (Iterator<ResolvedCallWithTrace<D>> iterator = cleanCandidates.iterator(); iterator.hasNext(); ) {
                ResolvedCallWithTrace<D> candidate = iterator.next();
                if (candidate.isDirty()) {
                    iterator.remove();
                }
            }

            if (cleanCandidates.isEmpty()) {
                cleanCandidates = candidates;
            }
            ResolvedCallWithTrace<D> maximallySpecific = OverloadingConflictResolver.INSTANCE.findMaximallySpecific(cleanCandidates, false);
            if (maximallySpecific != null) {
                return OverloadResolutionResultsImpl.success(maximallySpecific);
            }

            if (discriminateGenerics) {
                ResolvedCallWithTrace<D> maximallySpecificGenericsDiscriminated = OverloadingConflictResolver.INSTANCE.findMaximallySpecific(cleanCandidates, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
                }
            }

            Set<ResolvedCallWithTrace<D>> noOverrides = OverridingUtil.filterOverrides(candidates, MAP_TO_RESULT);

            return OverloadResolutionResultsImpl.ambiguity(noOverrides);
        }
        else {
            ResolvedCallWithTrace<D> result = candidates.iterator().next();

            return OverloadResolutionResultsImpl.success(result);
        }
    }


}
