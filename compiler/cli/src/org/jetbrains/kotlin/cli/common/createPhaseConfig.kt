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
    phasesToExecute: List<AnyNamedPhase>? = null,
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
    )
}

private fun getCornerPhasesToDump(
    arguments: CommonCompilerArguments,
    phasesToExecute: List<AnyNamedPhase>? = null,
): Pair<PhaseSet, PhaseSet> {
    if (phasesToExecute != null && arguments.phasesToDump?.contains("IrLowering") == true) {
        return PhaseSet.Enum(setOf(phasesToExecute.first().name)) to PhaseSet.Enum(setOf(phasesToExecute.last().name))
    }
    return PhaseSet.Empty to PhaseSet.Empty
}

fun PhaseConfig.list(compoundPhase: CompilerPhase<*, *, *>) {
    list(compoundPhase.getNamedSubphases())
}

@JvmName("listPhases")
fun PhaseConfig.list(phases: List<AnyNamedPhase>) {
    list(phases.flatMap { it.getNamedSubphases() })
}

private fun PhaseConfig.list(phases: List<Pair<Int, AnyNamedPhase>>) {
    for ((depth, phase) in phases) {
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
    "all" in names.map { it.toLowerCaseAsciiOnly() } -> PhaseSet.All
    else -> PhaseSet.Enum(names.toSet())
}
