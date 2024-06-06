/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.java.enhancement.inheritedKtPrivateCls
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object FirJavaClassInheritsKtPrivateClassExprChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {

    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitJavaClassInheritingPrivateKotlinClass))
            return

        @OptIn(SymbolInternals::class)
        val callableReference = expression.calleeReference.toResolvedCallableSymbol()?.fir ?: return
        if (callableReference.inheritedKtPrivateCls != null) {
            val javaClass = callableReference.getContainingClass()?.defaultType()!!
            reporter.reportOn(expression.source, JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS, javaClass, callableReference.inheritedKtPrivateCls!!, context)
        }
    }

}