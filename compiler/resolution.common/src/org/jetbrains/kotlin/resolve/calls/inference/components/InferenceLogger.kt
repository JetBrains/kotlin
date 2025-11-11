/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.InitialConstraint
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

@OptIn(AllowedToUsedOnlyInK1::class)
@DefaultImplementation(InferenceLogger.Dummy::class)
abstract class InferenceLogger {
    abstract fun logInitial(constraint: InitialConstraint, system: ConstraintSystemMarker)

    /**
     * Registers the creation of a new [Constraint].
     * Note that this function tracks the instantiation and not the addition of the constraint to the constraint storage.
     * This is because the compiler may want to "skip" adding some intermediate constraints, while the dumper should still
     * print them to make the inferences more obvious (see
     * [MutableVariableWithConstraints.addConstraint][org.jetbrains.kotlin.resolve.calls.inference.model.MutableVariableWithConstraints.addConstraint]
     * and its `isMatchingForSimplification`).
     */
    abstract fun log(variable: TypeVariableMarker, constraint: Constraint, system: ConstraintSystemMarker)

    abstract fun logError(error: ConstraintSystemError, system: ConstraintSystemMarker)

    abstract fun logNewVariable(variable: TypeVariableMarker, system: ConstraintSystemMarker)

    class FixationLogRecord(
        val map: Map<TypeVariableMarker, FixationLogVariableInfo<*>>,
        val chosen: TypeVariableMarker?,
    ) {
        var fixedTo: KotlinTypeMarker? = null
    }

    class FixationLogVariableInfo<Readiness : Any>(
        val readiness: Readiness,
        val constraints: List<Constraint>,
    ) {
        val constraintsBeforeFixationCount = constraints.size
    }

    abstract fun logReadiness(
        fixationLog: FixationLogRecord,
        system: ConstraintSystemMarker,
    )

    // Doesn't need to be performant, because it's only called in tests with the inference logger.
    abstract fun <T> withOrigin(constraint: InitialConstraint, block: () -> T): T

    // Doesn't need to be performant, because it's only called in tests with the inference logger.
    abstract fun <T> withOrigins(
        variable1: TypeVariableMarker,
        constraint1: Constraint,
        variable2: TypeVariableMarker,
        constraint2: Constraint,
        block: () -> T,
    ): T

    /**
     * Exists to satisfy K1's dependency injection as it doesn't support `null` values.
     */
    @AllowedToUsedOnlyInK1
    object Dummy : InferenceLogger() {
        override fun logInitial(
            constraint: InitialConstraint,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun log(
            variable: TypeVariableMarker,
            constraint: Constraint,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logError(
            error: ConstraintSystemError,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logNewVariable(
            variable: TypeVariableMarker,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun logReadiness(
            fixationLog: FixationLogRecord,
            system: ConstraintSystemMarker
        ) = error("Should never be called")

        override fun <T> withOrigin(constraint: InitialConstraint, block: () -> T): T = error("Should never be called")

        override fun <T> withOrigins(
            variable1: TypeVariableMarker,
            constraint1: Constraint,
            variable2: TypeVariableMarker,
            constraint2: Constraint,
            block: () -> T,
        ): T = error("Should never be called")
    }
}

inline fun <T> InferenceLogger?.withOrigin(constraint: InitialConstraint, crossinline block: () -> T): T = when {
    this == null -> block()
    else -> withOrigin(constraint) { block() }
}

inline fun <T> InferenceLogger?.withOrigins(
    variable1: TypeVariableMarker,
    constraint1: Constraint,
    variable2: TypeVariableMarker,
    constraint2: Constraint,
    crossinline block: () -> T,
): T = when {
    this == null -> block()
    else -> withOrigins(variable1, constraint1, variable2, constraint2) { block() }
}
