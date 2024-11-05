/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

fun CompilerPhase<*, *, *>.toPhaseMap(): MutableMap<String, AnyNamedPhase> =
    getNamedSubphases().fold(mutableMapOf()) { acc, (_, phase) ->
        check(phase.name !in acc) { "Duplicate phase name '${phase.name}'" }
        acc[phase.name] = phase
        acc
    }

class PhaseConfigBuilder(private val compoundPhase: CompilerPhase<*, *, *>) {
    val enabled = mutableSetOf<AnyNamedPhase>()
    val verbose = mutableSetOf<AnyNamedPhase>()
    val toDumpStateBefore = mutableSetOf<AnyNamedPhase>()
    val toDumpStateAfter = mutableSetOf<AnyNamedPhase>()
    var dumpToDirectory: String? = null
    var dumpOnlyFqName: String? = null
    val toValidateStateBefore = mutableSetOf<AnyNamedPhase>()
    val toValidateStateAfter = mutableSetOf<AnyNamedPhase>()
    var needProfiling = false
    var checkConditions = false
    var checkStickyConditions = false

    fun build() = PhaseConfig(
        compoundPhase, compoundPhase.toPhaseMap(), enabled,
        verbose, toDumpStateBefore, toDumpStateAfter, toValidateStateBefore, toValidateStateAfter,
        dumpToDirectory, dumpOnlyFqName,
        needProfiling, checkConditions, checkStickyConditions
    )
}

/**
 * Phase configuration that defines and configures [CompilerPhase]s that the compiler should execute.
 * It is defined before compilation and can't be modified in the process.
 */
class PhaseConfig(
    private val compoundPhase: CompilerPhase<*, *, *>,
    phases: Map<String, AnyNamedPhase> = compoundPhase.toPhaseMap(),
    initiallyEnabled: Set<AnyNamedPhase> = phases.values.toSet(),

    val verbose: Set<AnyNamedPhase> = emptySet(),
    val toDumpStateBefore: Set<AnyNamedPhase> = emptySet(),
    val toDumpStateAfter: Set<AnyNamedPhase> = emptySet(),
    private val toValidateStateBefore: Set<AnyNamedPhase> = emptySet(),
    private val toValidateStateAfter: Set<AnyNamedPhase> = emptySet(),
    override val dumpToDirectory: String? = null,
    override val dumpOnlyFqName: String? = null,
    override val needProfiling: Boolean = false,
    override val checkConditions: Boolean = false,
    override val checkStickyConditions: Boolean = false
) : PhaseConfigurationService {
    private val enabledMut = initiallyEnabled.toMutableSet()

    override fun isEnabled(phase: AnyNamedPhase): Boolean =
        phase in enabledMut

    override fun isVerbose(phase: AnyNamedPhase): Boolean =
        phase in verbose

    override fun disable(phase: AnyNamedPhase) {
        enabledMut.remove(phase)
    }

    override fun shouldDumpStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateBefore

    override fun shouldDumpStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toDumpStateAfter

    override fun shouldValidateStateBefore(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateBefore

    override fun shouldValidateStateAfter(phase: AnyNamedPhase): Boolean =
        phase in toValidateStateAfter

    fun list() {
        for ((depth, phase) in compoundPhase.getNamedSubphases()) {
            println(buildString {
                append("    ".repeat(depth))
                append(phase.name)
                if (phase !in enabledMut) append(" (Disabled)")
                if (phase in verbose) append(" (Verbose)")
            })
        }
    }
}
