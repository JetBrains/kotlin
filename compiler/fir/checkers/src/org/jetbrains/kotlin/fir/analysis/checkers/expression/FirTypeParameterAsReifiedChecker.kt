/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory1
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds

object FirTypeParameterAsReifiedChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleReference = expression.calleeReference
        val typeArguments = expression.typeArguments
        val typeParameters = calleReference.toResolvedCallableSymbol()?.typeParameterSymbols ?: return

        val count = minOf(typeArguments.size, typeParameters.size)
        for (index in 0 until count) {
            val typeArgumentProjection = typeArguments.elementAt(index)
            val source = typeArgumentProjection.source ?: calleReference.source
            val typeArgument = typeArgumentProjection.toConeTypeProjection().type
            val typeParameter = typeParameters[index]

            if (source != null && (typeParameter.isTypeParameterOfKotlinArray())) {
                checkArgumentAndReport(typeArgument, source, false, context, reporter)
            }
        }
    }

    private fun FirTypeParameterSymbol.isTypeParameterOfKotlinArray(): Boolean {
        val containingDeclaration = containingDeclarationSymbol
        return isReified ||
                containingDeclaration is FirRegularClassSymbol && containingDeclaration.classId == StandardClassIds.Array
    }

    private fun checkArgumentAndReport(
        typeArgument: ConeKotlinType?,
        source: FirSourceElement,
        isArray: Boolean,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (typeArgument?.classId == StandardClassIds.Array) {
            checkArgumentAndReport(typeArgument.typeArguments[0].type, source, true, context, reporter)
            return
        }

        var factory: FirDiagnosticFactory1<FirTypeParameterSymbol>? = null

        lateinit var symbol: FirTypeParameterSymbol
        if (typeArgument is ConeTypeParameterType) {
            factory = if (isArray) {
                if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNonReifiedArraysAsReifiedTypeArguments))
                    FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY else
                    FirErrors.TYPE_PARAMETER_AS_REIFIED_ARRAY_WARNING
            } else {
                FirErrors.TYPE_PARAMETER_AS_REIFIED
            }
            symbol = typeArgument.toSymbol(context.session) as FirTypeParameterSymbol
        }

        if (factory != null && !symbol.isReified) {
            reporter.reportOn(source, factory, symbol, context)
        }
    }
}