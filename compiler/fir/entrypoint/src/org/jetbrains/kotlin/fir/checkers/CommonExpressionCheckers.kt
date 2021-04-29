/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            FirAnnotationUsedAsAnnotationArgumentChecker,
        )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            FirReservedUnderscoreExpressionChecker
        )

    override val qualifiedAccessCheckers: Set<FirQualifiedAccessChecker>
        get() = setOf(
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
            FirFunInterfaceConstructorReferenceChecker
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirConventionFunctionCallChecker,
            FirDivisionByZeroChecker,
            FirConstructorCallChecker
        )

    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        get() = setOf(
            FirCatchParameterChecker
        )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = setOf(
            FirValReassignmentViaBackingFieldChecker,
            FirAssignmentTypeMismatchChecker
        )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = setOf(
            FirExhaustiveWhenChecker
        )

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
        get() = setOf(
            FirReturnAllowedChecker,
            FirFunctionReturnTypeMismatchChecker
        )

    override val blockCheckers: Set<FirBlockChecker>
        get() = setOf(
            FirForLoopChecker
        )

    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker>
        get() = setOf(
            FirNotNullAssertionChecker,
        )

    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker>
        get() = setOf(
            FirUselessElvisChecker,
        )

    override val getClassCallCheckers: Set<FirGetClassCallChecker>
        get() = setOf(
            FirClassLiteralChecker,
        )

    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker>
        get() = setOf(
            FirUnnecessarySafeCallChecker,
        )

    override val anonymousFunctionAsExpressionCheckers: Set<FirAnonymousFunctionAsExpressionChecker>
        get() = setOf(
            FirAnonymousFunctionChecker,
        )

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker>
        get() = setOf(
            FirUselessTypeOperationCallChecker,
        )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        get() = setOf(
            FirStandaloneQualifierChecker,
        )
}
