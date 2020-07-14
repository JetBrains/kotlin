/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantModalityModifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantReturnUnitType
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantVisibilityModifierChecker

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val declarationCheckers: List<FirBasicDeclarationChecker> = listOf(
        FirAnnotationClassDeclarationChecker,
        FirModifierChecker,
        RedundantVisibilityModifierChecker,
        RedundantReturnUnitType
    )

    override val memberDeclarationCheckers: List<FirMemberDeclarationChecker> = listOf(
        FirInfixFunctionDeclarationChecker,
        FirExposedVisibilityChecker,
        RedundantModalityModifierChecker
    )

    override val constructorCheckers: List<FirConstructorChecker> = listOf(
        FirConstructorAllowedChecker
    )
}