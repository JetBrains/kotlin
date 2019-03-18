/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

fun createDefaultPhaseConfig(compoundPhase: CompilerPhase<*, *, *>): PhaseConfig {
    val phases = compoundPhase.toPhaseMap()
    val enabled = phases.values.toMutableSet()

    return PhaseConfig(
        compoundPhase, phases, enabled, emptySet(), emptySet(), emptySet(), emptySet(), emptySet(),
        needProfiling = false,
        checkConditions = false,
        checkStickyConditions = false
    )
}

fun CompilerPhase<*, *, *>.toPhaseMap(): MutableMap<String, AnyNamedPhase> =
    getNamedSubphases().fold(mutableMapOf()) { acc, (_, phase) ->
        check(phase.name !in acc) { "Duplicate phase name '${phase.name}'"}
        acc[phase.name] = phase
        acc
    }

class PhaseConfig(
    private val compoundPhase: CompilerPhase<*, *, *>,
    private val phases: MutableMap<String, AnyNamedPhase>,
    enabled: MutableSet<AnyNamedPhase>,
    val verbose: Set<AnyNamedPhase>,
    val toDumpStateBefore: Set<AnyNamedPhase>,
    val toDumpStateAfter: Set<AnyNamedPhase>,
    val toValidateStateBefore: Set<AnyNamedPhase>,
    val toValidateStateAfter: Set<AnyNamedPhase>,
    val needProfiling: Boolean,
    val checkConditions: Boolean,
    val checkStickyConditions: Boolean
) {
    private val enabledMut = enabled

    val enabled: Set<AnyNamedPhase> get() = enabledMut

    fun known(name: String): String {
        if (phases[name] == null) {
            error("Unknown phase: $name. Use -Xlist-phases to see the list of phases.")
        }
        return name
    }

    fun list() {
        compoundPhase.getNamedSubphases().forEach { (depth, phase) ->
            val enabled = if (phase in enabled) "(Enabled)" else ""
            val verbose = if (phase in verbose) "(Verbose)" else ""

            println(String.format("%1$-50s %2$-50s %3$-10s", "${"\t".repeat(depth)}${phase.name}:", phase.description, "$enabled $verbose"))
        }
    }

    fun enable(phase: AnyNamedPhase) {
        enabledMut.add(phase)
    }

    fun disable(phase: AnyNamedPhase) {
        enabledMut.remove(phase)
    }

    fun switch(phase: AnyNamedPhase, onOff: Boolean) {
        if (onOff) {
            enable(phase)
        } else {
            disable(phase)
        }
    }
}
