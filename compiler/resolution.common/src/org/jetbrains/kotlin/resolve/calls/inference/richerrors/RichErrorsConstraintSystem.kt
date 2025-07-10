/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.richerrors

import org.jetbrains.kotlin.resolve.calls.inference.model.ErrorConstraint
import org.jetbrains.kotlin.types.model.*

fun List<ErrorConstraint>.currentSolution(typeSystemContext: TypeSystemContext): Map<TypeConstructorMarker, ErrorTypeMarker> {
    return solveSystemOfConstraints(typeSystemContext).mappings
}

fun List<ErrorConstraint>.hasContradiction(typeSystemContext: TypeSystemContext): Boolean {
    return solveSystemOfConstraints(typeSystemContext).hasContradiction
}

private fun List<ErrorConstraint>.solveSystemOfConstraints(typeSystemContext: TypeSystemContext): RichErrorsSystemSolution<ErrorTypeMarker> {
    val state = RichErrorsSystemState(map { RichErrorsSystemState.Constraint(it.lower, it.upper) })
    return with(typeSystemContext) { state.solveSystem() }
}
