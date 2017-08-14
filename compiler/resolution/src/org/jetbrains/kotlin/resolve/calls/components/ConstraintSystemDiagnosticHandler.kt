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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.components.KotlinCallCompleter.Context
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.ResolutionCandidateStatus
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

fun handleDiagnostics(
        c: KotlinConstraintSystemCompleter.Context,
        status: ResolutionCandidateStatus,
        isOuterCall: Boolean
): List<AggregatedConstraintError> {
    val positionErrors = groupErrorsByPosition(c, status, isOuterCall)
    val constraintSystemDiagnostics = mutableListOf<AggregatedConstraintError>()
    for ((position, incorporationPositionsWithTypeVariables) in positionErrors) {
        val variablesWithConstraints = incorporationPositionsWithTypeVariables.mapNotNull { (_, typeVariable) ->
            if (typeVariable == null) return@mapNotNull null
            if (c.canBeProper(typeVariable.defaultType)) return@mapNotNull null

            c.variableConstraints(typeVariable)
        }.distinctBy { it.typeVariable }

        if (variablesWithConstraints.isEmpty()) continue

        // Each position can refer to the same type variables, we'll fix it later
        // Also, probably it's enough to show error only about one type parameter
        variablesWithConstraints.mapTo(constraintSystemDiagnostics) {
            val properConstraints = it.constraints.filter { constraint -> c.canBeProper(constraint.type) }
            val sortedConstraints = divideByConstraints(properConstraints)
            AggregatedConstraintError(position, it.typeVariable, extractKind(it.typeVariable), simplify(sortedConstraints))
        }
    }

    return constraintSystemDiagnostics
}

data class SortedConstraints(val upper: List<Constraint>, val equality: List<Constraint>, val lower: List<Constraint>)

fun simplify(constraints: SortedConstraints): SortedConstraints {
    if (constraints.equality.isNotEmpty()) return constraints

    val onlyIncorrectConstraints = constraints.lower.filter { !it.isCorrect(constraints.upper) }
    return SortedConstraints(constraints.upper, constraints.equality, onlyIncorrectConstraints)
}

private fun Constraint.isCorrect(upperConstraints: List<Constraint>): Boolean {
    return upperConstraints.all { KotlinTypeChecker.DEFAULT.isSubtypeOf(this.type, it.type) }
}

private fun extractKind(typeVariable: NewTypeVariable): SpecialTypeVariableKind? {
    val freshTypeConstructor = typeVariable.freshTypeConstructor
    if (freshTypeConstructor !is TypeVariableTypeConstructor) return null

    val name = freshTypeConstructor.debugName
    return SPECIAL_TYPE_PARAMETER_NAME_TO_KIND[name]
}

// TODO: Get names of type parameters from ControlStructureTypingUtils.ResolveConstruct
private val SPECIAL_TYPE_PARAMETER_NAME_TO_KIND = mapOf(
        "<TYPE-PARAMETER-FOR-IF-RESOLVE>" to SpecialTypeVariableKind.IF,
        "<TYPE-PARAMETER-FOR-WHEN-RESOLVE>" to SpecialTypeVariableKind.WHEN,
        "<TYPE-PARAMETER-FOR-ELVIS-RESOLVE>" to SpecialTypeVariableKind.ELVIS
)

private fun divideByConstraints(constraints: List<Constraint>): SortedConstraints {
    return with(constraints) {
        SortedConstraints(getWith(ConstraintKind.UPPER), getWith(ConstraintKind.EQUALITY), getWith(ConstraintKind.LOWER))
    }
}

private fun List<Constraint>.getWith(kind: ConstraintKind) = filter { it.kind == kind }

private fun groupErrorsByPosition(c: KotlinConstraintSystemCompleter.Context, status: ResolutionCandidateStatus, isOuterCall: Boolean): Map<ConstraintPosition, List<PositionWithTypeVariable>> {
    val errorsFromConstraintSystem = if (c is NewConstraintSystem && isOuterCall) c.diagnostics else emptyList()
    return (status.diagnostics + errorsFromConstraintSystem)
            .filterIsInstance<NewConstraintError>()
            .filter { it.candidateApplicability != ResolutionCandidateApplicability.INAPPLICABLE_WRONG_RECEIVER }
            .distinctErrors()
            .groupBy({ it.position.from }) { PositionWithTypeVariable(it.position, it.typeVariable) }
}

private fun List<NewConstraintError>.distinctErrors(): List<NewConstraintError> {
    val distinctDiagnostics = mutableListOf<NewConstraintError>()
    for (diagnostic in this) {
        val added = distinctDiagnostics.any { otherDiagnostic ->
            diagnostic.lowerType == otherDiagnostic.lowerType &&
            diagnostic.upperType == otherDiagnostic.upperType &&
            diagnostic.position == otherDiagnostic.position &&
            diagnostic.candidateApplicability == otherDiagnostic.candidateApplicability
        }

        if (!added) distinctDiagnostics.add(diagnostic)
    }

    return distinctDiagnostics
}

private data class PositionWithTypeVariable(val position: IncorporationConstraintPosition, val typeVariable: NewTypeVariable?)