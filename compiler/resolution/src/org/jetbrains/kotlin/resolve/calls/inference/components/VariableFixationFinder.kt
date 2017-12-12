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
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtom
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.contains

class VariableFixationFinder {
    interface Context {
        val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>
    }

    data class VariableForFixation(val variable: TypeConstructor, val hasProperConstraint: Boolean)

    fun findFirstVariableForFixation(
            c: Context,
            allTypeVariables: List<TypeConstructor>,
            postponedKtPrimitives: List<PostponedResolvedAtom>,
            completionMode: ConstraintSystemCompletionMode,
            topLevelType: UnwrappedType
    ): VariableForFixation? = c.findTypeVariableForFixation(allTypeVariables, postponedKtPrimitives, completionMode, topLevelType)

    private enum class TypeVariableFixationReadiness {
        FORBIDDEN,
        WITHOUT_PROPER_ARGUMENT_CONSTRAINT, // proper constraint from arguments -- not from upper bound for type parameters
        WITH_COMPLEX_DEPENDENCY, // if type variable T has constraint with non fixed type variable inside (non-top-level): T <: Foo<S>
        RELATED_TO_ANY_OUTPUT_TYPE,
        READY_FOR_FIXATION,
    }

    private fun Context.getTypeVariableReadiness(
            variable: TypeConstructor,
            dependencyProvider: TypeVariableDependencyInformationProvider
    ): TypeVariableFixationReadiness = when {
        !notFixedTypeVariables.contains(variable) ||
        dependencyProvider.isVariableRelatedToTopLevelType(variable) -> TypeVariableFixationReadiness.FORBIDDEN
        !variableHasProperArgumentConstraints(variable) -> TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT
        hasDependencyToOtherTypeVariables(variable) -> TypeVariableFixationReadiness.WITH_COMPLEX_DEPENDENCY
        dependencyProvider.isVariableRelatedToAnyOutputType(variable) -> TypeVariableFixationReadiness.RELATED_TO_ANY_OUTPUT_TYPE
        else -> TypeVariableFixationReadiness.READY_FOR_FIXATION
    }

    private fun Context.findTypeVariableForFixation(
            allTypeVariables: List<TypeConstructor>,
            postponedKtPrimitives: List<PostponedResolvedAtom>,
            completionMode: ConstraintSystemCompletionMode,
            topLevelType: UnwrappedType
    ): VariableForFixation? {
        val dependencyProvider = TypeVariableDependencyInformationProvider(notFixedTypeVariables, postponedKtPrimitives,
                                                                           topLevelType.takeIf { completionMode == PARTIAL })

        val candidate = allTypeVariables.maxBy { getTypeVariableReadiness(it, dependencyProvider) } ?: return null
        val candidateReadiness = getTypeVariableReadiness(candidate, dependencyProvider)
        return when (candidateReadiness) {
            TypeVariableFixationReadiness.FORBIDDEN -> null
            TypeVariableFixationReadiness.WITHOUT_PROPER_ARGUMENT_CONSTRAINT -> VariableForFixation(candidate, false)
            else -> VariableForFixation(candidate, true)
        }
    }

    private fun Context.hasDependencyToOtherTypeVariables(typeVariable: TypeConstructor): Boolean {
        for (constraint in notFixedTypeVariables[typeVariable]?.constraints ?: return false) {
            if (constraint.type.arguments.isNotEmpty() && constraint.type.contains { notFixedTypeVariables.containsKey(it.constructor) }) {
                return true
            }
        }
        return false
    }

    private fun Context.variableHasProperArgumentConstraints(variable: TypeConstructor): Boolean =
            notFixedTypeVariables[variable]?.constraints?.any { isProperArgumentConstraint(it) } ?: false

    private fun Context.isProperArgumentConstraint(c: Constraint) =
            isProperType(c.type) && c.position.initialConstraint.position !is DeclaredUpperBoundConstraintPosition

    private fun Context.isProperType(type: UnwrappedType): Boolean =
            !type.contains { notFixedTypeVariables.containsKey(it.constructor) }

}