/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class ExplicitTypeParameterConstraintPositionImpl(
    typeArgument: SimpleTypeArgument
) : ExplicitTypeParameterConstraintPosition<SimpleTypeArgument>(typeArgument)

class InjectedAnotherStubTypeConstraintPositionImpl(builderInferenceLambdaOfInjectedStubType: LambdaKotlinCallArgument) :
    InjectedAnotherStubTypeConstraintPosition<LambdaKotlinCallArgument>(builderInferenceLambdaOfInjectedStubType)

class BuilderInferenceSubstitutionConstraintPositionImpl(
    builderInferenceLambda: LambdaKotlinCallArgument,
    initialConstraint: InitialConstraint,
    isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : BuilderInferenceSubstitutionConstraintPosition<LambdaKotlinCallArgument>(
    builderInferenceLambda, initialConstraint, isFromNotSubstitutedDeclaredUpperBound
)

class ExpectedTypeConstraintPositionImpl(topLevelCall: KotlinCall) : ExpectedTypeConstraintPosition<KotlinCall>(topLevelCall)

class DeclaredUpperBoundConstraintPositionImpl(
    typeParameter: TypeParameterDescriptor
) : DeclaredUpperBoundConstraintPosition<TypeParameterDescriptor>(typeParameter) {
    override fun toString() = "DeclaredUpperBound ${typeParameter.name} from ${typeParameter.containingDeclaration}"
}

class ArgumentConstraintPositionImpl(argument: KotlinCallArgument) : ArgumentConstraintPosition<KotlinCallArgument>(argument)

class CallableReferenceConstraintPositionImpl(val callableReferenceCall: CallableReferenceKotlinCall) :
    CallableReferenceConstraintPosition<CallableReferenceResolutionAtom>(callableReferenceCall)

class ReceiverConstraintPositionImpl(argument: KotlinCallArgument) : ReceiverConstraintPosition<KotlinCallArgument>(argument)

class FixVariableConstraintPositionImpl(
    variable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom?
) : FixVariableConstraintPosition<ResolvedAtom?>(variable, resolvedAtom)

class KnownTypeParameterConstraintPositionImpl(typeArgument: KotlinType) : KnownTypeParameterConstraintPosition<KotlinType>(typeArgument)

class LambdaArgumentConstraintPositionImpl(lambda: ResolvedLambdaAtom) : LambdaArgumentConstraintPosition<ResolvedLambdaAtom>(lambda)

class DelegatedPropertyConstraintPositionImpl(topLevelCall: KotlinCall) : DelegatedPropertyConstraintPosition<KotlinCall>(topLevelCall)

class NotEnoughInformationForTypeParameterImpl(
    typeVariable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : NotEnoughInformationForTypeParameter<ResolvedAtom>(typeVariable, resolvedAtom, couldBeResolvedWithUnrestrictedBuilderInference)
