/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.invoke
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.FirDeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition


internal object CreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val declaration = candidate.symbol.fir
        if (declaration !is FirTypeParametersOwner || declaration.typeParameters.isEmpty()) {
            candidate.substitutor = ConeSubstitutor.Empty
            candidate.freshVariables = emptyList()
            return
        }
        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) = createToFreshVariableSubstitutorAndAddInitialConstraints(declaration, candidate, csBuilder)
        candidate.substitutor = substitutor
        candidate.freshVariables = freshVariables

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) {
            sink.yieldApplicability(CandidateApplicability.INAPPLICABLE) //TODO: auto report it
            return
        }

        // optimization
//        if (resolvedCall.typeArgumentMappingByOriginal == NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
//            return
//        }

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


            val typeArgument =
                callInfo.typeArguments.getOrElse(index) { FirTypePlaceholderProjection }//resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)
//
            when (typeArgument) {
                is FirTypeProjectionWithVariance -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeArgument.typeRef.coneTypeUnsafe(),
                    SimpleConstraintSystemConstraintPosition // TODO
                )
                is FirStarProjection -> csBuilder.addEqualityConstraint(
                    freshVariable.defaultType,
                    typeParameter.bounds.firstOrNull()?.coneTypeUnsafe()
                        ?: sink.components.session.builtinTypes.nullableAnyType.type, //StandardClassIds.Any(sink.components.session.firSymbolProvider).constructType(emptyArray(), true),
                    SimpleConstraintSystemConstraintPosition
                )
                else -> assert(typeArgument == FirTypePlaceholderProjection) {
                    "Unexpected typeArgument: ${typeArgument.renderWithType()}"
                }
            }
        }
    }

}

fun createToFreshVariableSubstitutorAndAddInitialConstraints(
    declaration: FirTypeParametersOwner,
    candidate: Candidate,
    csBuilder: ConstraintSystemOperation
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
        csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.substituteOrSelf(upperBound), FirDeclaredUpperBoundConstraintPosition())
    }

    for (index in typeParameters.indices) {
        val typeParameter = typeParameters[index]
        val freshVariable = freshTypeVariables[index]
        //val position = DeclaredUpperBoundConstraintPosition(typeParameter)

        for (upperBound in typeParameter.bounds) {
            freshVariable.addSubtypeConstraint(upperBound.coneTypeUnsafe()/*, position*/)
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
