/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.model.TypeVariableMarker

/**
 * See [TypeCastSupport].
 *
 * Not an `object` to prevent IJ from suggesting imports of its members,
 * as it's better to access them via the actual session component.
 */
class DefaultTypeCastSupport : TypeCastSupport {
    override fun isCastToTargetTypeErased(
        targetType: ConeKotlinType,
        type: ConeKotlinType,
        session: FirSession,
    ): Boolean {
        val classSymbol = targetType.toRegularClassSymbol(session) ?: return false
        val staticallyKnownSubtype = findStaticallyKnownSubtype(
            type, classSymbol,
            isSubTypeMarkedNullable = false,
            attributes = ConeAttributes.Empty,
            session = session,
        )

        if (staticallyKnownSubtype == null) {
            return targetType.typeArguments.any { it !is ConeStarProjection }
        }

        return !staticallyKnownSubtype.isSubtypeOf(targetType, session)
    }

    override fun findStaticallyKnownSubtype(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        isSubTypeMarkedNullable: Boolean,
        attributes: ConeAttributes,
        session: FirSession,
    ): ConeKotlinType? {
        return subTypeClassSymbol.constructType(
            typeArguments = approximateStaticallyKnownSubtypeArguments(supertype, subTypeClassSymbol, session) ?: return null,
            isMarkedNullable = isSubTypeMarkedNullable,
            attributes = attributes,
        )
    }

    private fun approximateStaticallyKnownSubtypeArguments(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        session: FirSession,
    ): Array<ConeTypeProjection>? {
        val (constraintSystem, parametersToFreshVariables) = prepareConstraintsSystemForSubtypeArgumentsInference(
            supertype, subTypeClassSymbol, session,
        )

        if (constraintSystem.hasContradiction) {
            return null
        }

        val substitutorMap = mutableMapOf<TypeVariableMarker, ConeTypeProjection>()
        val trivialConstraintTypeInferenceOracle = TrivialConstraintTypeInferenceOracle.create(session.typeContext)
        val resultTypeResolver = ResultTypeResolver(
            session.typeApproximator, trivialConstraintTypeInferenceOracle, session.languageVersionSettings,
        )

        for ((_, constraintsWithVariable) in constraintSystem.currentStorage().notFixedTypeVariables) {
            val variable = constraintsWithVariable.typeVariable
            // The trivial constraints are always there, and we want to check the presence of those that may not be.
            // Non-proper constraints may contain type variables, and they will cause problems during the final
            // substitution
            val constraints = constraintsWithVariable.constraints.filter {
                !constraintSystem.isTrivial(it) && constraintSystem.isProperType(it.type)
            }

            constraints.firstOrNull { it.kind == ConstraintKind.EQUALITY }?.let {
                substitutorMap[variable] = it.type as ConeKotlinType
                continue
            }

            val combinedUpperBound = with(resultTypeResolver) { constraintSystem.findSuperType(constraints) }
            val combinedLowerBound = with(resultTypeResolver) { constraintSystem.findSubType(constraints) }
            val (upperConstraints, lowerConstraints) = constraints.partition { it.kind == ConstraintKind.UPPER }

            substitutorMap[variable] = when {
                // If the variable is bounded on both sides, there's no accurate representation as
                // a single ConeKotlinType without additional context in the current version of the
                // compiler. Picking the upper bound is a bit better than not picking anything at all.
                combinedUpperBound != null -> {
                    val satisfiesOtherBounds = upperConstraints.all { combinedUpperBound.isSubtypeOf(session.typeContext, it.type) }

                    when {
                        satisfiesOtherBounds -> ConeKotlinTypeProjectionOut(combinedUpperBound as ConeKotlinType)
                        else -> ConeStarProjection
                    }
                }
                combinedLowerBound != null -> {
                    val satisfiesOtherBounds = lowerConstraints.all { it.type.isSubtypeOf(session.typeContext, combinedLowerBound) }

                    when {
                        satisfiesOtherBounds -> ConeKotlinTypeProjectionIn(combinedLowerBound as ConeKotlinType)
                        else -> ConeStarProjection
                    }
                }
                else -> ConeStarProjection
            }
        }

        return parametersToFreshVariables.values
            .map { substitutorMap[it] ?: error("Variable $it has not been fixed to anything") }
            .toTypedArray()
    }

    private fun prepareConstraintsSystemForSubtypeArgumentsInference(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        session: FirSession,
    ): SubtypeArgumentsInferenceData {
        val constraintSystem = session.inferenceComponents.createConstraintSystem()

        val parametersToFreshVariables = subTypeClassSymbol.typeParameterSymbols.associateWith {
            ConeTypeVariable(it.name.asString(), it.toLookupTag()).also(constraintSystem::registerVariable)
        }
        val parametersToVariableTypes = parametersToFreshVariables.mapValues { it.value.defaultType }
        val subType = subTypeClassSymbol.constructType(parametersToVariableTypes.values.toTypedArray())
        val parametersToVariablesSubstitutor = substitutorByMap(parametersToVariableTypes, session)

        for ((parameter, variable) in parametersToFreshVariables) {
            for (bound in parameter.resolvedBounds) {
                val boundWithVariables = parametersToVariablesSubstitutor.substituteOrSelf(bound.coneType)
                constraintSystem.addSubtypeConstraint(variable.defaultType, boundWithVariables, SimpleConstraintSystemConstraintPosition)
            }
        }

        val supertypeComponents = when {
            supertype is ConeIntersectionType -> supertype.intersectedTypes
            else -> listOf(supertype)
        }

        for (superTypeComponent in supertypeComponents) {
            if (superTypeComponent !is ConeClassLikeType) {
                continue
            }

            if (subTypeClassSymbol.isSubclassOf(superTypeComponent.lookupTag, session, isStrict = false, lookupInterfaces = true)) {
                constraintSystem.addSubtypeConstraint(subType, superTypeComponent, SimpleConstraintSystemConstraintPosition)
            }
        }

        return SubtypeArgumentsInferenceData(constraintSystem, parametersToFreshVariables, subType)
    }

    private data class SubtypeArgumentsInferenceData(
        val constraintSystem: NewConstraintSystemImpl,
        val parametersToFreshVariables: Map<FirTypeParameterSymbol, ConeTypeVariable>,
        val subTypeWithVariables: ConeKotlinType,
    )
}

private fun NewConstraintSystemImpl.isTrivial(constraint: Constraint) =
    isTopConstraint(constraint) || isBottomConstraint(constraint)

private fun NewConstraintSystemImpl.isTopConstraint(constraint: Constraint) =
    constraint.kind == ConstraintKind.UPPER && constraint.type.isNullableAny()

private fun NewConstraintSystemImpl.isBottomConstraint(constraint: Constraint) =
    constraint.kind == ConstraintKind.LOWER && constraint.type.isNothing()
