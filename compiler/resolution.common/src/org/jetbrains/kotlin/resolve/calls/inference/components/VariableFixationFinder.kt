/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode.PARTIAL
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.*

class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val fixedTypeVariables: Map<TypeConstructorMarker, KotlinTypeMarker>
        val postponedTypeVariables: List<TypeVariableMarker>
        fun isReified(variable: TypeVariableMarker): Boolean
    }

    data class VariableForFixation(
        val variable: TypeConstructorMarker,
        val hasProperConstraint: Boolean,
        val hasOnlyTrivialProperConstraint: Boolean = false
    )

    fun findFirstVariableForFixation(
        c: Context,
        allTypeVariables: List<TypeConstructorMarker>,
        postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
        inferenceCompatibilityMode: Boolean = false,
    ): VariableForFixation? =
        c.findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType, inferenceCompatibilityMode)

    enum class TypeVariableFixationReadiness {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS, // proper trivial constraint from arguments, Nothing <: T
        RELATED_TO_ANY_OUTPUT_TYPE,
        FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND,
        READY_FOR_FIXATION_UPPER,
        READY_FOR_FIXATION_LOWER,
        READY_FOR_FIXATION,
        READY_FOR_FIXATION_REIFIED,
    }

    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        inferenceCompatibilityMode: Boolean
    ): TypeVariableFixationReadiness = when {
        !notFixedTypeVariables.contains(variable) ||
                dependencyProvider.isVariableRelatedToTopLevelType(variable) -> TypeVariableFixationReadiness.FORBIDDEN
        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
        variableHasTrivialOrNonProperConstraints(variable) -> TypeVariableFixationReadiness.WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS
        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
        variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable) ->
            TypeVariableFixationReadiness.FROM_INCORPORATION_OF_DECLARED_UPPER_BOUND
        isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
        inferenceCompatibilityMode -> {
            when {
                variableHasLowerProperConstraint(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_LOWER
                else -> TypeVariableFixationReadiness.READY_FOR_FIXATION_UPPER
            }
        }
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }

    fun isTypeVariableHasProperConstraint(
        context: Context,
        typeVariable: TypeConstructorMarker,
        inferenceCompatibilityMode: Boolean = false
    ): Boolean {
        return with(context) {
            val dependencyProvider = TypeVariableDependencyInformationProvider(
                notFixedTypeVariables, emptyList(), topLevelType = null, context
            )
            when (getTypeVariableReadiness(typeVariable, dependencyProvider, inferenceCompatibilityMode)) {
                TypeVariableFixationReadiness.FORBIDDEN, TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> false
                else -> true
            }
        }
    }

    private fun Context.variableHasTrivialOrNonProperConstraints(variable: TypeConstructorMarker): Boolean {
        return notFixedTypeVariables[variable]?.constraints?.all { constraint ->
            val isProperConstraint = isProperArgumentConstraint(constraint)
            isProperConstraint && trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !isProperConstraint
        } ?: false
    }

    private fun Context.variableHasOnlyIncorporatedConstraintsFromDeclaredUpperBound(variable: TypeConstructorMarker): Boolean {
        val constraints = notFixedTypeVariables[variable]?.constraints ?: return false

        return constraints.filter { isProperArgumentConstraint(it) }.all { it.position.isFromDeclaredUpperBound }
    }

    private fun Context.findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker,
        inferenceCompatibilityMode: Boolean,
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == PARTIAL }, this
        )

        val candidate =
            allTypeVariables.maxByOrNull { getTypeVariableReadiness(it, dependencyProvider, inferenceCompatibilityMode) } ?: return null

        return when (getTypeVariableReadiness(candidate, dependencyProvider, inferenceCompatibilityMode)) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS ->
                VariableForFixation(candidate, hasProperConstraint = true, hasOnlyTrivialProperConstraint = true)

            else -> VariableForFixation(candidate, true)
        }
    }

    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructorMarker): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            val dependencyPresenceCondition = { type: KotlinTypeMarker ->
                type.typeConstructor() != typeVariable && notFixedTypeVariables.containsKey(type.typeConstructor())
            }
            if (constraint.type.lowerBoundIfFlexible().argumentsCount() != 0 && constraint.type.contains(dependencyPresenceCondition))
                return true
        }
        return false
    }

    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.constraints?.any { isProperArgumentConstraint(it) } ?: false

    private fun Context.isProperArgumentConstraint(c: Constraint) =
        isProperType(c.type)
                && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition<*>
                && !c.isNullabilityConstraint

    private fun Context.isProperType(type: KotlinTypeMarker): Boolean =
        isProperTypeForFixation(type) { t -> !t.contains { notFixedTypeVariables.containsKey(it.typeConstructor()) } }

    private fun Context.isReified(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.typeVariable?.let { isReified(it) } ?: false

    private fun Context.variableHasLowerProperConstraint(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.constraints?.let { constraints ->
            constraints.any {
                it.kind.isLower() && isProperArgumentConstraint(it)
            }
        } ?: false
}

inline fun TypeSystemInferenceExtensionContext.isProperTypeForFixation(
    type: KotlinTypeMarker,
    isProper: (KotlinTypeMarker) -> Boolean
): Boolean {
    if (!isProper(type)) return false
    if (type.isCapturedType()) {
        val projection = (type as? SimpleTypeMarker)?.asCapturedType()?.typeConstructorProjection() ?: return true
        if (projection.isStarProjection()) return true

        if (!isProper(projection.getType())) return false
    }

    return true
}
