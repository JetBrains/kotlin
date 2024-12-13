/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

/**
 * Control which parts of compilation pipeline are enabled and how compiler should validate their invariants.
 * Phase configuration does not know anything about actual compiler pipeline upfront.
 *
 * @property verbose specify a set of phases that must print additional information during phase execution.
 * @property toDumpStateBefore specify a set of phases for which the state must be dumped right before phase execution.
 * @property toDumpStateAfter specify a set of phases for which the state must be dumped right after phase execution.
 * @property toValidateStateBefore specify a set of phases for which the compiler must validate state right before phase execution.
 * @property toValidateStateAfter specify a set of phases for which the compiler must validate state right after phase execution.
 * @property dumpToDirectory returns a path to a directory that should store phase dump. `null` if directory is not set.
 * @property dumpOnlyFqName returns a fully-qualified name that should be used to filter phase dump. `null` if dump should not be filtered.
 * @property needProfiling returns true if compiler should measure how long takes each phase.
 * @property checkConditions returns true if compiler should check pre- and post-conditions of compiler phases.
 * @property checkStickyConditions returns true if compiler should check post-conditions that are applicable to subsequent (thus "sticky") phases.
 */
class PhaseConfig(
    disabledPhases: Set<String> = emptySet(),
    val verbose: PhaseSet = PhaseSet.Enum(emptySet()),
    val toDumpStateBefore: PhaseSet = PhaseSet.Enum(emptySet()),
    val toDumpStateAfter: PhaseSet = PhaseSet.Enum(emptySet()),
    private val toValidateStateBefore: PhaseSet = PhaseSet.Enum(emptySet()),
    private val toValidateStateAfter: PhaseSet = PhaseSet.Enum(emptySet()),
    val dumpToDirectory: String? = null,
    val dumpOnlyFqName: String? = null,
    val needProfiling: Boolean = false,
    val checkConditions: Boolean = false,
    val checkStickyConditions: Boolean = false
) {
    private val disabledMut = disabledPhases.toMutableSet()

    /**
     * Check if the given [phase] should be executed during compilation.
     */
    fun isEnabled(phase: AnyNamedPhase): Boolean =
        phase.name !in disabledMut

    /**
     * Check if the compiler should print additional information during [phase] execution.
     */
    fun isVerbose(phase: AnyNamedPhase): Boolean =
        phase in verbose

    /**
     * Prevent compiler from executing the given [phase].
     */
    fun disable(phase: AnyNamedPhase) {
        disabledMut += phase.name
    }

    /**
     * Check if compiler should dump its state right before the execution of the given [phase].
     */
    fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateBefore

    /**
     * Check if compiler should dump its state right after the execution of the given [phase].
     */
    fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateAfter

    /**
     * Check if compiler should validate its state right before the execution of the given [phase].
     */
    fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateBefore

    /**
     * Check if compiler should validate its state right after the execution of the given [phase].
     */
    fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateAfter
}
