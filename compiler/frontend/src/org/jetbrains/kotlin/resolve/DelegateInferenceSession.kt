/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.DelegatedPropertyConstraintPositionImpl
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.StubTypesBasedInferenceSession
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.calls.tower.PSIPartialCallInfo
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.util.OperatorNameConventions

class DelegateInferenceSession(
    val variableDescriptor: VariableDescriptorWithAccessors,
    val expectedType: UnwrappedType?,
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    callComponents: KotlinCallComponents,
    builtIns: KotlinBuiltIns,
    override val parentSession: InferenceSession?
) : StubTypesBasedInferenceSession<FunctionDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents, builtIns
) {
    init {
        if (parentSession is StubTypesBasedInferenceSession<*>) {
            parentSession.addNestedInferenceSession(this)
        }
    }

    fun getNestedBuilderInferenceSessions(): List<BuilderInferenceSession> {
        val builderInferenceSessions = nestedInferenceSessions.filterIsInstance<BuilderInferenceSession>()
        val delegatedPropertyInferenceSessions = nestedInferenceSessions.filterIsInstance<DelegateInferenceSession>()

        return builderInferenceSessions + delegatedPropertyInferenceSessions.map { it.getNestedBuilderInferenceSessions() }.flatten()
    }

    override fun prepareForCompletion(commonSystem: NewConstraintSystem, resolvedCallsInfo: List<PSIPartialCallInfo>) {
        val csBuilder = commonSystem.getBuilder()
        for (callInfo in resolvedCallsInfo) {
            val resultAtom = callInfo.callResolutionResult.resultCallAtom
            when (resultAtom.candidateDescriptor.name) {
                OperatorNameConventions.GET_VALUE -> resultAtom.addConstraintsForGetValueMethod(csBuilder)
                OperatorNameConventions.SET_VALUE -> resultAtom.addConstraintsForSetValueMethod(csBuilder)
            }
        }
    }

    private fun ResolvedCallAtom.addConstraintForThis(descriptor: CallableDescriptor, commonSystem: ConstraintSystemBuilder) {
        val typeOfThis = variableDescriptor.extensionReceiverParameter?.type
            ?: variableDescriptor.dispatchReceiverParameter?.type
            ?: builtIns.nullableNothingType

        val valueParameterForThis = descriptor.valueParameters.getOrNull(0) ?: return
        val substitutedType = freshVariablesSubstitutor.safeSubstitute(valueParameterForThis.type.unwrap())
        commonSystem.addSubtypeConstraint(typeOfThis.unwrap(), substitutedType, DelegatedPropertyConstraintPositionImpl(atom))
    }

    private fun ResolvedCallAtom.addConstraintsForGetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val unsubstitutedReturnType = candidateDescriptor.returnType?.unwrap() ?: return
            val substitutedReturnType = freshVariablesSubstitutor.safeSubstitute(unsubstitutedReturnType)

            commonSystem.addSubtypeConstraint(substitutedReturnType, expectedType, DelegatedPropertyConstraintPositionImpl(atom))
        }

        addConstraintForThis(candidateDescriptor, commonSystem)
    }

    private fun ResolvedCallAtom.addConstraintsForSetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val unsubstitutedParameterType = candidateDescriptor.valueParameters.getOrNull(2)?.type?.unwrap() ?: return
            val substitutedParameterType = freshVariablesSubstitutor.safeSubstitute(unsubstitutedParameterType)

            commonSystem.addSubtypeConstraint(expectedType, substitutedParameterType, DelegatedPropertyConstraintPositionImpl(atom))
        }

        addConstraintForThis(candidateDescriptor, commonSystem)
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType> = emptyMap()

    override fun initializeLambda(lambda: ResolvedLambdaAtom) {}

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false

    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
}

class InferenceSessionForExistingCandidates(
    private val resolveReceiverIndependently: Boolean,
    override val parentSession: InferenceSession?
) : InferenceSession {
    override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean {
        return !ErrorUtils.isError(candidate.resolvedCall.candidateDescriptor)
    }

    override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
    override fun addErrorCallInfo(callInfo: ErrorCallInfo) {}

    override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType> = emptyMap()

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
    override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean {
        return !ErrorUtils.isError(resolvedCallAtom.candidateDescriptor)
    }

    override fun computeCompletionMode(
        candidate: ResolutionCandidate
    ): ConstraintSystemCompletionMode? = null

    override fun resolveReceiverIndependently(): Boolean = resolveReceiverIndependently

    override fun initializeLambda(lambda: ResolvedLambdaAtom) {}
}
