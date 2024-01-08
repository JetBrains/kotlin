/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableReference
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol

/**
 * This is K2 implementation.
 * For K1 implementation see: [org.jetbrains.kotlin.resolve.jvm.checkers.UnsupportedSyntheticCallableReferenceChecker]
 */
object FirUnsupportedSyntheticCallableReferenceChecker : FirExpressionChecker<FirCallableReferenceAccess>(MppCheckerKind.Common) {
    override fun check(expression: FirCallableReferenceAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val parent = context.containingElements.let {
            check(it.last() === expression)
            it[it.lastIndex - 1]
        }
        // We allow resolution of top-level callable references to synthetic Java extension properties in the delegate position. See KT-47299
        if (parent is FirProperty && parent.delegate === expression) return

        val resolvedSymbol = expression.toResolvedCallableReference()?.resolvedSymbol
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ReferencesToSyntheticJavaProperties) &&
            resolvedSymbol is FirSyntheticPropertySymbol &&
            // In K1 such properties were not considered synthetic and are called JavaPropertyDescriptor.
            // That's why we need to do an additional check in K2 checker, while in K1 we didn't need to do it
            resolvedSymbol !is FirJavaOverriddenSyntheticPropertySymbol
        ) {
            reporter.reportOn(
                expression.calleeReference.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.ReferencesToSyntheticJavaProperties to context.session.languageVersionSettings,
                context
            )
        }
    }
}
