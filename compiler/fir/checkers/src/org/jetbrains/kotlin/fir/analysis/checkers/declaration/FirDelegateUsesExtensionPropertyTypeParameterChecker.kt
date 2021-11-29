/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirDelegateUsesExtensionPropertyTypeParameterChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegate = declaration.delegate.safeAs<FirFunctionCall>() ?: return
        val parameters = declaration.typeParameters.mapTo(hashSetOf()) { it.symbol }

        val shouldReportError = delegate.typeRef.coneType.containsTypeParameterFrom(parameters, delegate, context, reporter)

        if (shouldReportError) {
            reporter.reportOn(declaration.source, FirErrors.DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER, context)
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
