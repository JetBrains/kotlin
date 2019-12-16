/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.KotlinResolutionCandidate
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.*
import java.util.*

typealias CsCompleterContext = KotlinConstraintSystemCompleter.Context

class CompletionModeCalculator {
    companion object {
        fun computeCompletionMode(
            candidate: KotlinResolutionCandidate,
            expectedType: UnwrappedType?,
            returnType: UnwrappedType?
        ): ConstraintSystemCompletionMode = with(candidate) {
            val csCompleterContext = getSystem().asConstraintSystemCompleterContext()

            // Presence of expected type means that we are trying to complete outermost call => completion mode should be full
            if (expectedType != null) return ConstraintSystemCompletionMode.FULL

            // This is questionable as null return type can be only for error call
            if (returnType == null) return ConstraintSystemCompletionMode.PARTIAL

            // Full if return type for call has no type variables
            if (csBuilder.isProperType(returnType)) return ConstraintSystemCompletionMode.FULL

            // For nested call with variables in return type check possibility of full completion
            return CalculatorForNestedCall(returnType, csCompleterContext).computeCompletionMode()
        }
    }

    private class CalculatorForNestedCall(
        private val returnType: UnwrappedType?,
        private val csCompleterContext: CsCompleterContext
    ) {
        private enum class FixationDirection {
            TO_SUBTYPE, TO_SUPERTYPE, EQUALITY
        }

        private val fixationDirectionsForVariables = mutableMapOf<VariableWithConstraints, FixationDirection>()
        private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()
        private val typesToProcess: Queue<KotlinTypeMarker> = ArrayDeque()

        fun computeCompletionMode(): ConstraintSystemCompletionMode = with(csCompleterContext) {
            // Add fixation directions for variables based on effective variance in type
            typesToProcess.add(returnType)
            computeDirections()

            // If all variables have required proper constraint, run full completion
            if (directionRequirementsForVariablesHold())
                return ConstraintSystemCompletionMode.FULL

            return ConstraintSystemCompletionMode.PARTIAL
        }

        private fun CsCompleterContext.computeDirections() {
            while (typesToProcess.isNotEmpty()) {
                val type = typesToProcess.poll() ?: break

                fixationRequirementForTopLevel(type)?.let { directionForVariable ->
                    updateDirection(directionForVariable)
                    enqueueTypesFromConstraints(directionForVariable.variable)
                }

                // find all variables in type and make requirements for them
                type.contains { fromReturnType ->
                    for (directionForVariable in directionsForVariablesInTypeArguments(fromReturnType)) {
                        updateDirection(directionForVariable)
                        enqueueTypesFromConstraints(directionForVariable.variable)
                    }
                    false
                }
            }
        }

        private fun enqueueTypesFromConstraints(variableWithConstraints: VariableWithConstraints) {
            val variable = variableWithConstraints.typeVariable
            if (variable !in variablesWithQueuedConstraints) {
                for (constraint in variableWithConstraints.constraints) {
                    typesToProcess.add(constraint.type)
                }

                variablesWithQueuedConstraints.add(variable)
            }
        }

        private fun CsCompleterContext.directionRequirementsForVariablesHold(): Boolean {
            for ((variable, fixationDirection) in fixationDirectionsForVariables) {
                if (!hasProperConstraint(variable, fixationDirection))
                    return false
            }
            return true
        }

        private fun updateDirection(directionForVariable: FixationDirectionForVariable) {
            val (variable, newDirection) = directionForVariable
            fixationDirectionsForVariables[variable]?.let { oldDirection ->
                // To sub and to super are merged into equality, old equality stays
                if (oldDirection != FixationDirection.EQUALITY && oldDirection != newDirection)
                    fixationDirectionsForVariables[variable] = FixationDirection.EQUALITY
            }
            fixationDirectionsForVariables[variable] = newDirection
        }

        private data class FixationDirectionForVariable(val variable: VariableWithConstraints, val direction: FixationDirection)

        private fun CsCompleterContext.fixationRequirementForTopLevel(type: KotlinTypeMarker): FixationDirectionForVariable? {
            return notFixedTypeVariables[type.typeConstructor()]?.let {
                FixationDirectionForVariable(it, FixationDirection.TO_SUBTYPE)
            }
        }

        private fun CsCompleterContext.directionsForVariablesInTypeArguments(type: KotlinTypeMarker): List<FixationDirectionForVariable> {
            assert(type.argumentsCount() == type.typeConstructor().parametersCount()) {
                "Arguments and parameters count don't match for type $type. " +
                        "Arguments: ${type.argumentsCount()}, parameters: ${type.typeConstructor().parametersCount()}"
            }

            val directionsForVariables = mutableListOf<FixationDirectionForVariable>()

            for (position in 0 until type.argumentsCount()) {
                val argument = type.getArgument(position)
                if (!argument.getType().mayBeTypeVariable())
                    continue

                val variableWithConstraints = notFixedTypeVariables[argument.getType().typeConstructor()] ?: continue

                val parameter = type.typeConstructor().getParameter(position)
                val effectiveVariance = AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())
                    ?: TypeVariance.OUT // Discuss

                val direction = when (effectiveVariance) {
                    TypeVariance.IN -> FixationDirection.TO_SUPERTYPE
                    TypeVariance.OUT -> FixationDirection.TO_SUBTYPE
                    TypeVariance.INV -> FixationDirection.EQUALITY
                }

                val requirement = FixationDirectionForVariable(variableWithConstraints, direction)
                directionsForVariables.add(requirement)
            }

            return directionsForVariables
        }

        private fun CsCompleterContext.hasProperConstraint(
            variableWithConstraints: VariableWithConstraints,
            direction: FixationDirection
        ): Boolean {
            val constraints = variableWithConstraints.constraints

            // todo check correctness for @Exact
            return constraints.isNotEmpty() && constraints.any { constraint ->
                constraint.hasRequiredKind(direction)
                        && !constraint.type.typeConstructor().isIntegerLiteralTypeConstructor()
                        && isProperType(constraint.type)
            }
        }

        private fun Constraint.hasRequiredKind(direction: FixationDirection) = when (direction) {
            FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()
            FixationDirection.TO_SUPERTYPE -> kind.isUpper() || kind.isEqual()
            FixationDirection.EQUALITY -> kind.isEqual()
        }
    }
}