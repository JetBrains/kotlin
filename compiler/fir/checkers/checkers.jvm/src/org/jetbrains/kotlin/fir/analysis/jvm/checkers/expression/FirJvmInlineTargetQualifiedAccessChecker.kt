/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement

object FirJvmInlineTargetQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val callableSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.origin.fromSource) return

        val isInline = when (callableSymbol) {
            is FirFunctionSymbol<*> -> callableSymbol.isInline
            is FirPropertySymbol -> {
                val accessor = if (expression.isLhsOfAssignment(context)) callableSymbol.setterSymbol else callableSymbol.getterSymbol
                accessor != null && accessor.isInline
            }
            else -> false
        }

        if (isInline) {
            checkInlineTargetVersion(callableSymbol, context, reporter, expression)
        }
    }

    private fun checkInlineTargetVersion(
        callableSymbol: FirCallableSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        element: FirElement,
    ) {
        val currentJvmTarget = context.session.jvmTargetProvider?.jvmTarget ?: return

        val containingClass = callableSymbol.containingClassLookupTag()
        val binaryClass = if (containingClass != null) {
            val containingClassSymbol = containingClass.toFirRegularClassSymbol(context.session) ?: return

            @OptIn(SymbolInternals::class)
            val sourceElement = containingClassSymbol.fir.sourceElement as? KotlinJvmBinarySourceElement ?: return
            sourceElement.binaryClass
        } else {
            val containerSource = callableSymbol.containerSource as? JvmPackagePartSource ?: return
            containerSource.knownJvmBinaryClass
        }

        val inlinedVersion = (binaryClass as? FileBasedKotlinClass)?.classVersion ?: return
        val currentVersion = currentJvmTarget.majorVersion

        if (currentVersion < inlinedVersion) {
            reporter.reportOn(
                element.toReference(context.session)?.source ?: element.source,
                FirJvmErrors.INLINE_FROM_HIGHER_PLATFORM,
                JvmTarget.getDescription(inlinedVersion),
                JvmTarget.getDescription(currentVersion),
                context,
            )
        }
    }
}
