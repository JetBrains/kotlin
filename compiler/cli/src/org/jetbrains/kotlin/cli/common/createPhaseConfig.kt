/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.config.phaser.AnyNamedPhase
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

fun createPhaseConfig(
    compoundPhase: CompilerPhase<*, *, *>,
    arguments: CommonCompilerArguments
): PhaseConfig {
    val phases = compoundPhase.toPhaseMap()
    val disabled = computeDisabled(phases, arguments.disablePhases).toMutableSet()
    val verbose = phaseSetFromArguments(phases, arguments.verbosePhases)

    val beforeDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpBefore)
    val afterDumpSet = phaseSetFromArguments(phases, arguments.phasesToDumpAfter)
    val bothDumpSet = phaseSetFromArguments(phases, arguments.phasesToDump)
    val toDumpStateBefore = beforeDumpSet + bothDumpSet
    val toDumpStateAfter = afterDumpSet + bothDumpSet
    val dumpDirectory = arguments.dumpDirectory
    val dumpOnlyFqName = arguments.dumpOnlyFqName
    val beforeValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateBefore)
    val afterValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidateAfter)
    val bothValidateSet = phaseSetFromArguments(phases, arguments.phasesToValidate)
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
): Set<AnyNamedPhase> {
    return phaseSetFromArguments(phases, namesOfDisabled)
}

private fun phaseSetFromArguments(
    phases: MutableMap<String, AnyNamedPhase>,
    names: Array<String>?,
): Set<AnyNamedPhase> {
    if (names == null) return emptySet()
    if ("ALL" in names) return phases.values.toSet()
    return names.mapNotNull { phases[it] }.toSet()
}
