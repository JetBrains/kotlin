/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
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
    private val partiallyResolvedCallsInfo = arrayListOf<PSIPartialCallInfo>()
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
        errorCallsInfo.add(callInfo as PSIErrorCallInfo<D>)
    }

    override fun currentConstraintSystem(): ConstraintStorage {
        return partiallyResolvedCallsInfo.lastOrNull()?.callResolutionResult?.constraintSystem ?: ConstraintStorage.Empty
    }

    override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean =
        !completedCalls.add(resolvedAtom)

    fun resolveCandidates(resolutionCallbacks: KotlinResolutionCallbacks): List<ResolutionResultCallInfo<D>> {
        val resolvedCallsInfo = partiallyResolvedCallsInfo.toList()

        val commonSystem = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns).apply {
            addOtherSystem(currentConstraintSystem())
        }

        prepareForCompletion(commonSystem, resolvedCallsInfo)

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()

        kotlinConstraintSystemCompleter.runCompletion(
            commonSystem.asConstraintSystemCompleterContext(),
            KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.FULL,
            resolvedCallsInfo.map { it.callResolutionResult },
            builtIns.unitType
        ) {
            postponedArgumentsAnalyzer.analyze(
                commonSystem.asPostponedArgumentsAnalyzerContext(), resolutionCallbacks, it, diagnosticHolder
            )
        }

        val allCandidates = arrayListOf<ResolutionResultCallInfo<D>>()

        resolvedCallsInfo.mapTo(allCandidates) {
            val resolutionResult = it.asCallResolutionResult(diagnosticHolder, commonSystem)
            ResolutionResultCallInfo(
                resolutionResult, psiCallResolver.convertToOverloadResolutionResults(it.context, resolutionResult, it.tracingStrategy)
            )
        }

        errorCallsInfo.mapTo(allCandidates) { ResolutionResultCallInfo(it.callResolutionResult, it.result) }

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

class PSIPartialCallInfo(
    override val callResolutionResult: PartialCallResolutionResult,
    val context: BasicCallResolutionContext,
    val tracingStrategy: TracingStrategy
) : PartialCallInfo

class PSICompletedCallInfo(
    override val callResolutionResult: CompletedCallResolutionResult,
    val context: BasicCallResolutionContext,
    val tracingStrategy: TracingStrategy
) : CompletedCallInfo

class PSIErrorCallInfo<D : CallableDescriptor>(
    override val callResolutionResult: CallResolutionResult,
    val result: OverloadResolutionResults<D>
) : ErrorCallInfo