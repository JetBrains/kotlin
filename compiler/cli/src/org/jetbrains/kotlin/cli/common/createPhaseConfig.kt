/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.phaser.AnyNamedPhase
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

fun createPhaseConfig(
    compoundPhase: CompilerPhase<*, *, *>,
    arguments: CommonCompilerArguments,
    messageCollector: MessageCollector
): PhaseConfig {
    fun warn(message: String) = messageCollector.report(CompilerMessageSeverity.WARNING, message)

    val phases = compoundPhase.toPhaseMap()
    val enabled = computeEnabled(phases, arguments.disablePhases, ::warn).toMutableSet()
    val verbose = phaseSetFromArguments(phases, arguments.verbosePhases, ::warn)

    val beforeDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpBefore, ::warn)
    val afterDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpAfter, ::warn)
    val bothDumpSet = phaseSetFromArguments(phases, arguments.phasesToDump, ::warn)
    val toDumpStateBefore = beforeDumpSet + bothDumpSet
    val toDumpStateAfter = afterDumpSet + bothDumpSet
    val beforeValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateBefore, ::warn)
    val afterValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateAfter, ::warn)
    val bothValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidate, ::warn)
    val toValidateStateBefore = beforeValidateSet + bothValidateSet
    val toValidateStateAfter = afterValidateSet + bothValidateSet

    val namesOfElementsExcludedFromDumping = arguments.namesExcludedFromDumping?.toSet() ?: emptySet()

    val needProfiling = arguments.profilePhases
    val checkConditions = arguments.checkPhaseConditions
    val checkStickyConditions = arguments.checkStickyPhaseConditions

    return PhaseConfig(
        compoundPhase, phases, enabled, verbose, toDumpStateBefore, toDumpStateAfter, toValidateStateBefore, toValidateStateAfter,
        namesOfElementsExcludedFromDumping,
        needProfiling, checkConditions, checkStickyConditions
    ).also {
        if (arguments.listPhases) {
            it.list()
        }
    }
}

private fun computeEnabled(
    phases: MutableMap<String, AnyNamedPhase>,
    namesOfDisabled: Array<String>?,
    warn: (String) -> Unit
): Set<AnyNamedPhase> {
    val disabledPhases = phaseSetFromArguments(phases, namesOfDisabled, warn)
    return phases.values.toSet() - disabledPhases
}

private fun phaseSetFromArguments(
    phases: MutableMap<String, AnyNamedPhase>,
    names: Array<String>?,
    warn: (String) -> Unit
): Set<AnyNamedPhase> {
    if (names == null) return emptySet()
    if ("ALL" in names) return phases.values.toSet()
    return names.mapNotNull {
        phases[it] ?: run {
            warn("no phase named $it, ignoring")
            null
        }
    }.toSet()
}
