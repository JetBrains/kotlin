/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType


class KotlinCallResolver(
    private val towerResolver: TowerResolver,
    private val kotlinCallCompleter: KotlinCallCompleter,
    private val overloadingConflictResolver: NewOverloadingConflictResolver,
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val callComponents: KotlinCallComponents
) {
    fun resolveAndCompleteCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        expectedType: UnwrappedType?,
        collectAllCandidates: Boolean,
    ): CallResolutionResult {
        val candidateFactory = createFactory(scopeTower, kotlinCall, resolutionCallbacks, expectedType)
        val candidates = resolveCall(scopeTower, resolutionCallbacks, kotlinCall, collectAllCandidates, candidateFactory)

        if (collectAllCandidates) {
            return kotlinCallCompleter.createAllCandidatesResult(candidates, expectedType, resolutionCallbacks)
        }

        return kotlinCallCompleter.runCompletion(candidateFactory, candidates, expectedType, resolutionCallbacks)
    }

    fun resolveCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        expectedType: UnwrappedType?,
        collectAllCandidates: Boolean,
    ): Collection<ResolutionCandidate> {
        val candidateFactory = createFactory(scopeTower, kotlinCall, resolutionCallbacks, expectedType)
        return resolveCall(scopeTower, resolutionCallbacks, kotlinCall, collectAllCandidates, candidateFactory)
    }

    fun resolveAndCompleteGivenCandidates(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        expectedType: UnwrappedType?,
        givenCandidates: Collection<GivenCandidate>,
        collectAllCandidates: Boolean
    ): CallResolutionResult {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        kotlinCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(callComponents, scopeTower, kotlinCall, resolutionCallbacks)
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
        val mostSpecificCandidates = choseMostSpecific(kotlinCall, resolutionCallbacks, candidates)

        return kotlinCallCompleter.runCompletion(candidateFactory, mostSpecificCandidates, expectedType, resolutionCallbacks)
    }

    fun resolveCallableReferenceArgument(
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): Collection<CallableReferenceResolutionCandidate> {
        val scopeTower = callComponents.statelessCallbacks.getScopeTowerForCallableReferenceArgument(argument)
        val factory = createCallableReferenceCallFactory(scopeTower, argument.call, resolutionCallbacks, expectedType, argument, baseSystem)

        return resolveCall(scopeTower, resolutionCallbacks, argument.call, collectAllCandidates = false, factory)
    }

    private fun createCallableReferenceCallFactory(
        scopeTower: ImplicitScopeTower,
        kotlinCall: KotlinCall,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        argument: CallableReferenceKotlinCallArgument? = null,
        baseSystem: ConstraintStorage? = null
    ): CandidateFactory<CallableReferenceResolutionCandidate> {
        val resolutionAtom = argument
            ?: CallableReferenceKotlinCall(kotlinCall, resolutionCallbacks.getLhsResult(kotlinCall), kotlinCall.name)

        return CallableReferencesCandidateFactory(resolutionAtom, callComponents, scopeTower, expectedType, baseSystem, resolutionCallbacks)
    }

    private fun createSimpleCallFactory(
        scopeTower: ImplicitScopeTower,
        kotlinCall: KotlinCall,
        resolutionCallbacks: KotlinResolutionCallbacks,
    ): CandidateFactory<ResolutionCandidate> = SimpleCandidateFactory(callComponents, scopeTower, kotlinCall, resolutionCallbacks)

    private fun createFactory(
        scopeTower: ImplicitScopeTower,
        kotlinCall: KotlinCall,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?
    ): CandidateFactory<ResolutionCandidate> =
        when (kotlinCall.callKind) {
            KotlinCallKind.CALLABLE_REFERENCE -> createCallableReferenceCallFactory(scopeTower, kotlinCall, resolutionCallbacks, expectedType)
            else -> createSimpleCallFactory(scopeTower, kotlinCall, resolutionCallbacks)
        }

    private fun <C : ResolutionCandidate> resolveCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        kotlinCall: KotlinCall,
        collectAllCandidates: Boolean,
        candidateFactory: CandidateFactory<C>,
    ): Collection<C> {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        kotlinCall.checkCallInvariants()

        @Suppress("UNCHECKED_CAST")
        val processor = when (kotlinCall.callKind) {
            KotlinCallKind.VARIABLE -> {
                createVariableAndObjectProcessor(scopeTower, kotlinCall.name, candidateFactory, kotlinCall.explicitReceiver?.receiver)
            }
            KotlinCallKind.FUNCTION -> {
                createFunctionProcessor(
                    scopeTower,
                    kotlinCall.name,
                    candidateFactory,
                    resolutionCallbacks.getCandidateFactoryForInvoke(scopeTower, kotlinCall),
                    kotlinCall.explicitReceiver?.receiver
                ) as ScopeTowerProcessor<C>
            }
            KotlinCallKind.CALLABLE_REFERENCE -> {
                createCallableReferenceProcessor(candidateFactory as CallableReferencesCandidateFactory) as ScopeTowerProcessor<C>
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
            return towerResolver.collectAllCandidates(scopeTower, processor, kotlinCall.name)
        }

        val candidates = towerResolver.runResolve(
            scopeTower,
            processor,
            useOrder = kotlinCall.callKind != KotlinCallKind.UNSUPPORTED,
            name = kotlinCall.name
        )

        @Suppress("UNCHECKED_CAST")
        return choseMostSpecific(kotlinCall, resolutionCallbacks, candidates) as Set<C>
    }

    private fun choseMostSpecific(
        kotlinCall: KotlinCall,
        resolutionCallbacks: KotlinResolutionCallbacks,
        candidates: Collection<ResolutionCandidate>
    ): Set<ResolutionCandidate> {
        var refinedCandidates = candidates

        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority) && kotlinCall.callKind != KotlinCallKind.CALLABLE_REFERENCE) {
            val nonSynthesized = candidates.filter { !it.resolvedCall.candidateDescriptor.isSynthesized }
            if (nonSynthesized.isNotEmpty()) {
                refinedCandidates = nonSynthesized
            }
        }

        var maximallySpecificCandidates = if (kotlinCall.callKind == KotlinCallKind.CALLABLE_REFERENCE) {
            @Suppress("UNCHECKED_CAST")
            callableReferenceArgumentResolver.callableReferenceOverloadConflictResolver.chooseMaximallySpecificCandidates(
                refinedCandidates as Collection<CallableReferenceResolutionCandidate>,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                discriminateGenerics = false
            )
        } else {
            overloadingConflictResolver.chooseMaximallySpecificCandidates(
                refinedCandidates,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                discriminateGenerics = true // todo
            )
        }

        if (maximallySpecificCandidates.size > 1) {
            if (maximallySpecificCandidates.size == 2) {
                val enumEntryCandidate = maximallySpecificCandidates.find {
                    val descriptor = it.resolvedCall.candidateDescriptor
                    descriptor is FakeCallableDescriptorForObject && descriptor.classDescriptor.kind == ClassKind.ENUM_ENTRY
                }
                if (enumEntryCandidate != null) {
                    val otherCandidate = maximallySpecificCandidates.find {
                        val candidateDescriptor = it.resolvedCall.candidateDescriptor
                        candidateDescriptor !is FakeCallableDescriptorForObject
                    }
                    if (otherCandidate != null) {
                        val propertyDescriptor = otherCandidate.resolvedCall.candidateDescriptor
                        if (propertyDescriptor is PropertyDescriptor) {
                            val enumEntryDescriptor =
                                (enumEntryCandidate.resolvedCall.candidateDescriptor as FakeCallableDescriptorForObject).classDescriptor
                            otherCandidate.addDiagnostic(EnumEntryAmbiguityWarning(propertyDescriptor, enumEntryDescriptor))
                            return setOf(otherCandidate)
                        }
                    }
                }
            }
            if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.OverloadResolutionByLambdaReturnType) &&
                kotlinCall.callKind != KotlinCallKind.CALLABLE_REFERENCE &&
                candidates.all { it.isSuccessful } &&
                candidates.all { resolutionCallbacks.inferenceSession.shouldRunCompletion(it) }
            ) {
                val candidatesWithAnnotation = candidates.filter {
                    it.resolvedCall.candidateDescriptor.annotations.hasAnnotation(OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME)
                }.toSet()
                val candidatesWithoutAnnotation = candidates - candidatesWithAnnotation
                if (candidatesWithAnnotation.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val newCandidates = kotlinCallCompleter.chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
                        maximallySpecificCandidates as Set<SimpleResolutionCandidate>,
                        resolutionCallbacks
                    )
                    maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
                        newCandidates,
                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        discriminateGenerics = true
                    )

                    if (maximallySpecificCandidates.size > 1 && candidatesWithoutAnnotation.any { it in maximallySpecificCandidates }) {
                        maximallySpecificCandidates = maximallySpecificCandidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
                        maximallySpecificCandidates.singleOrNull()?.addDiagnostic(CandidateChosenUsingOverloadResolutionByLambdaAnnotation())
                    }
                }
            }
        }

        return maximallySpecificCandidates
    }
}
