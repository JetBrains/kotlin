/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.*

object JvmExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker> = setOf(
        FirJvmProtectedInSuperClassCompanionCallChecker,
        FirJvmReflectionApiCallChecker,
        FirJvmMissingBuiltInDeclarationChecker
    )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = setOf(
        FirInterfaceDefaultMethodCallChecker,
        FirQualifiedAccessJavaNullabilityWarningChecker,
        FirJvmModuleAccessibilityQualifiedAccessChecker,
        FirJvmInlineTargetQualifiedAccessChecker,
        FirJavaClassInheritsKtPrivateClassExpressionChecker,
        FirArrayOfNullableNothingQualifiedChecker,
    )

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker> = setOf(
        FirSyntheticPropertyWithoutJavaOriginChecker,
        FirFieldAccessShadowedByInvisibleKotlinProperty,
        FirJavaClassOnCompanionChecker,
    )

    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> = setOf(
        FirUnsupportedSyntheticCallableReferenceChecker,
        FirFieldReferenceShadowedByInvisibleKotlinProperty,
        FirJavaSamInterfaceConstructorReferenceChecker,
        FirJavaClassPropertyReferenceChecker,
    )

    override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(
        FirJavaGenericVarianceViolationTypeChecker,
        FirSuperCallWithDefaultsChecker,
        FirJvmSuspensionPointInsideMutexLockChecker,
        FirJvmIdentitySensitiveCallWithValueTypeObjectChecker,
        FirJvmInconsistentOperatorFromJavaCallChecker,
        FirJvmPolymorphicSignatureCallChecker,
        FirJvmAtomicReferenceToPrimitiveCallChecker,
        FirJvmAtomicReferenceArrayToPrimitiveCallChecker,
        FirJavaSamConstructorNullabilityChecker,
        FirJavaAnnotationsConstructorCallChecker,
    )

    override val annotationCheckers: Set<FirAnnotationChecker> = setOf(
        FirJavaAnnotationsChecker,
        FirJvmPackageNameAnnotationsChecker,
        FirJvmSerializableLambdaChecker,
    )

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker> = setOf(
        FirLoopConditionJavaNullabilityWarningChecker,
    )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker> = setOf(
        FirWhenConditionJavaNullabilityWarningChecker,
        FirJavaWhenExhaustivenessWarningChecker,
    )

    override val booleanOperatorExpressionCheckers: Set<FirBooleanOperatorExpressionChecker> = setOf(
        FirLogicExpressionTypeJavaNullabilityWarningChecker,
    )

    override val throwExpressionCheckers: Set<FirThrowExpressionChecker> = setOf(
        FirThrowJavaNullabilityWarningChecker,
    )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker> = setOf(
        FirAssignmentJavaNullabilityWarningChecker,
    )

    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker> = setOf(
        FirJavaUnnecessarySafeCallChecker,
    )

    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker> = setOf(
        FirJavaUnnecessaryNotNullChecker,
    )

    override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> = setOf(
        FirJvmModuleAccessibilityResolvedQualifierChecker,
    )

    override val returnExpressionCheckers: Set<FirReturnExpressionChecker> = setOf(
        FirReturnJavaNullabilityWarningChecker,
    )

    override val equalityOperatorCallCheckers: Set<FirEqualityOperatorCallChecker> = setOf(
        FirJvmIdentityEqualsOnJavaValueBasedClass,
    )

    override val getClassCallCheckers: Set<FirGetClassCallChecker> = setOf(
        FirArrayOfNullableNothingClassLiteralChecker,
    )
}
