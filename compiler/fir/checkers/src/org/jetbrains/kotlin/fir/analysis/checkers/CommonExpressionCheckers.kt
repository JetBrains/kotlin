/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirCommaInWhenConditionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirConfusingWhenBranchSyntaxChecker
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.FirUnderscoredTypeArgumentSyntaxChecker

object CommonExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            FirAnnotationExpressionChecker,
            FirOptInAnnotationCallChecker,
        )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            FirUnderscoreChecker,
            FirExpressionAnnotationChecker,
            FirDeprecationChecker,
            FirRecursiveProblemChecker,
            FirOptInUsageAccessChecker,
        )

    override val throwExpressionCheckers: Set<FirThrowExpressionChecker>
        get() = setOf(
            FirThrowExpressionTypeChecker,
        )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = setOf(
            FirCallableReferenceChecker,
            FirSuperReferenceChecker,
            FirSuperclassNotAccessibleFromInterfaceChecker,
            FirAbstractSuperCallChecker,
            FirQualifiedSupertypeExtendedByOtherSupertypeChecker,
            FirProjectionsOnNonClassTypeArgumentChecker,
            FirUpperBoundViolatedExpressionChecker,
            FirTypeArgumentsNotAllowedExpressionChecker,
            FirTypeParameterInQualifiedAccessChecker,
            FirSealedClassConstructorCallChecker,
            FirUninitializedEnumChecker,
            FirFunInterfaceConstructorReferenceChecker,
            FirReifiedChecker,
            FirSuspendCallChecker,
            FirLateinitIntrinsicApplicabilityChecker,
            FirAbstractClassInstantiationChecker,
        )

    override val callCheckers: Set<FirCallChecker>
        get() = setOf(
            FirNamedVarargChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirConventionFunctionCallChecker,
            FirDivisionByZeroChecker,
            FirConstructorCallChecker,
            FirSpreadOfNullableChecker,
            FirAssignmentOperatorCallChecker,
            FirNamedVarargChecker,
            FirUnderscoredTypeArgumentSyntaxChecker,
            FirContractNotFirstStatementChecker,
        )

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
        get() = setOf(
            FirPropertyAccessTypeArgumentsChecker,
        )

    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        get() = setOf(
            FirCatchParameterChecker
        )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = setOf(
            FirReassignmentAndInvisibleSetterChecker,
            FirAssignmentTypeMismatchChecker,
        )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = setOf(
            FirExhaustiveWhenChecker,
            FirWhenConditionChecker,
            FirWhenSubjectChecker,
            FirCommaInWhenConditionChecker,
            FirConfusingWhenBranchSyntaxChecker,
        )

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker>
        get() = setOf(
            FirLoopConditionChecker,
        )

    override val loopJumpCheckers: Set<FirLoopJumpChecker>
        get() = setOf(
            FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker
        )

    override val logicExpressionCheckers: Set<FirLogicExpressionChecker>
        get() = setOf(
            FirLogicExpressionTypeChecker,
        )

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker>
        get() = setOf(
            FirReturnSyntaxAndLabelChecker,
            FirFunctionReturnTypeMismatchChecker
        )

    override val blockCheckers: Set<FirBlockChecker>
        get() = setOf(
            FirForLoopChecker,
            FirConflictsExpressionChecker
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

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker>
        get() = setOf(
            FirUselessTypeOperationCallChecker,
            FirCastOperatorsChecker
        )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        get() = setOf(
            FirStandaloneQualifierChecker,
            FirOptInUsageQualifierChecker,
            FirDeprecatedQualifierChecker,
            FirVisibilityQualifierChecker,
        )

    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker>
        get() = setOf(
            FirEqualityCompatibilityChecker,
        )

    override val arrayOfCallCheckers: Set<FirArrayOfCallChecker>
        get() = setOf(
            FirUnsupportedArrayLiteralChecker
        )

    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker>
        get() = setOf(
            FirReceiverAccessBeforeSuperCallChecker,
        )
}
