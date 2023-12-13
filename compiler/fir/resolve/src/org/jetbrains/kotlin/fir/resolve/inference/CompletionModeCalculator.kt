/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import java.util.*

fun Candidate.computeCompletionMode(
    components: InferenceComponents,
    resolutionMode: ResolutionMode,
    currentReturnType: ConeKotlinType?
): ConstraintSystemCompletionMode {
    return when {
        // Expected type is present or call is being resolved in independent context
        resolutionMode.forceFullCompletion -> ConstraintSystemCompletionMode.FULL

        // This is questionable as null return type can be only for error call
        currentReturnType == null -> ConstraintSystemCompletionMode.PARTIAL

        // Full if return type for call has no type variables
        csBuilder.isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL

        else -> CalculatorForNestedCall(
            this, currentReturnType, csBuilder, components.trivialConstraintTypeInferenceOracle
        ).computeCompletionMode()
    }
}

private typealias CsCompleterContext = ConstraintSystemCompletionContext

private class CalculatorForNestedCall(
    private val candidate: Candidate,
    private val returnType: ConeKotlinType?,
    private val context: CsCompleterContext,
    private val oracle: TrivialConstraintTypeInferenceOracle
) {
    private enum class FixationDirection {
        TO_SUBTYPE, EQUALITY
    }

    private val fixationDirectionsForVariables: MutableMap<VariableWithConstraints, FixationDirection> =
        newLinkedHashMapWithExpectedSize(context.notFixedTypeVariables.size)
    private val variablesWithQueuedConstraints = mutableSetOf<TypeVariableMarker>()
    private val typesToProcess: Queue<KotlinTypeMarker> = ArrayDeque()

    private val postponedAtoms: List<PostponedResolvedAtom> by lazy {
        ConstraintSystemCompleter.getOrderedNotAnalyzedPostponedArguments(candidate)
    }

    fun computeCompletionMode(): ConstraintSystemCompletionMode = with(context) {
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

            if (!type.contains { it.typeConstructor() in notFixedTypeVariables })
                continue

            val fixationDirectionsFromType = mutableSetOf<FixationDirectionForVariable>()
            collectRequiredDirectionsForVariables(type, TypeVariance.OUT, fixationDirectionsFromType)

            for (directionForVariable in fixationDirectionsFromType) {
                updateDirection(directionForVariable)
                enqueueTypesFromConstraints(directionForVariable.variable)
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
            if (oldDirection != FixationDirection.EQUALITY && oldDirection != newDirection)
                fixationDirectionsForVariables[variable] = FixationDirection.EQUALITY
        } ?: run {
            fixationDirectionsForVariables[variable] = newDirection
        }
    }

    private data class FixationDirectionForVariable(val variable: VariableWithConstraints, val direction: FixationDirection)

    private fun CsCompleterContext.collectRequiredDirectionsForVariables(
        type: KotlinTypeMarker, outerVariance: TypeVariance,
        fixationDirectionsCollector: MutableSet<FixationDirectionForVariable>
    ) {
        val unwrappedType = type.lowerBoundIfFlexible()
        val typeArgumentsCount = unwrappedType.argumentsCount()
        if (typeArgumentsCount > 0 && !unwrappedType.isError()) {
            for (position in 0 until typeArgumentsCount) {
                val argument = unwrappedType.getArgument(position)
                val parameter = unwrappedType.typeConstructor().getParameter(position)

                if (argument.isStarProjection())
                    continue

                collectRequiredDirectionsForVariables(
                    argument.getType(),
                    compositeVariance(outerVariance, argument, parameter),
                    fixationDirectionsCollector
                )
            }
        } else {
            processTypeWithoutParameters(type, outerVariance, fixationDirectionsCollector)
        }
    }

    private fun CsCompleterContext.compositeVariance(
        outerVariance: TypeVariance,
        argument: TypeArgumentMarker,
        parameter: TypeParameterMarker
    ): TypeVariance {
        val effectiveArgumentVariance = AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())
            ?: TypeVariance.INV // conflicting variance
        return when (outerVariance) {
            TypeVariance.INV -> TypeVariance.INV
            TypeVariance.OUT -> effectiveArgumentVariance
            TypeVariance.IN -> effectiveArgumentVariance.reversed()
        }
    }

    private fun TypeVariance.reversed(): TypeVariance = when (this) {
        TypeVariance.IN -> TypeVariance.OUT
        TypeVariance.OUT -> TypeVariance.IN
        TypeVariance.INV -> TypeVariance.INV
    }

    private fun CsCompleterContext.processTypeWithoutParameters(
        type: KotlinTypeMarker, compositeVariance: TypeVariance,
        newRequirementsCollector: MutableSet<FixationDirectionForVariable>
    ) {
        val variableWithConstraints = notFixedTypeVariables[type.typeConstructor()] ?: return
        val direction = when (compositeVariance) {
            TypeVariance.IN -> FixationDirection.EQUALITY // Assuming that variables in contravariant positions are fixed to subtype
            TypeVariance.OUT -> FixationDirection.TO_SUBTYPE
            TypeVariance.INV -> FixationDirection.EQUALITY
        }
        val requirement = FixationDirectionForVariable(variableWithConstraints, direction)
        newRequirementsCollector.add(requirement)
    }

    private fun CsCompleterContext.hasProperConstraint(
        variableWithConstraints: VariableWithConstraints,
        direction: FixationDirection
    ): Boolean {
        val constraints = variableWithConstraints.constraints
        val variable = variableWithConstraints.typeVariable

        // ILT constraint tracking is necessary to prevent incorrect full completion from Nothing constraint
        // Consider ILT <: T; Nothing <: T for T requiring lower constraint
        // Nothing would trigger full completion, but resulting type would be Int
        // Possible restrictions on integer constant from outer calls would be ignored

        var iltConstraintPresent = false
        var properConstraintPresent = false
        var nonNothingProperConstraintPresent = false

        for (constraint in constraints) {
            if (!constraint.hasRequiredKind(direction) || !isProperType(constraint.type))
                continue

            if (constraint.type.typeConstructor().isIntegerLiteralTypeConstructor()) {
                iltConstraintPresent = true
            } else if (oracle.isSuitableResultedType(constraint.type)) {
                properConstraintPresent = true
                nonNothingProperConstraintPresent = true
            } else if (!isLowerConstraintForPartiallyAnalyzedVariable(constraint, variable)) {
                properConstraintPresent = true
            }
        }

        if (!properConstraintPresent) return false

        return !iltConstraintPresent || nonNothingProperConstraintPresent
    }

    private fun Constraint.hasRequiredKind(direction: FixationDirection) = when (direction) {
        FixationDirection.TO_SUBTYPE -> kind.isLower() || kind.isEqual()
        FixationDirection.EQUALITY -> kind.isEqual()
    }

    private fun CsCompleterContext.isLowerConstraintForPartiallyAnalyzedVariable(
        constraint: Constraint,
        variable: TypeVariableMarker
    ): Boolean {
        val defaultType = variable.defaultType()
        return constraint.kind.isLower() && postponedAtoms.any { atom ->
            atom.expectedType?.contains { type -> defaultType == type } ?: false
        }
    }
}
