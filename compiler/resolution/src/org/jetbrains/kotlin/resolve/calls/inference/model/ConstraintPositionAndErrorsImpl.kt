/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.TypeVariableMarker

@K1Deprecation
class ExplicitTypeParameterConstraintPositionImpl(
    typeArgument: SimpleTypeArgument
) : ExplicitTypeParameterConstraintPosition<SimpleTypeArgument>(typeArgument)

@K1Deprecation
class InjectedAnotherStubTypeConstraintPositionImpl(builderInferenceLambdaOfInjectedStubType: LambdaKotlinCallArgument) :
    InjectedAnotherStubTypeConstraintPosition<LambdaKotlinCallArgument>(builderInferenceLambdaOfInjectedStubType)

@K1Deprecation
class BuilderInferenceSubstitutionConstraintPositionImpl(
    builderInferenceLambda: LambdaKotlinCallArgument,
    initialConstraint: InitialConstraint,
    isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : BuilderInferenceSubstitutionConstraintPosition<LambdaKotlinCallArgument>(
    builderInferenceLambda, initialConstraint, isFromNotSubstitutedDeclaredUpperBound
)

@K1Deprecation
class ExpectedTypeConstraintPositionImpl(topLevelCall: KotlinCall) : ExpectedTypeConstraintPosition<KotlinCall>(topLevelCall)

@K1Deprecation
class DeclaredUpperBoundConstraintPositionImpl(
    typeParameter: TypeParameterDescriptor,
    val kotlinCall: KotlinCall
) : DeclaredUpperBoundConstraintPosition<TypeParameterDescriptor>(typeParameter) {
    override fun toString() = "DeclaredUpperBound ${typeParameter.name} from ${typeParameter.containingDeclaration}"
}

@K1Deprecation
class ArgumentConstraintPositionImpl(argument: KotlinCallArgument) : RegularArgumentConstraintPosition<KotlinCallArgument>(argument)

@K1Deprecation
class CallableReferenceConstraintPositionImpl(val callableReferenceCall: CallableReferenceKotlinCall) :
    CallableReferenceConstraintPosition<CallableReferenceResolutionAtom>(callableReferenceCall)

@K1Deprecation
class ReceiverConstraintPositionImpl(
    argument: KotlinCallArgument,
    val selectorCall: KotlinCall?
) : ReceiverConstraintPosition<KotlinCallArgument>(argument)

@K1Deprecation
class FixVariableConstraintPositionImpl(
    variable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom?
) : FixVariableConstraintPosition<ResolvedAtom?>(variable, resolvedAtom)

@K1Deprecation
class KnownTypeParameterConstraintPositionImpl(typeArgument: KotlinType) : KnownTypeParameterConstraintPosition<KotlinType>(typeArgument)

@K1Deprecation
class LambdaArgumentConstraintPositionImpl(lambda: ResolvedLambdaAtom) : LambdaArgumentConstraintPosition<ResolvedLambdaAtom>(lambda)

@K1Deprecation
class DelegatedPropertyConstraintPositionImpl(topLevelCall: KotlinCall) : DelegatedPropertyConstraintPosition<KotlinCall>(topLevelCall)

@K1Deprecation
class NotEnoughInformationForTypeParameterImpl(
    typeVariable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : NotEnoughInformationForTypeParameter<ResolvedAtom>(typeVariable, resolvedAtom, couldBeResolvedWithUnrestrictedBuilderInference)
