/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.checkers.extra.*

object ExtraDeclarationCheckers : DeclarationCheckers() {
    override val fileCheckers: Set<FirFileChecker> = setOf(
        PlatformClassMappedToKotlinImportsChecker,
    )

    override val anonymousFunctionCheckers: Set<FirAnonymousFunctionChecker> = setOf(
        FirAnonymousUnusedParamChecker,
    )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        RedundantVisibilityModifierSyntaxChecker,
        RedundantModalityModifierSyntaxChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        RedundantSetterParameterTypeChecker,
    )

    override val variableAssignmentCfaBasedCheckers: Set<AbstractFirPropertyInitializationChecker> = setOf(
        CanBeValChecker,
        UnusedVariableAssignmentChecker,
    )

    override val controlFlowAnalyserCheckers: Set<FirControlFlowChecker> = setOf(
        UnreachableCodeChecker,
    )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(
        RedundantReturnUnitType,
    )
}
