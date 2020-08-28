/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSealedClassConstructorCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val expressionCheckers: List<FirBasicExpresionChecker> = listOf()
    override val qualifiedAccessCheckers: List<FirQualifiedAccessChecker> = listOf(
        FirSuperNotAvailableChecker,
        FirNotASupertypeChecker,
        FirSuperclassNotAccessibleFromInterfaceChecker,
        FirAbstractSuperCallChecker,
        FirQualifiedSupertypeExtendedByOtherSupertypeChecker,
        FirProjectionsOnNonClassTypeArgumentChecker,
        FirUpperBoundViolatedChecker,
        FirTypeArgumentsNotAllowedExpressionChecker,
        FirSealedClassConstructorCallChecker,
    )
    override val functionCallCheckers: List<FirFunctionCallChecker> = listOf()
}
