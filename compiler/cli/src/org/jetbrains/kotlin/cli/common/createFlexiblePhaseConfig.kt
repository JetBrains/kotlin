/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.phaser.FlexiblePhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaseSet
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

fun createFlexiblePhaseConfig(
    arguments: CommonCompilerArguments,
): FlexiblePhaseConfig {
    fun Array<String>?.asNonNullSet(): Set<String> = this?.toSet() ?: emptySet()

    val toDumpBoth = createPhaseSetFromArguments(arguments.phasesToDump)
    val toValidateBoth = createPhaseSetFromArguments(arguments.phasesToValidate)

    return FlexiblePhaseConfig(
        disabled = arguments.disablePhases.asNonNullSet(),
        verbose = arguments.verbosePhases.asNonNullSet(),
        toDumpStateBefore = createPhaseSetFromArguments(arguments.phasesToDumpBefore) + toDumpBoth,
        toDumpStateAfter = createPhaseSetFromArguments(arguments.phasesToDumpAfter) + toDumpBoth,
        toValidateStateBefore = createPhaseSetFromArguments(arguments.phasesToValidateBefore) + toValidateBoth,
        toValidateStateAfter = createPhaseSetFromArguments(arguments.phasesToValidateAfter) + toValidateBoth,
        dumpOnlyFqName = arguments.dumpOnlyFqName,
        dumpToDirectory = arguments.dumpDirectory,
        needProfiling = arguments.profilePhases,
        checkConditions = arguments.checkPhaseConditions,
        checkStickyConditions = arguments.checkStickyPhaseConditions,
    )
}

private fun createPhaseSetFromArguments(names: Array<String>?): PhaseSet = when {
    names == null -> PhaseSet.Enum(emptySet())
    "all" in names.map { it.toLowerCaseAsciiOnly() } -> PhaseSet.ALL
    else -> PhaseSet.Enum(names.map { it.toLowerCaseAsciiOnly() }.toSet())
}