/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.resolve.calls.inference.components.VariableFixationFinder.TypeVariableFixationReadiness
import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintSystemError
import org.jetbrains.kotlin.resolve.calls.inference.model.InitialConstraint
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeVariableMarker

@DefaultImplementation(ConstraintsLogger.Dummy::class)
abstract class ConstraintsLogger {
    protected abstract val currentContext: TypeSystemInferenceExtensionContext?

    protected abstract fun verifyContext(context: TypeSystemInferenceExtensionContext)

    abstract fun logInitial(constraint: InitialConstraint, context: TypeSystemInferenceExtensionContext)

    abstract fun log(variable: TypeVariableMarker, constraint: Constraint, context: TypeSystemInferenceExtensionContext)

    abstract fun logConstraintSubstitution(
        variable: TypeVariableMarker,
        substitutedConstraint: Constraint,
        context: TypeSystemInferenceExtensionContext
    )

    abstract fun logError(error: ConstraintSystemError, context: TypeSystemInferenceExtensionContext)

    abstract fun logNewVariable(variable: TypeVariableMarker, context: TypeSystemInferenceExtensionContext)

    class FixationLogRecord(
        val map: Map<TypeVariableMarker, FixationLogVariableInfo>,
        val chosen: TypeVariableMarker?,
    ) {
        var fixedTo: KotlinTypeMarker? = null
    }

    class FixationLogVariableInfo(
        val readiness: TypeVariableFixationReadiness,
        val constraints: List<Constraint>,
    )

    abstract fun logReadiness(
        fixationLog: FixationLogRecord,
        context: TypeSystemInferenceExtensionContext,
    )

    abstract val currentState: State

    abstract class State() {
        abstract fun withPrevious(constraint: InitialConstraint)

        abstract fun withPrevious(
            variable1: TypeVariableMarker, constraint1: Constraint,
            variable2: TypeVariableMarker, constraint2: Constraint,
        )

        abstract fun restore()
    }

    /**
     * Exists to satisfy K1's dependency injection as it doesn't support `null` values.
     */
    object Dummy : ConstraintsLogger() {
        override val currentContext: TypeSystemInferenceExtensionContext
            get() = error("Should not be called")

        override fun verifyContext(context: TypeSystemInferenceExtensionContext) {}

        override fun logInitial(
            constraint: InitialConstraint,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override fun log(
            variable: TypeVariableMarker,
            constraint: Constraint,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override fun logConstraintSubstitution(
            variable: TypeVariableMarker,
            substitutedConstraint: Constraint,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override fun logError(
            error: ConstraintSystemError,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override fun logNewVariable(
            variable: TypeVariableMarker,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override fun logReadiness(
            fixationLog: FixationLogRecord,
            context: TypeSystemInferenceExtensionContext
        ) {
        }

        override val currentState: State = object : State() {
            override fun withPrevious(constraint: InitialConstraint) {}

            override fun withPrevious(
                variable1: TypeVariableMarker,
                constraint1: Constraint,
                variable2: TypeVariableMarker,
                constraint2: Constraint
            ) {
            }

            override fun restore() {}
        }
    }
}

inline fun <T> ConstraintsLogger?.withStateAdvancement(block: () -> T, advance: ConstraintsLogger.State.() -> Unit): T {
    val oldContext = this?.currentState
    return try {
        oldContext?.advance()
        block()
    } finally {
        oldContext?.restore()
    }
}

inline fun <T> ConstraintsLogger?.withPrevious(constraint: InitialConstraint, block: () -> T): T =
    withStateAdvancement(block) { withPrevious(constraint) }

inline fun <T> ConstraintsLogger?.withPrevious(
    variable1: TypeVariableMarker,
    constraint1: Constraint,
    variable2: TypeVariableMarker,
    constraint2: Constraint,
    block: () -> T,
): T = withStateAdvancement(block) { withPrevious(variable1, constraint1, variable2, constraint2) }
