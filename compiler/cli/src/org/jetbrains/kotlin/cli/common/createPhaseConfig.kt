/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    fun report(message: String) = messageCollector.report(CompilerMessageSeverity.ERROR, message)

    val phases = compoundPhase.toPhaseMap()
    val disabled = computeDisabled(phases, arguments.disablePhases, ::report).toMutableSet()
    val verbose = phaseSetFromArguments(phases, arguments.verbosePhases, ::report)

    val beforeDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpBefore, ::report)
    val afterDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpAfter, ::report)
    val bothDumpSet = phaseSetFromArguments(phases, arguments.phasesToDump, ::report)
    val toDumpStateBefore = beforeDumpSet + bothDumpSet
    val toDumpStateAfter = afterDumpSet + bothDumpSet
    val dumpDirectory = arguments.dumpDirectory
    val dumpOnlyFqName = arguments.dumpOnlyFqName
    val beforeValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateBefore, ::report)
    val afterValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateAfter, ::report)
    val bothValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidate, ::report)
    val toValidateStateBefore = beforeValidateSet + bothValidateSet
    val toValidateStateAfter = afterValidateSet + bothValidateSet

    val needProfiling = arguments.profilePhases
    val checkConditions = arguments.checkPhaseConditions
    val checkStickyConditions = arguments.checkStickyPhaseConditions

    return PhaseConfig(
        disabled,
        verbose,
        toDumpStateBefore,
        toDumpStateAfter,
        toValidateStateBefore,
        toValidateStateAfter,
        dumpDirectory,
        dumpOnlyFqName,
        needProfiling,
        checkConditions,
        checkStickyConditions
    ).also {
        if (arguments.listPhases) {
            list(compoundPhase, disabled, verbose)
        }
    }
}

private fun list(
    compoundPhase: CompilerPhase<*, *, *>,
    disabled: Set<AnyNamedPhase> = mutableSetOf(),
    verbose: Set<AnyNamedPhase> = mutableSetOf(),
) {
    for ((depth, phase) in compoundPhase.getNamedSubphases()) {
        println(buildString {
            append("    ".repeat(depth))
            append(phase.name)
            if (phase in disabled) append(" (Disabled)")
            if (phase in verbose) append(" (Verbose)")
        })
    }
}

private fun computeDisabled(
    phases: MutableMap<String, AnyNamedPhase>,
    namesOfDisabled: Array<String>?,
    report: (String) -> Unit
): Set<AnyNamedPhase> {
    return phaseSetFromArguments(phases, namesOfDisabled, report)
}

private fun phaseSetFromArguments(
    phases: MutableMap<String, AnyNamedPhase>,
    names: Array<String>?,
    report: (String) -> Unit
): Set<AnyNamedPhase> {
    if (names == null) return emptySet()
    if ("ALL" in names) return phases.values.toSet()
    return names.mapNotNull {
        phases[it] ?: run {
            report("no phase named $it")
            null
        }
    }.toSet()
}
