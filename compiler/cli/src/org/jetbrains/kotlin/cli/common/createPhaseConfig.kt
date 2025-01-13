/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.config.phaser.*
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

fun createPhaseConfig(
    arguments: CommonCompilerArguments,
    phasesToExecute: CompilerPhase<*, *, *>? = null,
): PhaseConfig {
    val toDumpBoth = createPhaseSetFromArguments(arguments.phasesToDump)
    val toValidateBoth = createPhaseSetFromArguments(arguments.phasesToValidate)

    val (additionalBefore, additionalAfter) = getCornerPhasesToDump(arguments, phasesToExecute)
    return PhaseConfig(
        createPhaseSetFromArguments(arguments.disablePhases),
        createPhaseSetFromArguments(arguments.verbosePhases),
        createPhaseSetFromArguments(arguments.phasesToDumpBefore) + toDumpBoth + additionalBefore,
        createPhaseSetFromArguments(arguments.phasesToDumpAfter) + toDumpBoth + additionalAfter,
        createPhaseSetFromArguments(arguments.phasesToValidateBefore) + toValidateBoth,
        createPhaseSetFromArguments(arguments.phasesToValidateAfter) + toValidateBoth,
        arguments.dumpDirectory,
        arguments.dumpOnlyFqName,
        arguments.profilePhases,
        arguments.checkPhaseConditions,
        arguments.checkStickyPhaseConditions
    )
}

private fun getCornerPhasesToDump(
    arguments: CommonCompilerArguments,
    phasesToExecute: CompilerPhase<*, *, *>? = null,
): Pair<PhaseSet, PhaseSet> {
    if (phasesToExecute != null && arguments.phasesToDump?.contains("IrLowering") == true) {
        val (firstPhase, lastPhase) = phasesToExecute.getNamedSubphases().let { it.first().second.name to it.last().second.name }
        return PhaseSet.Enum(setOf(firstPhase)) to PhaseSet.Enum(setOf(lastPhase))
    }
    return PhaseSet.Empty to PhaseSet.Empty
}

fun PhaseConfig.list(compoundPhase: CompilerPhase<*, *, *>) {
    for ((depth, phase) in compoundPhase.getNamedSubphases()) {
        println(buildString {
            append("    ".repeat(depth))
            append(phase.name)
            if (!isEnabled(phase)) append(" (Disabled)")
            if (isVerbose(phase)) append(" (Verbose)")
        })
    }
}

private fun createPhaseSetFromArguments(names: Array<String>?): PhaseSet = when {
    names == null -> PhaseSet.Empty
    "all" in names.map { it.toLowerCaseAsciiOnly() } -> PhaseSet.ALL
    else -> PhaseSet.Enum(names.toSet())
}
