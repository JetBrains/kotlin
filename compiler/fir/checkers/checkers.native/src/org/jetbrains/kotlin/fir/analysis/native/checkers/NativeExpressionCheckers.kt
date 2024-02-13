/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirSuperCallWithDefaultsChecker

object NativeExpressionCheckers : ExpressionCheckers() {
    override val typeOperatorCallCheckers = setOf(
        FirNativeForwardDeclarationTypeOperatorChecker,
    )
    override val getClassCallCheckers = setOf(
        FirNativeForwardDeclarationGetClassCallChecker
    )
    override val qualifiedAccessExpressionCheckers = setOf(
        FirNativeForwardDeclarationReifiedChecker
    )
    override val functionCallCheckers: Set<FirFunctionCallChecker>
        get() = setOf(FirSuperCallWithDefaultsChecker)
}