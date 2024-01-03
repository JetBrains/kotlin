/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.*

object JvmExpressionCheckers : ExpressionCheckers() {
    override val basicExpressionCheckers: Set<FirBasicExpressionChecker>
        get() = setOf(
            FirJvmProtectedInSuperClassCompanionCallChecker,
            FirJvmReflectionApiCallChecker,
        )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = setOf(
            FirInterfaceDefaultMethodCallChecker,
            FirJavaSamInterfaceConstructorReferenceChecker,
            FirQualifiedAccessJavaNullabilityWarningChecker,
        )

    override val propertyAccessExpressionCheckers: Set<FirPropertyAccessExpressionChecker>
        get() = setOf(
            FirSyntheticPropertyWithoutJavaOriginChecker,
        )

    override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker>
        get() = setOf(
            FirUnsupportedSyntheticCallableReferenceChecker,
        )

    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirJavaGenericVarianceViolationTypeChecker,
            FirSuperCallWithDefaultsChecker,
            FirJvmSuspensionPointInsideMutexLockChecker,
            FirJvmInconsistentOperatorFromJavaCallChecker,
            FirJvmPolymorphicSignatureCallChecker
        )

    override val annotationCheckers: Set<FirAnnotationChecker>
        get() = setOf(
            FirJavaAnnotationsChecker,
            FirJvmPackageNameAnnotationsChecker,
            FirJvmSerializableLambdaChecker,
        )

    override val loopExpressionCheckers: Set<FirLoopExpressionChecker>
        get() = setOf(
            FirLoopConditionJavaNullabilityWarningChecker,
        )

    override val whenExpressionCheckers: Set<FirWhenExpressionChecker>
        get() = setOf(
            FirWhenConditionJavaNullabilityWarningChecker,
        )

    override val logicExpressionCheckers: Set<FirLogicExpressionChecker>
        get() = setOf(
            FirLogicExpressionTypeJavaNullabilityWarningChecker,
        )

    override val throwExpressionCheckers: Set<FirThrowExpressionChecker>
        get() = setOf(
            FirThrowJavaNullabilityWarningChecker,
        )

    override val variableAssignmentCheckers: Set<FirVariableAssignmentChecker>
        get() = setOf(
            FirAssignmentJavaNullabilityWarningChecker,
        )

    override val safeCallExpressionCheckers: Set<FirSafeCallExpressionChecker>
        get() = setOf(
            FirJavaUnnecessarySafeCallChecker,
        )

    override val checkNotNullCallCheckers: Set<FirCheckNotNullCallChecker>
        get() = setOf(
            FirJavaUnnecessaryNotNullChecker,
        )
}
