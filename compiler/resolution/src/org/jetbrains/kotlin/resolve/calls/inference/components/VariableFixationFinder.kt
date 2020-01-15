/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.PARTIAL
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class VariableFixationFinder(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
) {
    interface Context : TypeSystemInferenceExtensionContext {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
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
        topLevelType: KotlinTypeMarker
    ): VariableForFixation? = c.findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)

    enum class TypeVariableFixationReadiness {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS, // proper trivial constraint from arguments, Nothing <: T
        RELATED_TO_ANY_OUTPUT_TYPE,
        READY_FOR_FIXATION,
        READY_FOR_FIXATION_REIFIED,
    }

    private fun Context.getTypeVariableReadiness(
        variable: TypeConstructorMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): TypeVariableFixationReadiness = when {
        !notFixedTypeVariables.contains(variable) ||
                dependencyProvider.isVariableRelatedToTopLevelType(variable) -> TypeVariableFixationReadiness.FORBIDDEN
        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
        variableHasTrivialOrNonProperConstraints(variable) -> TypeVariableFixationReadiness.WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS
        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
        isReified(variable) -> TypeVariableFixationReadiness.READY_FOR_FIXATION_REIFIED
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }

    fun isTypeVariableHasProperConstraint(context: Context, typeVariable: TypeConstructorMarker): Boolean {
        return with(context) {
            val dependencyProvider = TypeVariableDependencyInformationProvider(
                notFixedTypeVariables, emptyList(), topLevelType = null, context
            )
            when (getTypeVariableReadiness(typeVariable, dependencyProvider)) {
                TypeVariableFixationReadiness.FORBIDDEN, TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> false
                else -> true
            }
        }
    }

    private fun Context.findTypeVariableForFixation(
        allTypeVariables: List<TypeConstructorMarker>,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: KotlinTypeMarker
    ): VariableForFixation? {
        if (allTypeVariables.isEmpty()) return null

        val dependencyProvider = TypeVariableDependencyInformationProvider(
            notFixedTypeVariables, postponedArguments, topLevelType.takeIf { completionMode == PARTIAL }, this
        )

        val candidate = allTypeVariables.maxBy { getTypeVariableReadiness(it, dependencyProvider) } ?: return null

        val candidateReadiness = getTypeVariableReadiness(candidate, dependencyProvider)
        return when (candidateReadiness) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            TypeVariableFixationReadiness.WITH_TRIVIAL_OR_NON_PROPER_CONSTRAINTS ->
                VariableForFixation(candidate, hasProperConstraint = true, hasOnlyTrivialProperConstraint = true)

            else -> VariableForFixation(candidate, true)
        }
    }

    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructorMarker): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            if (constraint.type.lowerBoundIfFlexible().argumentsCount() != 0 && constraint.type.contains { notFixedTypeVariables.containsKey(it.typeConstructor()) }) {
                return true
            }
        }
        return false
    }

    private fun Context.variableHasTrivialOrNonProperConstraints(variable: TypeConstructorMarker): Boolean {
        return notFixedTypeVariables[variable]?.constraints?.all { constraint ->
            val isProperConstraint = isProperArgumentConstraint(constraint)
            isProperConstraint && trivialConstraintTypeInferenceOracle.isNotInterestingConstraint(constraint) || !isProperConstraint
        } ?: false
    }

    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.constraints?.any { isProperArgumentConstraint(it) } ?: false

    private fun Context.isProperArgumentConstraint(c: Constraint) =
        isProperType(c.type)
                && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition
                && !c.isNullabilityConstraint

    private fun Context.isProperType(type: KotlinTypeMarker): Boolean =
        !type.contains { notFixedTypeVariables.containsKey(it.typeConstructor()) }

    private fun Context.isReified(variable: TypeConstructorMarker): Boolean =
        notFixedTypeVariables[variable]?.typeVariable?.let { isReified(it) } ?: false
}