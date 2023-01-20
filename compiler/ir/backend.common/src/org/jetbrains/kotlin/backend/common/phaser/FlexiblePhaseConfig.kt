/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * Phase configuration that does not know anything
 * about actual compiler pipeline upfront.
 */
class FlexiblePhaseConfig(
    disabled: Set<String>,
    private val verbose: Set<String>,
    private val toDumpStateBefore: PhaseSet,
    private val toDumpStateAfter: PhaseSet,
    private val toValidateStateBefore: PhaseSet,
    private val toValidateStateAfter: PhaseSet,
    override val dumpToDirectory: String? = null,
    override val dumpOnlyFqName: String? = null,
    override val needProfiling: Boolean = false,
    override val checkConditions: Boolean = false,
    override val checkStickyConditions: Boolean = false
) : PhaseConfigurationService {
    private val disabledMut = disabled.toMutableSet()

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

sealed class PhaseSet {
    abstract operator fun contains(phase: AnyNamedPhase): Boolean

    abstract operator fun plus(phaseSet: PhaseSet): PhaseSet

    class Enum(val phases: Set<String>) : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean =
            phase.name.toLowerCaseAsciiOnly() in phases

        override fun plus(phaseSet: PhaseSet): PhaseSet = when (phaseSet) {
            ALL -> ALL
            is Enum -> Enum(phases + phaseSet.phases)
        }
    }
    object ALL : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean =
            true

        override fun plus(phaseSet: PhaseSet): PhaseSet = ALL
    }

}
