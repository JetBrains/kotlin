/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirDelegateUsesExtensionPropertyTypeParameterChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegate = declaration.delegate.safeAs<FirFunctionCall>() ?: return
        val parameters = declaration.typeParameters.mapTo(hashSetOf()) { it.symbol }

        val shouldReportError = delegate.typeRef.coneType.containsTypeParameterFrom(parameters, delegate, context, reporter)

        if (shouldReportError) {
            val diagnostic = getProperDiagnostic(context)
            reporter.reportOn(declaration.source, diagnostic, context)
        }
    }

    private fun getProperDiagnostic(context: CheckerContext): FirDiagnosticFactory0 {
        val reportAsError = context.session.languageVersionSettings.supportsFeature(
            LanguageFeature.ForbidUsingExtensionPropertyTypeParameterInDelegate
        )

        return if (reportAsError) {
            FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER
        } else {
            FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_WARNING
        }
    }

    private fun ConeKotlinType.containsTypeParameterFrom(
        parameters: HashSet<FirTypeParameterSymbol>,
        delegate: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ): Boolean {
        for (it in typeArguments) {
            val theType = it.type ?: continue
            val symbol = theType.toSymbol(context.session)

            if (
                symbol in parameters ||
                theType.containsTypeParameterFrom(parameters, delegate, context, reporter)
            ) {
                return true
            }
        }

        return false
    }
}
