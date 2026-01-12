/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker.checkApplicability
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker.ifInapplicable
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker.reportInapplicabilityDiagnostic
import org.jetbrains.kotlin.fir.analysis.checkers.expression.isCaseMissedByAdditionalK1IncompatibleEnumsCheck
import org.jetbrains.kotlin.fir.analysis.checkers.expression.isCaseMissedByK1Intersector
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class FirPlatformSpecificEqualityChecker : FirSessionComponent {
    context(context: CheckerContext)
    abstract fun runApplicabilityCheck(
        expression: FirEqualityOperatorCall,
        leftType: ConeKotlinType,
        rightType: ConeKotlinType,
        checker: FirEqualityCompatibilityChecker,
    ): FirEqualityCompatibilityChecker.Applicability

    object Default : FirPlatformSpecificEqualityChecker() {
        context(context: CheckerContext)
        override fun runApplicabilityCheck(
            expression: FirEqualityOperatorCall,
            leftType: ConeKotlinType,
            rightType: ConeKotlinType,
            checker: FirEqualityCompatibilityChecker,
        ): FirEqualityCompatibilityChecker.Applicability =
            checker.checkApplicability(expression.operation, leftType.toTypeInfo(context.session), rightType.toTypeInfo(context.session))
    }
}

val FirSession.firPlatformSpecificEqualityChecker: FirPlatformSpecificEqualityChecker by FirSession.sessionComponentAccessor()
