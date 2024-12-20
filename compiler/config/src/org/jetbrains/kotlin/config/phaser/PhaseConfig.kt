/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

/**
 * Control which parts of compilation pipeline are enabled and how compiler should validate their invariants.
 * Phase configuration does not know anything about actual compiler pipeline upfront.
 */
class PhaseConfig(
    private val disabled: PhaseSet = PhaseSet.Enum(emptySet()),
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
    fun isEnabled(phase: AnyNamedPhase): Boolean =
        phase !in disabled

    fun isVerbose(phase: AnyNamedPhase): Boolean =
        phase in verbose

    fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateBefore

    fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateAfter

    fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateBefore

    fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateAfter
}
