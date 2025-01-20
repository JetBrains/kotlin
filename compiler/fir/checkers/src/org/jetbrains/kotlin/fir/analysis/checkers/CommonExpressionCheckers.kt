/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirUnsafeDowncastWrtVarianceChecker

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
            FirPrefixAndSuffixSyntaxChecker,
            FirExpressionWithErrorTypeChecker,
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
            FirDataClassCopyUsageWillBecomeInaccessibleChecker,
            FirIncompatibleProjectionsOnTypeArgumentChecker,
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
            FirInlineBodyQualifiedAccessExpressionChecker,
            FirIncompatibleClassExpressionChecker,
            FirMissingDependencyClassChecker,
            FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker,
            FirArrayOfNothingQualifierChecker,
            FirPrivateToThisAccessChecker,
        )

    override val callCheckers: Set<FirCallChecker>
        get() = setOf(
            FirNamedVarargChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.FunctionCall,
            FirConventionFunctionCallChecker,
            FirDivisionByZeroChecker,
            FirConstructorCallChecker,
            FirSpreadOfNullableChecker,
            FirAssignmentOperatorCallChecker,
            FirNamedVarargChecker,
            FirUnderscoredTypeArgumentSyntaxChecker,
            FirContractNotFirstStatementChecker,
            FirProtectedConstructorNotInSuperCallChecker,
            FirOptionalExpectationExpressionChecker,
            FirParenthesizedLhsSetOperatorChecker,
            FirCommonAtomicReferenceToPrimitiveCallChecker,
            FirGenericQualifierOnConstructorCallChecker,
        )

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
        get() = setOf(
            FirPropertyAccessTypeArgumentsChecker,
            FirCustomEnumEntriesMigrationAccessChecker,
        )

    override val tryExpressionCheckers: Set<FirTryExpressionChecker>
        get() = setOf(
            FirCatchParameterChecker
        )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = setOf(
            FirReassignmentAndInvisibleSetterChecker,
            FirAssignmentTypeMismatchChecker,
            FirInlineBodyVariableAssignmentChecker,
            FirParenthesizedLhsVariableAssignmentChecker,
        )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = setOf(
            FirExhaustiveWhenChecker,
            FirWhenConditionChecker,
            FirWhenSubjectChecker,
            FirCommaInWhenConditionChecker,
            FirConfusingWhenBranchSyntaxChecker,
            FirWhenGuardChecker,
        )

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker>
        get() = setOf(
            FirLoopConditionChecker,
            FirForLoopStatementAssignmentChecker
        )

    override val loopJumpCheckers: Set<FirLoopJumpChecker>
        get() = setOf(
            FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker
        )

    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker>
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
            FirConflictsExpressionChecker,
            FirSingleNamedFunctionChecker,
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

    override val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker>
        get() = setOf(
            FirDeprecatedSmartCastChecker
        )

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker>
        get() = setOf(
            FirUnsafeDowncastWrtVarianceChecker,
            FirCastOperatorsChecker,
        )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker>
        get() = setOf(
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

    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker>
        get() = setOf(
            FirEqualityCompatibilityChecker,
        )

    override val arrayLiteralCheckers: Set<FirArrayLiteralChecker>
        get() = setOf(
            FirUnsupportedArrayLiteralChecker
        )

    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker>
        get() = setOf(
            FirReceiverAccessBeforeSuperCallChecker,
        )

    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker>
        get() = setOf(
            FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.CallableReference,
            FirTypeArgumentsOfQualifierOfCallableReferenceChecker,
            FirCustomEnumEntriesMigrationReferenceChecker,
        )

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker>
        get() = setOf(
            FirMultiDollarInterpolationCheckerConcatenation,
        )

    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker>
        get() = setOf(
            FirMultiDollarInterpolationCheckerLiteral,
        )
}
