/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

/**
 * Phase configuration that does not know anything
 * about actual compiler pipeline upfront.
 */
class PhaseConfig(
    disabledPhases: Set<String> = emptySet(),
    val verbose: PhaseSet = PhaseSet.Enum(emptySet()),
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
        phase in verbose

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
