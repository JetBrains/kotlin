/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.container.PlatformExtensionsClashResolver
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.StubType
import org.jetbrains.kotlin.types.UnwrappedType

// stateless component
interface KotlinResolutionStatelessCallbacks {
    fun isDescriptorFromSource(descriptor: CallableDescriptor): Boolean
    fun isInfixCall(kotlinCall: KotlinCall): Boolean
    fun isOperatorCall(kotlinCall: KotlinCall): Boolean
    fun isSuperOrDelegatingConstructorCall(kotlinCall: KotlinCall): Boolean
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor, kotlinCall: KotlinCall, resolutionCallbacks: KotlinResolutionCallbacks
    ): Boolean

    fun isSuperExpression(receiver: SimpleKotlinCallArgument?): Boolean
    fun getScopeTowerForCallableReferenceArgument(argument: CallableReferenceKotlinCallArgument): ImplicitScopeTower
    fun getVariableCandidateIfInvoke(functionCall: KotlinCall): KotlinResolutionCandidate?
    fun isCoroutineCall(argument: KotlinCallArgument, parameter: ValueParameterDescriptor): Boolean
    fun isApplicableCallForBuilderInference(descriptor: CallableDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean
}

// This components hold state (trace). Work with this carefully.
interface KotlinResolutionCallbacks {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaKotlinCallArgument,
        isSuspend: Boolean,
        receiverType: UnwrappedType?,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?, // null means, that return type is not proper i.e. it depends on some type variables
        stubsForPostponedVariables: Map<NewTypeVariable, StubType>
    ): Pair<List<KotlinCallArgument>, InferenceSession?>

    fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom)

    fun createReceiverWithSmartCastInfo(resolvedAtom: ResolvedCallAtom): ReceiverValueWithSmartCastInfo?

    fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean

    val inferenceSession: InferenceSession

    fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType?
}

interface SamConversionTransformer : PlatformSpecificExtension<SamConversionTransformer> {
    fun getFunctionTypeForPossibleSamType(possibleSamType: UnwrappedType): UnwrappedType?

    fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean

    object Empty : SamConversionTransformer {
        override fun getFunctionTypeForPossibleSamType(possibleSamType: UnwrappedType): UnwrappedType? = null
        override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean = false
    }
}

class SamConversionTransformerClashesResolver : PlatformExtensionsClashResolver.UseAnyOf<SamConversionTransformer>(
    SamConversionTransformer.Empty,
    SamConversionTransformer::class.java
)