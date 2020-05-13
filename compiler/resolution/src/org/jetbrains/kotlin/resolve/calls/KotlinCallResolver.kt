/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType
import java.lang.UnsupportedOperationException


class KotlinCallResolver(
    private val towerResolver: TowerResolver,
    private val kotlinCallCompleter: KotlinCallCompleter,
    private val overloadingConflictResolver: NewOverloadingConflictResolver,
    private val callableReferenceResolver: CallableReferenceResolver,
    private val callComponents: KotlinCallComponents
) {
    companion object {
        private val OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION = FqName.fromSegments(listOf("kotlin", "OverloadResolutionByLambdaReturnType"))
    }

    fun resolveCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        expectedType: UnwrappedType?,
        collectAllCandidates: Boolean,
        createFactoryProviderForInvoke: () -> CandidateFactoryProviderForInvoke<KotlinResolutionCandidate>
    ): CallResolutionResult {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        kotlinCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(
            callComponents, scopeTower, kotlinCall, resolutionCallbacks, callableReferenceResolver
        )
        val processor = when (kotlinCall.callKind) {
            KotlinCallKind.VARIABLE -> {
                createVariableAndObjectProcessor(scopeTower, kotlinCall.name, candidateFactory, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.FUNCTION -> {
                createFunctionProcessor(
                    scopeTower,
                    kotlinCall.name,
                    candidateFactory,
                    createFactoryProviderForInvoke(),
                    kotlinCall.explicitReceiver?.receiver
                )
            }
            KotlinCallKind.INVOKE -> {
                createProcessorWithReceiverValueOrEmpty(kotlinCall.explicitReceiver?.receiver) {
                    createCallTowerProcessorForExplicitInvoke(
                        scopeTower,
                        candidateFactory,
                        kotlinCall.dispatchReceiverForInvokeExtension?.receiver as ReceiverValueWithSmartCastInfo,
                        it
                    )
                }
            }
            KotlinCallKind.UNSUPPORTED -> throw UnsupportedOperationException()
        }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.collectAllCandidates(scopeTower, processor, kotlinCall.name)
            return kotlinCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)
        }

        val candidates = towerResolver.runResolve(
            scopeTower,
            processor,
            useOrder = kotlinCall.callKind != KotlinCallKind.UNSUPPORTED,
            name = kotlinCall.name
        )

        return choseMostSpecific(candidateFactory, resolutionCallbacks, expectedType, candidates)
    }

    fun resolveGivenCandidates(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        expectedType: UnwrappedType?,
        givenCandidates: Collection<GivenCandidate>,
        collectAllCandidates: Boolean
    ): CallResolutionResult {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        kotlinCall.checkCallInvariants()
        val candidateFactory = SimpleCandidateFactory(
            callComponents, scopeTower, kotlinCall, resolutionCallbacks, callableReferenceResolver
        )

        val resolutionCandidates = givenCandidates.map { candidateFactory.createCandidate(it).forceResolution() }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolutionCandidates),
                TowerResolver.AllCandidatesCollector(),
                useOrder = false
            )
            return kotlinCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)

        }
        val candidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolutionCandidates),
            TowerResolver.SuccessfulResultCollector(),
            useOrder = true
        )
        return choseMostSpecific(candidateFactory, resolutionCallbacks, expectedType, candidates)
    }

    private fun choseMostSpecific(
        candidateFactory: SimpleCandidateFactory,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        candidates: Collection<KotlinResolutionCandidate>
    ): CallResolutionResult {
        var refinedCandidates = candidates
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority)) {
            val nonSynthesized = candidates.filter { !it.resolvedCall.candidateDescriptor.isSynthesized }
            if (!nonSynthesized.isEmpty()) {
                refinedCandidates = nonSynthesized
            }
        }

        var maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
            refinedCandidates,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            discriminateGenerics = true // todo
        )

        if (
            maximallySpecificCandidates.size > 1 &&
            callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FactoryPatternResolution) &&
            candidates.all { resolutionCallbacks.inferenceSession.shouldRunCompletion(it) }
        ) {
            val candidatesWithAnnotation =
                candidates.filter { it.resolvedCall.candidateDescriptor.annotations.hasAnnotation(OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION) }
            if (candidatesWithAnnotation.isNotEmpty()) {
                val newCandidates = kotlinCallCompleter.chooseCandidateRegardingFactoryPatternResolution(maximallySpecificCandidates, resolutionCallbacks)
                maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
                    newCandidates,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    discriminateGenerics = true
                )

                if (maximallySpecificCandidates.size > 1) {
                    maximallySpecificCandidates = candidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
                }
            }
        }

        return kotlinCallCompleter.runCompletion(candidateFactory, maximallySpecificCandidates, expectedType, resolutionCallbacks)
    }
}

