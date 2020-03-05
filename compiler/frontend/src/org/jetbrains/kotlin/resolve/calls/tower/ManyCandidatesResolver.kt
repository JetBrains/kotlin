/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.typeForTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.types.TypeConstructor

abstract class ManyCandidatesResolver<D : CallableDescriptor>(
    private val psiCallResolver: PSICallResolver,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    protected val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    protected val callComponents: KotlinCallComponents,
    val builtIns: KotlinBuiltIns
) : InferenceSession {
    protected val partiallyResolvedCallsInfo = arrayListOf<PSIPartialCallInfo>()
    private val errorCallsInfo = arrayListOf<PSIErrorCallInfo<D>>()
    private val completedCalls = hashSetOf<ResolvedAtom>()

    open fun prepareForCompletion(commonSystem: NewConstraintSystem, resolvedCallsInfo: List<PSIPartialCallInfo>) {
        // do nothing
    }

    override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean {
        return false
    }

    override fun addPartialCallInfo(callInfo: PartialCallInfo) {
        if (callInfo !is PSIPartialCallInfo) {
            throw AssertionError("Call info for $callInfo should be instance of PSIPartialCallInfo")
        }
        partiallyResolvedCallsInfo.add(callInfo)
    }

    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        // do nothing
    }

    override fun addErrorCallInfo(callInfo: ErrorCallInfo) {
        if (callInfo !is PSIErrorCallInfo<*>) {
            throw AssertionError("Error call info for $callInfo should be instance of PSIErrorCallInfo")
        }
        @Suppress("UNCHECKED_CAST")
        errorCallsInfo.add(callInfo as PSIErrorCallInfo<D>)
    }

    override fun currentConstraintSystem(): ConstraintStorage {
        return partiallyResolvedCallsInfo.lastOrNull()?.callResolutionResult?.constraintSystem ?: ConstraintStorage.Empty
    }

    override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean =
        !completedCalls.add(resolvedAtom)

    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true

    fun resolveCandidates(resolutionCallbacks: KotlinResolutionCallbacks): List<ResolutionResultCallInfo<D>> {
        val resolvedCallsInfo = partiallyResolvedCallsInfo.toList()

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()

        val hasOneSuccessfulAndOneErrorCandidate = if (resolvedCallsInfo.size > 1) {
            val hasErrors = resolvedCallsInfo.map {
                it.callResolutionResult.constraintSystem.errors.isNotEmpty() || it.callResolutionResult.diagnostics.isNotEmpty()
            }
            hasErrors.any { it } && !hasErrors.all { it }
        } else {
            false
        }

        fun runCompletion(constraintSystem: NewConstraintSystem, atoms: List<ResolvedAtom>) {
            kotlinConstraintSystemCompleter.runCompletion(
                constraintSystem.asConstraintSystemCompleterContext(),
                KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.FULL,
                atoms,
                builtIns.unitType,
                diagnosticHolder
            ) {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(), resolutionCallbacks, it, diagnosticHolder
                )
            }

        }

        val allCandidates = arrayListOf<ResolutionResultCallInfo<D>>()

        if (hasOneSuccessfulAndOneErrorCandidate) {
            val goodCandidate = resolvedCallsInfo.first { it.callResolutionResult.constraintSystem.errors.isEmpty() && it.callResolutionResult.diagnostics.isEmpty() }
            val badCandidate = resolvedCallsInfo.first { it.callResolutionResult.constraintSystem.errors.isNotEmpty() || it.callResolutionResult.diagnostics.isNotEmpty()}

            for (callInfo in listOf(goodCandidate, badCandidate)) {
                val atomsToAnalyze = mutableListOf<ResolvedAtom>(callInfo.callResolutionResult)
                val system = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns).apply {
                    addOtherSystem(callInfo.callResolutionResult.constraintSystem)
                    /*
                     * This is needed for very stupid case, when we have some delegate with good `getValue` and bad `setValue` that
                     *   was provided by some function call with generic (e.g. var x by lazy { "" })
                     * The problem is that we want to complete candidates for `getValue` and `setValue` separately, so diagnostics
                     *   from `setValue` don't leak into resolved call of `getValue`, but both calls can have same
                     *   atom for receiver (and type variables from it). After we complete first call, completion of
                     *   second call fails because it's atoms don't contains type variable from receiver, because
                     *   they was completed in first call
                     * To fix that we add equality constraints from first call to system of second call and create
                     *   stub atoms in order to call completer doesn't fail
                     */
                    if (callInfo === badCandidate) {
                        for ((typeVariable, fixedType) in allCandidates[0].resolutionResult.constraintSystem.fixedTypeVariables) {
                            if (typeVariable in this.notFixedTypeVariables) {
                                val type = (typeVariable as TypeConstructor).typeForTypeVariable()
                                addEqualityConstraint(
                                    type,
                                    fixedType,
                                    SimpleConstraintSystemConstraintPosition
                                )
                                atomsToAnalyze += StubResolvedAtom(typeVariable)
                            }
                        }
                    }
                }
                runCompletion(system, atomsToAnalyze)
                val resolutionResult = callInfo.asCallResolutionResult(diagnosticHolder, system)
                allCandidates += ResolutionResultCallInfo(
                    resolutionResult,
                    psiCallResolver.convertToOverloadResolutionResults(callInfo.context, resolutionResult, callInfo.tracingStrategy)
                )
            }
        } else {
            val commonSystem = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns).apply {
                addOtherSystem(currentConstraintSystem())
            }

            prepareForCompletion(commonSystem, resolvedCallsInfo)
            runCompletion(commonSystem, resolvedCallsInfo.map { it.callResolutionResult })
            resolvedCallsInfo.mapTo(allCandidates) {
                val resolutionResult = it.asCallResolutionResult(diagnosticHolder, commonSystem)
                ResolutionResultCallInfo(
                    resolutionResult, psiCallResolver.convertToOverloadResolutionResults(it.context, resolutionResult, it.tracingStrategy)
                )
            }
        }

        val results = allCandidates.map { it.resolutionResult }
        errorCallsInfo.filter { it.callResolutionResult !in results }.mapTo(allCandidates) { ResolutionResultCallInfo(it.callResolutionResult, it.result) }
        return allCandidates
    }

    private fun PartialCallInfo.asCallResolutionResult(
        diagnosticsHolder: KotlinDiagnosticsHolder.SimpleHolder,
        commonSystem: NewConstraintSystem
    ): CallResolutionResult {
        val diagnostics = diagnosticsHolder.getDiagnostics() + callResolutionResult.diagnostics + commonSystem.diagnostics
        return CompletedCallResolutionResult(callResolutionResult.resultCallAtom, diagnostics, commonSystem.asReadOnlyStorage())
    }
}

data class ResolutionResultCallInfo<D : CallableDescriptor>(
    val resolutionResult: CallResolutionResult,
    val overloadResolutionResults: OverloadResolutionResults<D>
)

abstract class CallInfo(
    open val callResolutionResult: SingleCallResolutionResult,
    val context: BasicCallResolutionContext,
    val tracingStrategy: TracingStrategy
)

class PSIPartialCallInfo(
    override val callResolutionResult: PartialCallResolutionResult,
    context: BasicCallResolutionContext,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), PartialCallInfo

class PSICompletedCallInfo(
    override val callResolutionResult: CompletedCallResolutionResult,
    context: BasicCallResolutionContext,
    val resolvedCall: ResolvedCall<*>,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), CompletedCallInfo

class PSIErrorCallInfo<D : CallableDescriptor>(
    override val callResolutionResult: CallResolutionResult,
    val result: OverloadResolutionResults<D>
) : ErrorCallInfo