/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.AbstractTypeChecker.effectiveVariance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.TypeVariance

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
        val (constraintSystem, parametersToFreshVariables) = prepareConstraintsSystemForSubtypeArgumentsInference(
            type, classSymbol, session,
        )

        // We haven't yet added any `targetType`-specific information into the CS, so a contradiction
        // manifests arguments-irrelevant typing issues, such as casts impossible simply due to
        // declaration-site data
        if (constraintSystem.hasContradiction) {
            return targetType.typeArguments.any { it !is ConeStarProjection }
        }

        for ((_, constraintsWithVariable) in constraintSystem.currentStorage().notFixedTypeVariables) {
            val variable = constraintsWithVariable.typeVariable
            val variableIndex = parametersToFreshVariables.values.indexOf(variable)
            val argument = targetType.typeArguments[variableIndex]
            // The trivial constraints are always there, and we want to check the presence of those that may not be
            val constraints = constraintsWithVariable.constraints.filterNot(constraintSystem::isTrivial)
            val argumentType = argument.type ?: continue // Absent for star projections, no need to check them

            val variance = with(constraintSystem) {
                effectiveVariance(
                    declared = parametersToFreshVariables.keys.toList()[variableIndex].toLookupTag().getVariance(),
                    useSite = argument.getVariance(),
                ) ?: return false // Variance errors must be reported separately
            }

            val equalityType = constraints.firstOrNull { it.kind == ConstraintKind.EQUALITY }?.type

            when (variance) {
                TypeVariance.INV -> {
                    val it = equalityType ?: return true
                    constraintSystem.addSubtypeConstraint(it, argumentType, SimpleConstraintSystemConstraintPosition)
                    constraintSystem.addSubtypeConstraint(argumentType, it, SimpleConstraintSystemConstraintPosition)
                }
                TypeVariance.OUT -> {
                    val itType = equalityType ?: constraints.combinedUpperBound(session)
                    constraintSystem.addSubtypeConstraint(itType, argumentType, SimpleConstraintSystemConstraintPosition)
                }
                TypeVariance.IN -> {
                    val itType = equalityType ?: constraints.combinedLowerBound(session)
                    constraintSystem.addSubtypeConstraint(argumentType, itType, SimpleConstraintSystemConstraintPosition)
                }
            }

            if (constraintSystem.hasContradiction) {
                return true
            }
        }

        return constraintSystem.hasContradiction
    }

    private fun List<Constraint>.combinedUpperBound(session: FirSession): KotlinTypeMarker {
        val filtered = mapNotNull { if (it.kind == ConstraintKind.UPPER) it.type else null }
        return when {
            filtered.isNotEmpty() -> session.typeContext.intersectTypes(filtered)
            else -> session.builtinTypes.nullableAnyType.coneType
        }
    }

    private fun List<Constraint>.combinedLowerBound(session: FirSession): KotlinTypeMarker {
        val filtered = mapNotNull { if (it.kind == ConstraintKind.LOWER) it.type else null }
        return when {
            filtered.isNotEmpty() -> session.typeContext.commonSuperType(filtered)
            else -> session.builtinTypes.nothingType.coneType
        }
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

        for ((_, constraintsWithVariable) in constraintSystem.currentStorage().notFixedTypeVariables) {
            val variable = constraintsWithVariable.typeVariable
            // The trivial constraints are always there, and we want to check the presence of those that may not be
            val constraints = constraintsWithVariable.constraints.filterNot(constraintSystem::isTrivial)

            constraints.firstOrNull { it.kind == ConstraintKind.EQUALITY }?.let {
                substitutorMap[variable] = it.type as ConeKotlinType
                continue
            }

            val (upperConstraints, lowerConstraints) = constraints.partition { it.kind == ConstraintKind.UPPER }

            when {
                upperConstraints.isNotEmpty() && lowerConstraints.isEmpty() -> substitutorMap[variable] = session.typeContext
                    .intersectTypes(upperConstraints.map { it.type })
                    .let { ConeKotlinTypeProjectionOut(it) }
                upperConstraints.isEmpty() && lowerConstraints.isNotEmpty() -> substitutorMap[variable] = session.typeContext
                    .commonSuperType(lowerConstraints.map { it.type })
                    .let { ConeKotlinTypeProjectionIn(it as ConeKotlinType) }
                // If both are empty, then this is correct, if both are not, then there's no
                // obvious way how we should fix the variable, so we choose to be conservative.
                else -> substitutorMap[variable] = ConeStarProjection
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
