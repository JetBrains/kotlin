/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol

object FirGenericQualifierOnConstructorCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val resolvedQualifier = expression.explicitReceiver as? FirResolvedQualifier ?: return
        if (resolvedQualifier.typeArguments.isEmpty()) return
        val constructorSymbol = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        if (constructorSymbol.getContainingClassSymbol()?.isInner != false) return

        reporter.reportOn(
            resolvedQualifier.source,
            FirErrors.GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL,
            context
        )
    }
}