/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.cfa.FirPropertyInitializationAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val declarationCheckers: List<FirBasicDeclarationChecker> = listOf(
        FirAnnotationClassDeclarationChecker,
        FirModifierChecker,
        FirManyCompanionObjectsChecker,
        FirLocalEntityNotAllowedChecker,
        FirTypeParametersInObjectChecker,
    )

    override val memberDeclarationCheckers: List<FirMemberDeclarationChecker> = listOf(
        FirInfixFunctionDeclarationChecker,
        FirExposedVisibilityDeclarationChecker,
    )

    override val constructorCheckers: List<FirConstructorChecker> = listOf(
        FirConstructorAllowedChecker
    )

    override val controlFlowAnalyserCheckers: List<FirControlFlowChecker> = listOf(
        FirPropertyInitializationAnalyzer
    )
}