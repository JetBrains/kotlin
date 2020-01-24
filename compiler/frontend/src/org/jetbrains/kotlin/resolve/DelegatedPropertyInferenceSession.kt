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
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.DelegatedPropertyConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ManyCandidatesResolver
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.calls.tower.PSIPartialCallInfo
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.util.OperatorNameConventions

class DelegatedPropertyInferenceSession(
    val variableDescriptor: VariableDescriptorWithAccessors,
    val expectedType: UnwrappedType?,
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    callComponents: KotlinCallComponents,
    builtIns: KotlinBuiltIns
) : ManyCandidatesResolver<FunctionDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents, builtIns
) {

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
        commonSystem.addSubtypeConstraint(typeOfThis.unwrap(), substitutedType, DelegatedPropertyConstraintPosition(atom))
    }

    private fun ResolvedCallAtom.addConstraintsForGetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val unsubstitutedReturnType = candidateDescriptor.returnType?.unwrap() ?: return
            val substitutedReturnType = freshVariablesSubstitutor.safeSubstitute(unsubstitutedReturnType)

            commonSystem.addSubtypeConstraint(substitutedReturnType, expectedType, DelegatedPropertyConstraintPosition(atom))
        }

        addConstraintForThis(candidateDescriptor, commonSystem)
    }

    private fun ResolvedCallAtom.addConstraintsForSetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val unsubstitutedParameterType = candidateDescriptor.valueParameters.getOrNull(2)?.type?.unwrap() ?: return
            val substitutedParameterType = freshVariablesSubstitutor.safeSubstitute(unsubstitutedParameterType)

            commonSystem.addSubtypeConstraint(expectedType, substitutedParameterType, DelegatedPropertyConstraintPosition(atom))
        }

        addConstraintForThis(candidateDescriptor, commonSystem)
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType> = emptyMap()

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false

    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
}

object InferenceSessionForExistingCandidates : InferenceSession {
    override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean {
        return !ErrorUtils.isError(candidate.resolvedCall.candidateDescriptor)
    }

    override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
    override fun addErrorCallInfo(callInfo: ErrorCallInfo) {}

    override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType> = emptyMap()

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
    override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean {
        return !ErrorUtils.isError(resolvedCallAtom.candidateDescriptor)
    }
}
