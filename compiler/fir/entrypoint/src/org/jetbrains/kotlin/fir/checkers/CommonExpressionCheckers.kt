/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = setOf(
        FirAnonymousFunctionChecker,
        FirCheckNotNullCallChecker,
        FirGetClassCallChecker,
        FirSafeCallExpressionChecker,
    )

    override val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker> = setOf(
        FirCallableReferenceChecker,
        FirSuperNotAvailableChecker,
        FirNotASupertypeChecker,
        FirSuperclassNotAccessibleFromInterfaceChecker,
        FirAbstractSuperCallChecker,
        FirQualifiedSupertypeExtendedByOtherSupertypeChecker,
        FirProjectionsOnNonClassTypeArgumentChecker,
        FirUpperBoundViolatedChecker,
        FirTypeArgumentsNotAllowedExpressionChecker,
        FirTypeParameterInQualifiedAccessChecker,
        FirSealedClassConstructorCallChecker,
        FirUninitializedEnumChecker,
    )

    override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
        FirConventionFunctionCallChecker,
    )

    override val tryExpressionCheckers: Set<FirTryExpressionChecker> = setOf(
        FirCatchParameterChecker
    )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = setOf(
        FirValReassignmentViaBackingFieldChecker
    )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker> = setOf(
        FirExhaustiveWhenChecker
    )

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = setOf(
        FirReturnAllowedChecker
    )
}
