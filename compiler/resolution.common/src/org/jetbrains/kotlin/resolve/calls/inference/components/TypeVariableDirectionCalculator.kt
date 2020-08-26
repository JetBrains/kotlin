/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.FlexibleTypeMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.utils.SmartList


private typealias Variable = VariableWithConstraints

class TypeVariableDirectionCalculator(
    private val c: VariableFixationFinder.Context,
    private val postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
    topLevelType: KotlinTypeMarker
) {
    enum class ResolveDirection {
        TO_SUBTYPE,
        TO_SUPERTYPE,
        UNKNOWN
    }

    data class NodeWithDirection(val variableWithConstraints: VariableWithConstraints, val direction: ResolveDirection) {
        override fun toString() = "$variableWithConstraints to $direction"
    }

    private val directions = HashMap<Variable, ResolveDirection>()

    init {
        setupDirections(topLevelType)
    }

    fun getDirection(typeVariable: Variable): ResolveDirection =
        directions.getOrDefault(typeVariable, ResolveDirection.UNKNOWN)

    private fun setupDirections(topReturnType: KotlinTypeMarker) {
        topReturnType.visitType(ResolveDirection.TO_SUBTYPE) { variableWithConstraints, direction ->
            enterToNode(variableWithConstraints, direction)
        }
        for (postponedArgument in postponedKtPrimitives) {
            for (inputType in postponedArgument.inputTypes) {
                inputType.visitType(ResolveDirection.TO_SUBTYPE) { variableWithConstraints, direction ->
                    enterToNode(variableWithConstraints, direction)
                }
            }
        }
    }

    private fun enterToNode(variable: Variable, direction: ResolveDirection) {
        if (direction == ResolveDirection.UNKNOWN) return

        val previous = directions[variable]
        if (previous != null) {
            if (previous != direction) {
                directions[variable] = ResolveDirection.UNKNOWN
            }
            return
        }

        directions[variable] = direction

        for ((otherVariable, otherDirection) in getConstraintDependencies(variable, direction)) {
            enterToNode(otherVariable, otherDirection)
        }
    }

    private fun getConstraintDependencies(
        variable: Variable,
        direction: ResolveDirection
    ): List<NodeWithDirection> =
        SmartList<NodeWithDirection>().also { result ->
            for (constraint in variable.constraints) {
                if (!isInterestingConstraint(direction, constraint)) continue

                constraint.type.visitType(direction) { nodeVariable, nodeDirection ->
                    result.add(NodeWithDirection(nodeVariable, nodeDirection))
                }
            }
        }


    private fun isInterestingConstraint(direction: ResolveDirection, constraint: Constraint): Boolean =
        !(direction == ResolveDirection.TO_SUBTYPE && constraint.kind == ConstraintKind.UPPER) &&
                !(direction == ResolveDirection.TO_SUPERTYPE && constraint.kind == ConstraintKind.LOWER)

    private fun KotlinTypeMarker.visitType(
        startDirection: ResolveDirection,
        action: (variable: Variable, direction: ResolveDirection) -> Unit
    ) = when (this) {
        is SimpleTypeMarker -> visitType(startDirection, action)
        is FlexibleTypeMarker -> {
            with(c) {
                lowerBound().visitType(startDirection, action)
                upperBound().visitType(startDirection, action)
            }
        }
        else -> error("?!")
    }

    private fun SimpleTypeMarker.visitType(
        startDirection: ResolveDirection,
        action: (variable: Variable, direction: ResolveDirection) -> Unit
    ): Unit = with(c) {
        val constructor = typeConstructor()
        if (constructor.isIntersection()) {
            constructor.supertypes().forEach {
                it.visitType(startDirection, action)
            }
            return
        }

        if (argumentsCount() == 0) {
            c.notFixedTypeVariables[constructor]?.let {
                action(it, startDirection)
            }
            return
        }

        if (constructor.parametersCount() != argumentsCount()) return // incorrect type

        for (index in 0 until constructor.parametersCount()) {
            val parameter = constructor.getParameter(index)
            val argument = getArgument(index)
            if (argument.isStarProjection()) continue

            val variance = AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance()) ?: TypeVariance.INV
            val innerDirection = when (variance) {
                TypeVariance.INV -> ResolveDirection.UNKNOWN
                TypeVariance.OUT -> startDirection
                TypeVariance.IN -> startDirection.opposite()
            }

            argument.getType().visitType(innerDirection, action)
        }
    }

    private fun ResolveDirection.opposite() = when (this) {
        ResolveDirection.UNKNOWN -> ResolveDirection.UNKNOWN
        ResolveDirection.TO_SUPERTYPE -> ResolveDirection.TO_SUBTYPE
        ResolveDirection.TO_SUBTYPE -> ResolveDirection.TO_SUPERTYPE
    }
}
