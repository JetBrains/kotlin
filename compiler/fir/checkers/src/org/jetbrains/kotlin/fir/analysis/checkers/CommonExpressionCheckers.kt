/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.checkers.syntax.*

object CommonExpressionCheckers : ExpressionCheckers() {
    override val annotationCallCheckers: Set<FirAnnotationCallChecker> = [
        FirAnnotationExpressionChecker,
        FirOptInAnnotationCallChecker,
    ]

    override val annotationCheckers: Set<FirAnnotationChecker> = [
        FirDslMarkerUseSiteChecker,
    ]

    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = [
        FirUnderscoreChecker,
        FirExpressionAnnotationChecker,
        FirDeprecationChecker,
        FirRecursiveProblemChecker,
        FirOptInUsageAccessChecker,
        FirPrefixAndSuffixSyntaxChecker,
        FirAnnotatedBinaryExpressionChecker,
        FirExpressionWithErrorTypeChecker,
        FirInlineBodyResolvableExpressionChecker,
        ArrayEqualityCanBeReplacedWithContentEquals,
    ]

    override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = [
        FirThrowExpressionTypeChecker,
    ]

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = [
        FirSuperReferenceChecker,
        FirSuperclassNotAccessibleFromInterfaceChecker,
        FirAbstractSuperCallChecker,
        FirProjectionsOnNonClassTypeArgumentChecker,
        FirDataClassCopyUsageWillBecomeInaccessibleChecker,
        FirIncompatibleProjectionsOnTypeArgumentChecker,
        FirUpperBoundViolatedQualifiedAccessExpressionChecker,
        FirTypeArgumentsNotAllowedExpressionChecker,
        FirTypeParameterInQualifiedAccessChecker,
        FirSealedClassConstructorCallChecker,
        FirUninitializedEnumChecker,
        FirReifiedChecker,
        FirSuspendCallChecker,
        FirLateinitIntrinsicApplicabilityChecker,
        FirLargeArityFunctionCallableReferenceChecker,
        FirAbstractClassInstantiationChecker,
        FirIncompatibleClassExpressionChecker,
        FirMissingDependencyClassChecker,
        FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker,
        FirArrayOfNothingQualifiedChecker,
        FirPrivateToThisAccessChecker,
        FirContextParameterInCalledSignatureChecker,
        FirInlineExposedLessVisibleTypeQualifiedAccessChecker,
    ]

    override val callCheckers: Set<FirCallChecker> = [
        FirNamedVarargChecker,
    ]

    override val functionCallCheckers: Set<FirFunctionCallChecker> = [
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.FunctionCall,
        FirConventionFunctionCallChecker,
        FirDivisionByZeroChecker,
        FirTrimMarginBlankPrefixChecker,
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
        FirVarargWithNonTrivialUpperBoundInferredToNothingChecker,
        PlatformClassMappedToKotlinConstructorCallChecker,
        RedundantCallOfConversionMethodChecker,
        FirImplicitPropertyTypeMakesBehaviorOrderDependantChecker,
    ]

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = [
        FirPropertyAccessTypeArgumentsChecker,
        FirCustomEnumEntriesMigrationAccessChecker,
    ]

    override val tryExpressionCheckers: Set<FirTryExpressionChecker> = [
        FirCatchParameterChecker,
    ]

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = [
        FirReassignmentAndInvisibleSetterChecker,
        FirAssignmentTypeMismatchChecker,
        FirInlineBodyVariableAssignmentChecker,
        FirParenthesizedLhsVariableAssignmentChecker,
    ]

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker> = [
        FirExhaustiveWhenChecker,
        FirWhenConditionChecker,
        FirWhenSubjectChecker,
        FirCommaInWhenConditionChecker,
        FirConfusingWhenBranchSyntaxChecker,
        FirWhenGuardChecker,
        FirWhenReturnTypeChecker,
    ]

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker> = [
        FirLoopConditionChecker,
        FirForLoopStatementAssignmentChecker,
    ]

    override val loopJumpCheckers: Set<FirLoopJumpChecker> = [
        FirBreakOrContinueJumpsAcrossFunctionBoundaryChecker
    ]

    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker> = [
        FirLogicExpressionTypeChecker,
    ]

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = [
        FirReturnSyntaxAndLabelChecker,
        FirFunctionReturnTypeMismatchChecker,
    ]

    override val blockCheckers: Set<FirBlockChecker> = [
        FirForLoopChecker,
        FirConflictsExpressionChecker,
        FirSingleNamedFunctionChecker,
    ]

    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = [
        FirNotNullAssertionChecker,
    ]

    override val elvisExpressionCheckers: Set<FirElvisExpressionChecker> = [
        FirUselessElvisChecker,
    ]

    override val getClassCallCheckers: Set<FirGetClassCallChecker> = [
        FirClassLiteralChecker,
        FirArrayOfNothingClassLiteralChecker,
    ]

    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = [
        FirUnnecessarySafeCallChecker,
    ]

    override val smartCastExpressionCheckers: Set<FirSmartCastExpressionChecker> = [
        FirDeprecatedSmartCastChecker
    ]

    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = [
        FirCastOperatorsChecker,
        FirContextSensitiveResolutionAmbiguityCheckerForTypeOperators,
    ]

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = [
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.ResolvedQualifier,
        FirStandaloneQualifierChecker,
        FirPackageOnLhsQualifierChecker,
        FirOptInUsageQualifierChecker,
        FirDeprecatedQualifierChecker,
        FirVisibilityQualifierChecker,
        FirInlineBodyResolvedQualifierChecker,
        FirCustomEnumEntriesMigrationQualifierChecker,
        FirQualifierWithTypeArgumentsChecker,
        FirRootIdePackageDeprecatedInCliQualifierChecker,
        FirParenthesizedPackageQualifierChecker,
    ]

    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = [
        FirEqualityCompatibilityChecker,
        FirContextSensitiveResolutionAmbiguityCheckerForEqualities,
    ]

    override val collectionLiteralCheckers: Set<FirCollectionLiteralChecker> = [
        FirUnsupportedArrayLiteralChecker
    ]

    override val inaccessibleReceiverCheckers: Set<FirInaccessibleReceiverChecker> = [
        FirReceiverAccessBeforeSuperCallChecker,
    ]

    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = [
        FirCallableReferenceChecker,
        FirKotlinActualAnnotationHasNoEffectInKotlinExpressionChecker.CallableReference,
        FirTypeInLhsOfCallableReferenceChecker,
        FirCustomEnumEntriesMigrationReferenceChecker,
    ]

    override val stringConcatenationCallCheckers: Set<FirStringConcatenationCallChecker> = [
        FirMultiDollarInterpolationCheckerConcatenation,
    ]

    override val literalExpressionCheckers: Set<FirLiteralExpressionChecker> = [
        FirMultiDollarInterpolationCheckerLiteral,
    ]

    override val thisReceiverExpressionCheckers: Set<FirThisReceiverExpressionChecker> = [
        FirInlineExposedLessVisibleThisReceiverChecker
    ]
}
