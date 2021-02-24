/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.calls.tower.TowerUtilsKt;
import org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsForResolveKt;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;
import org.jetbrains.kotlin.util.CancellationChecker;

import java.util.*;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*;

public class ResolutionResultsHandler {

    private final OverloadingConflictResolver<MutableResolvedCall<?>> overloadingConflictResolver;

    public ResolutionResultsHandler(
            @NotNull KotlinBuiltIns builtIns,
            @NotNull ModuleDescriptor module,
            @NotNull TypeSpecificityComparator specificityComparator,
            @NotNull PlatformOverloadsSpecificityComparator platformOverloadsSpecificityComparator,
            @NotNull CancellationChecker cancellationChecker,
            @NotNull KotlinTypeRefiner kotlinTypeRefiner
    ) {
        overloadingConflictResolver = FlatSignatureForResolvedCallKt.createOverloadingConflictResolver(
                builtIns, module, specificityComparator, platformOverloadsSpecificityComparator, cancellationChecker, kotlinTypeRefiner
        );
    }

    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            @NotNull CallResolutionContext context,
            @NotNull TracingStrategy tracing,
            @NotNull Collection<MutableResolvedCall<D>> candidates,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        Set<MutableResolvedCall<D>> successfulCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> failedCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> incompleteCandidates = new LinkedHashSet<>();
        Set<MutableResolvedCall<D>> candidatesWithWrongReceiver = new LinkedHashSet<>();
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
            return computeSuccessfulResult(
                    context, tracing, successfulCandidates, incompleteCandidates, context.checkArguments, languageVersionSettings);
        }
        else if (!failedCandidates.isEmpty()) {
            return computeFailedResult(tracing, context.trace, failedCandidates, context.checkArguments, languageVersionSettings);
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
            @NotNull CallResolutionContext<?> context,
            @NotNull TracingStrategy tracing,
            @NotNull Set<MutableResolvedCall<D>> successfulCandidates,
            @NotNull Set<MutableResolvedCall<D>> incompleteCandidates,
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        Set<MutableResolvedCall<D>> successfulAndIncomplete = new LinkedHashSet<>();
        successfulAndIncomplete.addAll(successfulCandidates);
        successfulAndIncomplete.addAll(incompleteCandidates);
        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(
                successfulAndIncomplete, true, checkArgumentsMode, languageVersionSettings);
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
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (failedCandidates.size() == 1) {
            return recordFailedInfo(tracing, trace, failedCandidates);
        }

        for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
            Set<MutableResolvedCall<D>> thisLevel = new LinkedHashSet<>();
            for (MutableResolvedCall<D> candidate : failedCandidates) {
                if (severityLevel.contains(candidate.getStatus())) {
                    thisLevel.add(candidate);
                }
            }
            if (!thisLevel.isEmpty()) {
                if (severityLevel.contains(ARGUMENTS_MAPPING_ERROR)) {
                    @SuppressWarnings("unchecked")
                    OverloadingConflictResolver<MutableResolvedCall<D>> myResolver = (OverloadingConflictResolver) overloadingConflictResolver;
                    return recordFailedInfo(tracing, trace, myResolver.filterOutEquivalentCalls(new LinkedHashSet<>(thisLevel)));
                }
                OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(
                        thisLevel, false, checkArgumentsMode, languageVersionSettings);
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

    @NotNull
    @SuppressWarnings("unchecked")
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenerics,
            @NotNull CheckArgumentTypesMode checkArgumentsMode,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        OverloadingConflictResolver<MutableResolvedCall<D>> myResolver = (OverloadingConflictResolver) overloadingConflictResolver;

        Set<MutableResolvedCall<D>> refinedCandidates = candidates;
        if (!languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority)) {
            Set<MutableResolvedCall<D>> nonSynthesized = new HashSet<>();
            for (MutableResolvedCall<D> candidate : candidates) {
                if (!TowerUtilsKt.isSynthesized(candidate.getCandidateDescriptor())) {
                    nonSynthesized.add(candidate);
                }
            }

            if (!nonSynthesized.isEmpty()) {
                refinedCandidates = nonSynthesized;
            }
        }

        Set<MutableResolvedCall<D>> specificCalls =
                myResolver.chooseMaximallySpecificCandidates(refinedCandidates, checkArgumentsMode, discriminateGenerics);

        if (specificCalls.size() > 1) {
            specificCalls = specificCalls.stream()
                    .filter((call) ->
                                    !call.getCandidateDescriptor().getAnnotations().hasAnnotation(
                                            AnnotationsForResolveKt.getOVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME())
                    ).collect(Collectors.toSet());
        }

        if (specificCalls.size() == 1) {
            return OverloadResolutionResultsImpl.success(specificCalls.iterator().next());
        }
        else {
            return OverloadResolutionResultsImpl.ambiguity(specificCalls);
        }
    }
}
