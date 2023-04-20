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
            FirJvmProtectedInSuperClassCompanionCallChecker
        )

    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker>
        get() = setOf(
            FirInterfaceDefaultMethodCallChecker,
            FirJavaSamInterfaceConstructorReferenceChecker,
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
}
