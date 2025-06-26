/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.richerrors

import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.types.model.ErrorTypeMarker
import org.jetbrains.kotlin.types.model.RichErrorsSystemSolution
import org.jetbrains.kotlin.types.model.RichErrorsSystemState
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.model.TypeVariableTypeConstructorMarker

class RichErrorsConstraintSystem(
    val typeSystemContext: TypeSystemContext,
) {
    private val constraints = mutableListOf<Constraint>()

    val errors: List<ConstraintSystemError>
        get() = emptyList()

    val hasContradiction: Boolean
        get() = solveSystemOfConstraints().hasContradiction

    val currentSolution: Map<TypeConstructorMarker, ErrorTypeMarker>
        get() = solveSystemOfConstraints().mappings

    fun addSubtypeConstraint(lowerType: ErrorTypeMarker, upperType: ErrorTypeMarker, position: ConstraintPosition) {
        with(typeSystemContext) {
            val simplifiedConstraints = simplifyAndIncorporateSubtyping(lowerType, upperType)
            simplifiedConstraints.forEach { (lt, ut) ->
                constraints.add(Constraint(lt, ut, position))
            }
        }
    }

    fun addEqualityConstraint(a: ErrorTypeMarker, b: ErrorTypeMarker, position: ConstraintPosition) {
        addSubtypeConstraint(a, b, position)
        addSubtypeConstraint(b, a, position)
    }

    private fun solveSystemOfConstraints(): RichErrorsSystemSolution<ErrorTypeMarker> {
        val state = RichErrorsSystemState(constraints.map { RichErrorsSystemState.Constraint(it.lower, it.upper) })
        return with(typeSystemContext) { state.solveSystem() }
    }

    private data class Constraint(
        val lower: ErrorTypeMarker,
        val upper: ErrorTypeMarker,
        val position: ConstraintPosition,
    )
}
