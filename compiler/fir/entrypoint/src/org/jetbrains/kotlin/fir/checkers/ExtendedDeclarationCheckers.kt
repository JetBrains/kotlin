/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.extended.*

object ExtendedDeclarationCheckers : DeclarationCheckers() {
    override val declarationCheckers = listOf(
        RedundantVisibilityModifierChecker,
        RedundantReturnUnitType
    )

    override val memberDeclarationCheckers = listOf(
        RedundantModalityModifierChecker,
        RedundantExplicitTypeChecker,
        RedundantSetterParameterTypeChecker
    )

    override val variableAssignmentCfaBasedCheckers: List<AbstractFirPropertyInitializationChecker> = listOf(
        CanBeValChecker,
    )

    override val controlFlowAnalyserCheckers: List<FirControlFlowChecker> = listOf(
        UnusedChecker
    )
}
