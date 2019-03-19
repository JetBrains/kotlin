/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
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

    return PhaseConfig(
        compoundPhase, phases, enabled, verbose, toDumpStateBefore, toDumpStateAfter, toValidateStateBefore, toValidateStateAfter,
        needProfiling, checkConditions, checkStickyConditions
    )
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
    return phaseNames.mapNotNull {
        phases[it] ?: run {
            warn(config, "no phase named $it, ignoring")
            null
        }
    }.toSet()
}

private fun warn(config: CompilerConfiguration, message: String) {
    val messageCollector = config.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE
    messageCollector.report(CompilerMessageSeverity.WARNING, message)
}
