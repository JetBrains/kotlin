/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.inference.TypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.FirDeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition

internal object CreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val declaration = candidate.symbol.fir
        if (declaration !is FirTypeParameterRefsOwner || declaration.typeParameters.isEmpty()) {
            candidate.substitutor = ConeSubstitutor.Empty
            candidate.freshVariables = emptyList()
            return
        }
        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(declaration, candidate, csBuilder, callInfo.session)
        candidate.substitutor = substitutor
        candidate.freshVariables = freshVariables

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) {
            sink.yieldApplicability(CandidateApplicability.INAPPLICABLE) //TODO: auto report it
            return
        }

        // optimization
        if (candidate.typeArgumentMapping == TypeArgumentMapping.NoExplicitArguments /*&& knownTypeParametersResultingSubstitutor == null*/) {
            return
        }

        val typeParameters = declaration.typeParameters
        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshVariables[index]

//            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
//            if (knownTypeArgument != null) {
//                csBuilder.addEqualityConstraint(
//                    freshVariable.defaultType,
//                    knownTypeArgument.unwrap(),
//                    KnownTypeParameterConstraintPosition(knownTypeArgument)
//                )
//                continue
//            }

            when (val typeArgument = candidate.typeArgumentMapping[index]) {
                is FirTypeProjectionWithVariance -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    getTypePreservingFlexibilityWrtTypeVariable(
                        typeArgument.typeRef.coneType,
                        typeParameter,
                        candidate.bodyResolveComponents.inferenceComponents.ctx
                    ),
                    SimpleConstraintSystemConstraintPosition // TODO
                )
                is FirStarProjection -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeParameter.symbol.fir.bounds.firstOrNull()?.coneType
                        ?: sink.components.session.builtinTypes.nullableAnyType.type,
                    SimpleConstraintSystemConstraintPosition
                )
                else -> assert(typeArgument == FirTypePlaceholderProjection) {
                    "Unexpected typeArgument: ${typeArgument.renderWithType()}"
                }
            }
        }
    }

    private fun getTypePreservingFlexibilityWrtTypeVariable(
        type: ConeKotlinType,
        typeParameter: FirTypeParameterRef,
        context: ConeTypeContext
    ): ConeKotlinType {
        return if (typeParameter.shouldBeFlexible(context)) {
            val notNullType = type.withNullability(ConeNullability.NOT_NULL)
            ConeFlexibleType(notNullType, notNullType.withNullability(ConeNullability.NULLABLE))
        } else {
            type
        }
    }

    private fun FirTypeParameterRef.shouldBeFlexible(context: ConeTypeContext): Boolean {
        return symbol.fir.bounds.any {
            val type = it.coneType
            type is ConeFlexibleType || with(context) {
                (type.typeConstructor() as? FirTypeParameterSymbol)?.fir?.shouldBeFlexible(context) ?: false
            }
        }
    }

}

private fun createToFreshVariableSubstitutorAndAddInitialConstraints(
    declaration: FirTypeParameterRefsOwner,
    candidate: Candidate,
    csBuilder: ConstraintSystemOperation,
    session: FirSession
): Pair<ConeSubstitutor, List<ConeTypeVariable>> {

    val typeParameters = declaration.typeParameters

    val freshTypeVariables = typeParameters.map { TypeParameterBasedTypeVariable(it.symbol) }

    val toFreshVariables = substitutorByMap(freshTypeVariables.associate { it.typeParameterSymbol to it.defaultType })

    for (freshVariable in freshTypeVariables) {
        csBuilder.registerVariable(freshVariable)
    }

    fun TypeParameterBasedTypeVariable.addSubtypeConstraint(
        upperBound: ConeKotlinType//,
        //position: DeclaredUpperBoundConstraintPosition
    ) {
        if ((upperBound.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag?.classId == StandardClassIds.Any &&
            upperBound.upperBoundIfFlexible().isMarkedNullable
        ) {
            return
        }

        csBuilder.addSubtypeConstraint(
            defaultType,
            toFreshVariables.substituteOrSelf(upperBound),
            FirDeclaredUpperBoundConstraintPosition()
        )
    }

    for (index in typeParameters.indices) {
        val typeParameter = typeParameters[index]
        val freshVariable = freshTypeVariables[index]
        //val position = DeclaredUpperBoundConstraintPosition(typeParameter)

        for (upperBound in typeParameter.symbol.fir.bounds) {
            freshVariable.addSubtypeConstraint(upperBound.coneType/*, position*/)
        }
    }

//    if (candidateDescriptor is TypeAliasConstructorDescriptor) {
//        val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
//        val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
//        val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
//        for (index in typeParameters.indices) {
//            val typeParameter = typeParameters[index]
//            val freshVariable = freshTypeVariables[index]
//            val typeMapping = originalTypes.mapIndexedNotNull { i: Int, kotlinType: KotlinType ->
//                if (kotlinType == typeParameter.defaultType) i else null
//            }
//            for (originalIndex in typeMapping) {
//                // there can be null in case we already captured type parameter in outer class (in case of inner classes)
//                // see test innerClassTypeAliasConstructor.kt
//                val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
//                val position = DeclaredUpperBoundConstraintPosition(originalTypeParameter)
//                for (upperBound in originalTypeParameter.upperBounds) {
//                    freshVariable.addSubtypeConstraint(upperBound, position)
//                }
//            }
//        }
//    }
    return toFreshVariables to freshTypeVariables
}
