/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

fun CompilerPhase<*, *, *>.toPhaseMap(): MutableMap<String, AnyNamedPhase> =
    getNamedSubphases().fold(mutableMapOf()) { acc, (_, phase) ->
        check(phase.name !in acc) { "Duplicate phase name '${phase.name}'" }
        acc[phase.name] = phase
        acc
    }

/**
 * Phase configuration that defines and configures [CompilerPhase]s that the compiler should execute.
 * It is defined before compilation and can't be modified in the process.
 */
class PhaseConfig(
    disabledPhases: Set<String> = emptySet(),
    val verbose: Set<String> = emptySet(),
    val toDumpStateBefore: PhaseSet = PhaseSet.Enum(emptySet()),
    val toDumpStateAfter: PhaseSet = PhaseSet.Enum(emptySet()),
    private val toValidateStateBefore: PhaseSet = PhaseSet.Enum(emptySet()),
    private val toValidateStateAfter: PhaseSet = PhaseSet.Enum(emptySet()),
    override val dumpToDirectory: String? = null,
    override val dumpOnlyFqName: String? = null,
    override val needProfiling: Boolean = false,
    override val checkConditions: Boolean = false,
    override val checkStickyConditions: Boolean = false
) : PhaseConfigurationService {
    private val disabledMut = disabledPhases.toMutableSet()

    override fun isEnabled(phase: AnyNamedPhase): Boolean =
        phase.name !in disabledMut

    override fun isVerbose(phase: AnyNamedPhase): Boolean =
        phase.name in verbose

    override fun disable(phase: AnyNamedPhase) {
        disabledMut += phase.name
    }

    override fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateBefore

    override fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateAfter

    override fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateBefore

    override fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateAfter
}
