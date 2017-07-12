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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.components.ConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints

typealias VariableResolutionNode = FixationOrderCalculator.NodeWithDirection

class InferenceStepResolver(
        private val resultTypeResolver: ResultTypeResolver
) {
    /**
     * Resolves one or more of the `variables`.
     * Returns `true` if type variable resolution should stop.
     */
    fun resolveVariables(c: ConstraintSystemCompleter.Context, variables: List<VariableResolutionNode>): Boolean {
        if (variables.isEmpty()) return true
        if (c.hasContradiction) return true

        val nodeToResolve = variables.firstOrNull { it.variableWithConstraints.hasProperConstraint(c) } ?:
                            variables.first()
        val (variableWithConstraints, direction) = nodeToResolve
        val variable = variableWithConstraints.typeVariable

        val resultType = resultTypeResolver.findResultType(c.asResultTypeResolverContext(), variableWithConstraints, direction)
        if (resultType == null) {
            c.addError(NotEnoughInformationForTypeParameter(variable))
            return true
        }
        c.fixVariable(variable, resultType)
        return false
    }

    private fun VariableWithConstraints.hasProperConstraint(c: ConstraintSystemCompleter.Context) =
            constraints.any { !it.isTrivial() && c.canBeProper(it.type) }

    private fun Constraint.isTrivial() =
            kind == ConstraintKind.LOWER && KotlinBuiltIns.isNothing(type) ||
            kind == ConstraintKind.UPPER && KotlinBuiltIns.isNullableAny(type)
}