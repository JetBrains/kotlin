/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCastOperatorsChecker
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificCastChecker : FirSessionComponent {
    context(context: CheckerContext)
    abstract fun runApplicabilityCheck(
        expression: FirTypeOperatorCall,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        checker: FirCastOperatorsChecker,
    ): FirCastOperatorsChecker.Applicability

    object Default : FirPlatformSpecificCastChecker() {
        context(context: CheckerContext)
        override fun runApplicabilityCheck(
            expression: FirTypeOperatorCall,
            fromType: ConeKotlinType,
            toType: ConeKotlinType,
            checker: FirCastOperatorsChecker,
        ): FirCastOperatorsChecker.Applicability =
            checker.checkGeneralApplicability(expression, fromType.toTypeInfo(context.session), toType.toTypeInfo(context.session))
    }
}

val FirSession.firPlatformSpecificCastChecker: FirPlatformSpecificCastChecker
        by FirSession.sessionComponentAccessorWithDefault(FirPlatformSpecificCastChecker.Default)
