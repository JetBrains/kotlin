/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.backend.common.phaser.FlexiblePhaseConfig
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

fun createFlexiblePhaseConfig(
    arguments: CommonCompilerArguments,
): FlexiblePhaseConfig {
    fun Array<String>?.asNonNullSet(): Set<String> = this?.toSet() ?: emptySet()

    val toDumpBoth = arguments.phasesToDump.asNonNullSet()
    val toValidateBoth = arguments.phasesToValidate.asNonNullSet()

    return FlexiblePhaseConfig(
        disabled = arguments.disablePhases.asNonNullSet(),
        verbose = arguments.verbosePhases.asNonNullSet(),
        toDumpStateBefore = arguments.phasesToDumpBefore.asNonNullSet() + toDumpBoth,
        toDumpStateAfter = arguments.phasesToDumpAfter.asNonNullSet() + toDumpBoth,
        toValidateStateBefore = arguments.phasesToValidateBefore.asNonNullSet() + toValidateBoth,
        toValidateStateAfter = arguments.phasesToValidateAfter.asNonNullSet() + toValidateBoth,
        dumpOnlyFqName = arguments.dumpOnlyFqName,
        dumpToDirectory = arguments.dumpDirectory,
        needProfiling = arguments.profilePhases,
        checkConditions = arguments.checkPhaseConditions,
        checkStickyConditions = arguments.checkStickyPhaseConditions,
    )
}