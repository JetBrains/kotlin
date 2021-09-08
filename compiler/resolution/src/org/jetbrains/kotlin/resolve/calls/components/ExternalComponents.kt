/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.tower.CandidateFactoryProviderForInvoke
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.StubTypeForBuilderInference
import org.jetbrains.kotlin.types.UnwrappedType

// stateless component
interface KotlinResolutionStatelessCallbacks {
    fun isDescriptorFromSource(descriptor: CallableDescriptor): Boolean
    fun isInfixCall(kotlinCall: KotlinCall): Boolean
    fun isOperatorCall(kotlinCall: KotlinCall): Boolean
    fun isSuperOrDelegatingConstructorCall(kotlinCall: KotlinCall): Boolean
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, kotlinCallArgument: KotlinCallArgument, resolutionCallbacks: KotlinResolutionCallbacks
    ): Boolean

    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, kotlinCall: KotlinCall, resolutionCallbacks: KotlinResolutionCallbacks
    ): Boolean

    fun isSuperExpression(receiver: SimpleKotlinCallArgument?): Boolean
    fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceKotlinCallArgument): ImplicitScopeTower
    fun getVariableCandidateIfInvoke(functionCall: KotlinCall): ResolutionCandidate?
    fun isBuilderInferenceCall(argument: KotlinCallArgument, parameter: ValueParameterDescriptor): Boolean
    fun isApplicableCallForBuilderInference(descriptor: CallableDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean

    fun isOldIntersectionIsEmpty(types: Collection<KotlinType>): Boolean

    fun createConstraintSystemForOverloadResolution(
        constraintInjector: ConstraintInjector, builtIns: KotlinBuiltIns
    ): SimpleConstraintSystem
}

data class ReturnArgumentsInfo(
    val nonErrorArguments: List<KotlinCallArgument>,
    val lastExpression: KotlinCallArgument?,
    val lastExpressionCoercedToUnit: Boolean,
    val returnArgumentsExist: Boolean
) {
    companion object {
        val empty = ReturnArgumentsInfo(emptyList(), null, lastExpressionCoercedToUnit = false, returnArgumentsExist = false)
    }
}

data class ReturnArgumentsAnalysisResult(
    val returnArgumentsInfo: ReturnArgumentsInfo,
    val inferenceSession: InferenceSession?,
    val hasInapplicableCallForBuilderInference: Boolean = false
)

// This components hold state (trace). Work with this carefully.
interface KotlinResolutionCallbacks {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaKotlinCallArgument,
        isSuspend: Boolean,
        receiverType: UnwrappedType?,
        contextReceiversTypes: List<UnwrappedType>,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?, // null means, that return type is not proper i.e. it depends on some type variables
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    ): ReturnArgumentsAnalysisResult

    fun getCandidateFactoryForInvoke(
        scopeTower: ImplicitScopeTower,
        kotlinCall: KotlinCall,
    ): CandidateFactoryProviderForInvoke<ResolutionCandidate>

    fun resolveCallableReferenceArgument(
        argument: CallableReferenceKotlinCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
    ): Collection<CallableReferenceResolutionCandidate>

    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)

    fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean

    val inferenceSession: InferenceSession

    fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType?

    fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom)

    fun getLhsResult(call: KotlinCall): LHSResult

    fun convertSignedConstantToUnsigned(argument: KotlinCallArgument): IntegerValueTypeConstant?

    fun recordInlinabilityOfLambda(atom: Set<Map.Entry<SimpleResolutionCandidate, ResolvedLambdaAtom>>)
}
