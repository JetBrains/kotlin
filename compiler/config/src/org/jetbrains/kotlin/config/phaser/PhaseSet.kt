/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

sealed class PhaseSet {
    abstract operator fun contains(phase: AnyNamedPhase): Boolean

    abstract operator fun plus(phaseSet: PhaseSet): PhaseSet

    class Enum(phases: Set<String>) : PhaseSet() {
        private val phases: Set<String> = phases.map { it.toLowerCaseAsciiOnly() }.toSet()

        override fun contains(phase: AnyNamedPhase): Boolean =
            phase.name.toLowerCaseAsciiOnly() in phases

        override fun plus(phaseSet: PhaseSet): PhaseSet = when (phaseSet) {
            All -> All
            Empty -> this
            is Enum -> Enum(phases + phaseSet.phases)
        }
    }

    object All : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean =
            true

        override fun plus(phaseSet: PhaseSet): PhaseSet = All
    }

    object Empty : PhaseSet() {
        override fun contains(phase: AnyNamedPhase): Boolean =
            false

        override fun plus(phaseSet: PhaseSet): PhaseSet = phaseSet
    }
}
