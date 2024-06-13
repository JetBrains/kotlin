/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.*

object NativeExpressionCheckers : ExpressionCheckers() {
    override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> = setOf(
        FirNativeForwardDeclarationTypeOperatorChecker,
    )
    override val getClassCallCheckers: Set<FirGetClassCallChecker> = setOf(
        FirNativeForwardDeclarationGetClassCallChecker
    )
    override val qualifiedAccessExpressionCheckers: Set<FirQualifiedAccessExpressionChecker> = setOf(
        FirNativeForwardDeclarationReifiedChecker
    )
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(FirSuperCallWithDefaultsChecker)
}
