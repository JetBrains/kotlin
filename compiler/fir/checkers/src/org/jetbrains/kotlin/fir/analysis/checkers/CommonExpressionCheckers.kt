/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker> = setOf(
        FirAnnotationExpressionChecker,
        FirOptInAnnotationCallChecker,
    )

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = setOf(
        FirUnderscoreChecker,
        FirExpressionAnnotationChecker,
        FirDeprecationChecker,
        FirRecursiveProblemChecker,
        FirOptInUsageAccessChecker,
        FirPrefixAndSuffixSyntaxChecker,
        FirAnnotatedBinaryExpressionChecker,
        FirExpressionWithErrorTypeChecker,
        FirInlineBodyResolvableExpressionChecker,
    )

    override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = setOf(
        FirThrowExpressionTypeChecker,
    )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = setOf(
        FirCallableReferenceChecker,
        FirSuperReferenceChecker,
        FirSuperclassNotAccessibleFromInterfaceChecker,
        FirAbstractSuperCallChecker,
        FirProjectionsOnNonClassTypeArgumentChecker,
        FirDataClassCopyUsageWillBecomeInaccessibleChecker,
        FirIncompatibleProjectionsOnTypeArgumentChecker,
        FirUpperBoundViolatedExpressionChecker,
        FirTypeArgumentsNotAllowedExpressionChecker,
        FirTypeParameterInQualifiedAccessChecker,
        FirSealedClassConstructorCallChecker,
        FirUninitializedEnumChecker,
        FirReifiedChecker,
        FirSuspendCallChecker,
        FirLateinitIntrinsicApplicabilityChecker,
        FirAbstractClassInstantiationChecker,
        FirIncompatibleClassExpressionChecker,
        FirMissingDependencyClassChecker,
        FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker,
        FirArrayOfNothingQualifierChecker,
        FirPrivateToThisAccessChecker,
        FirContextParameterInCalledSignatureChecker,
        FirInlineExposedLessVisibleTypeQualifierAccessChecker,
    )

    override val callCheckers: Set<FirCallChecker> = setOf(
        FirNamedVarargChecker,
    )

    override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.FunctionCall,
        FirConventionFunctionCallChecker,
        FirDivisionByZeroChecker,
        FirConstructorCallChecker,
        FirSpreadOfNullableChecker,
        FirAssignmentOperatorCallChecker,
        FirUnderscoredTypeArgumentSyntaxChecker,
        FirContractNotFirstStatementChecker,
        FirProtectedConstructorNotInSuperCallChecker,
        FirOptionalExpectationExpressionChecker,
        FirParenthesizedLhsSetOperatorChecker,
        FirCommonAtomicReferenceToPrimitiveCallChecker,
        FirCommonAtomicArrayToPrimitiveCallChecker,
        FirGenericQualifierOnConstructorCallChecker,
    )

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOf(
        FirPropertyAccessTypeArgumentsChecker,
        FirCustomEnumEntriesMigrationAccessChecker,
    )

    override val tryExpressionCheckers: Set<FirTryExpressionChecker> = setOf(
        FirCatchParameterChecker,
    )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = setOf(
        FirReassignmentAndInvisibleSetterChecker,
        FirAssignmentTypeMismatchChecker,
        FirInlineBodyVariableAssignmentChecker,
        FirParenthesizedLhsVariableAssignmentChecker,
    )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker> = setOf(
        FirExhaustiveWhenChecker,
        FirWhenConditionChecker,
        FirWhenSubjectChecker,
        FirCommaInWhenConditionChecker,
        FirConfusingWhenBranchSyntaxChecker,
        FirWhenGuardChecker,
        FirWhenReturnTypeChecker,
    )

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker> = setOf(
        FirLoopConditionChecker,
        FirForLoopStatementAssignmentChecker,
    )

    override val loopJumpCheckers: Set<FirLoopJumpChecker> = setOf(
        FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker
    )

    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker> = setOf(
        FirLogicExpressionTypeChecker,
    )

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = setOf(
        FirReturnSyntaxAndLabelChecker,
        FirFunctionReturnTypeMismatchChecker,
    )

    override val blockCheckers: Set<FirBlockChecker> = setOf(
        FirForLoopChecker,
        FirConflictsExpressionChecker,
        FirSingleNamedFunctionChecker,
    )

    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = setOf(
        FirNotNullAssertionChecker,
    )

    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker> = setOf(
        FirUselessElvisChecker,
    )

    override val getClassCallCheckers: Set<FirGetClassCallChecker> = setOf(
        FirClassLiteralChecker,
    )

    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = setOf(
        FirUnnecessarySafeCallChecker,
    )

    override val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker> = setOf(
        FirDeprecatedSmartCastChecker
    )

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = setOf(
        FirCastOperatorsChecker,
    )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = setOf(
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.ResolvedQualifier,
        FirStandaloneQualifierChecker,
        FirPackageOnLhsQualifierChecker,
        FirOptInUsageQualifierChecker,
        FirDeprecatedQualifierChecker,
        FirVisibilityQualifierChecker,
        FirInlineBodyResolvedQualifierChecker,
        FirCustomEnumEntriesMigrationQualifierChecker,
        FirQualifierWithTypeArgumentsChecker,
    )

    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = setOf(
        FirEqualityCompatibilityChecker,
    )

    override val arrayLiteralCheckers: Set<FirArrayLiteralChecker> = setOf(
        FirUnsupportedArrayLiteralChecker
    )

    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> = setOf(
        FirReceiverAccessBeforeSuperCallChecker,
    )

    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = setOf(
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.CallableReference,
        FirTypeArgumentsOfQualifierOfCallableReferenceChecker,
        FirCustomEnumEntriesMigrationReferenceChecker,
    )

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = setOf(
        FirMultiDollarInterpolationCheckerConcatenation,
    )

    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker> = setOf(
        FirMultiDollarInterpolationCheckerLiteral,
    )
}
