/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.FirDeprecatedJavaAnnotationsChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.FirJavaGenericVarianceViolationTypeChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.FirJvmPackageNameAnnotationsChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.FirSuperCallWithDefaultsChecker

object JvmExpressionCheckers : ExpressionCheckers() {
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(
            FirJavaGenericVarianceViolationTypeChecker,
            FirSuperCallWithDefaultsChecker,
        )

    override val annotationCallCheckers: Set<FirAnnotationCallChecker>
        get() = setOf(
            FirDeprecatedJavaAnnotationsChecker,
            FirJvmPackageNameAnnotationsChecker,
        )
}
