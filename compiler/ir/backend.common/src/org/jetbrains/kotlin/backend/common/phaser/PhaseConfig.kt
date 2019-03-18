/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

fun createPhaseConfig(compoundPhase: CompilerPhase<*, *, *>, config: CompilerConfiguration): PhaseConfig {
    val phases = compoundPhase.toPhaseMap()
    val enabled = computeEnabled(phases, config).toMutableSet()
    val verbose = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.VERBOSE_PHASES)

    val beforeDumpSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_DUMP_STATE_BEFORE)
    val afterDumpSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_DUMP_STATE_AFTER)
    val bothDumpSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_DUMP_STATE)
    val toDumpStateBefore = beforeDumpSet + bothDumpSet
    val toDumpStateAfter = afterDumpSet + bothDumpSet
    val beforeValidateSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_VALIDATE_BEFORE)
    val afterValidateSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_VALIDATE_AFTER)
    val bothValidateSet = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.PHASES_TO_VALIDATE)
    val toValidateStateBefore = beforeValidateSet + bothValidateSet
    val toValidateStateAfter = afterValidateSet + bothValidateSet

    val needProfiling = config.getBoolean(CommonConfigurationKeys.PROFILE_PHASES)
    val checkConditions = config.getBoolean(CommonConfigurationKeys.CHECK_PHASE_CONDITIONS)
    val checkStickyConditions = config.getBoolean(CommonConfigurationKeys.CHECK_STICKY_CONDITIONS)

    return PhaseConfig(compoundPhase, phases, enabled, verbose, toDumpStateBefore, toDumpStateAfter, toValidateStateBefore, toValidateStateAfter, needProfiling, checkConditions, checkStickyConditions)
}

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

private fun CompilerPhase<*, *, *>.toPhaseMap(): MutableMap<String, AnyNamedPhase> =
    getNamedSubphases().fold(mutableMapOf()) { acc, (_, phase) ->
        check(phase.name !in acc) { "Duplicate phase name '${phase.name}'"}
        acc[phase.name] = phase
        acc
    }

private fun computeEnabled(
    phases: MutableMap<String, AnyNamedPhase>,
    config: CompilerConfiguration
): Set<AnyNamedPhase> {
    val disabledPhases = phaseSetFromConfiguration(phases, config, CommonConfigurationKeys.DISABLED_PHASES)
    return phases.values.toSet() - disabledPhases
}

private fun phaseSetFromConfiguration(
    phases: MutableMap<String, AnyNamedPhase>,
    config: CompilerConfiguration,
    key: CompilerConfigurationKey<Set<String>>
): Set<AnyNamedPhase> {
    val phaseNames = config.get(key) ?: emptySet()
    if ("ALL" in phaseNames) return phases.values.toSet()
    return phaseNames.map { phases[it]!! }.toSet()
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
