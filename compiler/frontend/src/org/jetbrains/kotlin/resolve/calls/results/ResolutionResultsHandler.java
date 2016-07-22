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

package org.jetbrains.kotlin.resolve.calls.results;

import com.google.common.collect.Sets;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;

import java.util.*;

import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*;

public class ResolutionResultsHandler {
    private static final Function1<MutableResolvedCall<?>, CallableDescriptor> MAP_RESOLVED_CALL_TO_RESULTING_DESCRIPTOR =
            new Function1<MutableResolvedCall<?>, CallableDescriptor>() {
                @Override
                public CallableDescriptor invoke(MutableResolvedCall<?> resolvedCall) {
                    return resolvedCall.getResultingDescriptor();
                }
            };

    private static final Function1<MutableResolvedCall<?>, Integer> MAP_RESOLVED_CALL_TO_SOURCE_PRESENCE =
            new Function1<MutableResolvedCall<?>, Integer>() {
                @Override
                public Integer invoke(MutableResolvedCall<?> resolvedCall) {
                    return DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.getResultingDescriptor()) != null ? 0 : 1;
                }
            };

    private final OverloadingConflictResolver overloadingConflictResolver;

    public ResolutionResultsHandler(@NotNull OverloadingConflictResolver overloadingConflictResolver) {
        this.overloadingConflictResolver = overloadingConflictResolver;
    }

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull CallResolutionContext context,
            @NotNull TracingStrategy tracing,
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
            return computeSuccessfulResult(context, tracing, successfulCandidates, incompleteCandidates, context.checkArguments);
        }
        else if (!failedCandidates.isEmpty()) {
            return computeFailedResult(tracing, context.trace, failedCandidates, context.checkArguments);
        }
        if (!candidatesWithWrongReceiver.isEmpty()) {
            tracing.unresolvedReferenceWrongReceiver(context.trace, candidatesWithWrongReceiver);
            return OverloadResolutionResultsImpl.candidatesWithWrongReceiver(candidatesWithWrongReceiver);
        }
        tracing.unresolvedReference(context.trace);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeSuccessfulResult(
            @NotNull CallResolutionContext context,
            @NotNull TracingStrategy tracing,
            @NotNull Set<MutableResolvedCall<D>> successfulCandidates,
            @NotNull Set<MutableResolvedCall<D>> incompleteCandidates,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        Set<MutableResolvedCall<D>> successfulAndIncomplete = Sets.newLinkedHashSet();
        successfulAndIncomplete.addAll(successfulCandidates);
        successfulAndIncomplete.addAll(incompleteCandidates);
        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(
                successfulAndIncomplete, true, context.isDebuggerContext, checkArgumentsMode);
        if (results.isSingleResult()) {
            MutableResolvedCall<D> resultingCall = results.getResultingCall();
            resultingCall.getTrace().moveAllMyDataTo(context.trace);
            if (resultingCall.getStatus() == INCOMPLETE_TYPE_INFERENCE) {
                return OverloadResolutionResultsImpl.incompleteTypeInference(resultingCall);
            }
        }
        if (results.isAmbiguity()) {
            tracing.recordAmbiguity(context.trace, results.getResultingCalls());
            boolean allCandidatesIncomplete = allIncomplete(results.getResultingCalls());
            // This check is needed for the following case:
            //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
            if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ||
                    !CallUtilKt.hasUnresolvedArguments(context.call, context)) {
                if (allCandidatesIncomplete) {
                    tracing.cannotCompleteResolve(context.trace, results.getResultingCalls());
                }
                else {
                    tracing.ambiguity(context.trace, results.getResultingCalls());
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
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull Set<MutableResolvedCall<D>> failedCandidates,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        if (failedCandidates.size() == 1) {
            return recordFailedInfo(tracing, trace, failedCandidates);
        }

        for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
            Set<MutableResolvedCall<D>> thisLevel = Sets.newLinkedHashSet();
            for (MutableResolvedCall<D> candidate : failedCandidates) {
                if (severityLevel.contains(candidate.getStatus())) {
                    thisLevel.add(candidate);
                }
            }
            if (!thisLevel.isEmpty()) {
                if (severityLevel.contains(ARGUMENTS_MAPPING_ERROR)) {
                    return recordFailedInfo(tracing, trace, thisLevel);
                }
                OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false, false, checkArgumentsMode);
                return recordFailedInfo(tracing, trace, results.getResultingCalls());
            }
        }

        throw new AssertionError("Should not be reachable, cause every status must belong to some level: " + failedCandidates);
    }

    @NotNull
    private static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> recordFailedInfo(
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull Collection<MutableResolvedCall<D>> candidates
    ) {
        if (candidates.size() == 1) {
            MutableResolvedCall<D> failed = candidates.iterator().next();
            failed.getTrace().moveAllMyDataTo(trace);
            return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
        }
        tracing.noneApplicable(trace, candidates);
        tracing.recordAmbiguity(trace, candidates);
        return OverloadResolutionResultsImpl.manyFailedCandidates(candidates);
    }

    private static <D extends CallableDescriptor> boolean allIncomplete(@NotNull Collection<MutableResolvedCall<D>> results) {
        for (MutableResolvedCall<D> result : results) {
            if (result.getStatus() != INCOMPLETE_TYPE_INFERENCE) return false;
        }
        return true;
    }

    // Sometimes we should compare "copies" from sources and from binary files.
    // But we cannot compare return types for such copies, because it may lead us to recursive problem (see KT-11995).
    // Because of this we compare them without return type and choose descriptor from source if we found duplicate.
    @NotNull
    private static <D extends CallableDescriptor> Set<MutableResolvedCall<D>> filterOutEquivalentCalls(
            @NotNull Set<MutableResolvedCall<D>> candidates
    ) {
        if (candidates.size() <= 1) return candidates;

        List<MutableResolvedCall<D>> fromSourcesGoesFirst = CollectionsKt.sortedBy(candidates, MAP_RESOLVED_CALL_TO_SOURCE_PRESENCE);

        Set<MutableResolvedCall<D>> result = new LinkedHashSet<MutableResolvedCall<D>>();
        outerLoop:
        for (MutableResolvedCall<D> meD : fromSourcesGoesFirst) {
            for (MutableResolvedCall<D> otherD : result) {
                D me = meD.getResultingDescriptor();
                D other = otherD.getResultingDescriptor();
                boolean ignoreReturnType = (DescriptorToSourceUtils.descriptorToDeclaration(me) == null) !=
                                           (DescriptorToSourceUtils.descriptorToDeclaration(other) == null);
                if (DescriptorEquivalenceForOverrides.INSTANCE.areCallableDescriptorsEquivalent(me, other, ignoreReturnType)) {
                    continue outerLoop;
                }
            }
            result.add(meD);
        }

        return result;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenerics,
            boolean isDebuggerContext,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        if (candidates.size() == 1) {
            return OverloadResolutionResultsImpl.success(candidates.iterator().next());
        }

        if (candidates.iterator().next() instanceof VariableAsFunctionResolvedCall) {
            candidates = overloadingConflictResolver.findMaximallySpecificVariableAsFunctionCalls(candidates);
        }

        Set<MutableResolvedCall<D>> noEquivalentCalls = filterOutEquivalentCalls(candidates);
        Set<MutableResolvedCall<D>> noOverrides =
                OverridingUtil.filterOverrides(noEquivalentCalls, MAP_RESOLVED_CALL_TO_RESULTING_DESCRIPTOR);
        if (noOverrides.size() == 1) {
            return OverloadResolutionResultsImpl.success(noOverrides.iterator().next());
        }

        MutableResolvedCall<D> maximallySpecific = overloadingConflictResolver.findMaximallySpecific(noOverrides, checkArgumentsMode, false, isDebuggerContext);
        if (maximallySpecific != null) {
            return OverloadResolutionResultsImpl.success(maximallySpecific);
        }

        if (discriminateGenerics) {
            MutableResolvedCall<D> maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(
                    noOverrides, checkArgumentsMode, true, isDebuggerContext);
            if (maximallySpecificGenericsDiscriminated != null) {
                return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
            }
        }

        return OverloadResolutionResultsImpl.ambiguity(noOverrides);
    }
}
