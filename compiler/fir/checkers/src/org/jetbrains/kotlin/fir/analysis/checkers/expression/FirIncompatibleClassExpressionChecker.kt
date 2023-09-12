/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object FirIncompatibleClassExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return

        checkType(symbol.resolvedReturnType, expression, context, reporter)
        checkType(symbol.receiverParameter?.typeRef?.coneType, expression, context, reporter)
        if (symbol is FirFunctionSymbol) {
            for (parameter in symbol.valueParameterSymbols) {
                checkType(parameter.resolvedReturnTypeRef.type, expression, context, reporter)
            }
        }

        @OptIn(SymbolInternals::class)
        checkSourceElement(symbol.fir.containerSource, expression, context, reporter)
    }

    internal fun checkType(type: ConeKotlinType?, element: FirElement, context: CheckerContext, reporter: DiagnosticReporter) {
        val classSymbol = type?.toRegularClassSymbol(context.session)
        checkSourceElement(classSymbol?.sourceElement, element, context, reporter)
    }

    private fun checkSourceElement(source: SourceElement?, element: FirElement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (source !is DeserializedContainerSource) return

        val incompatibility = source.incompatibility
        if (incompatibility != null) {
            reporter.reportOn(element.source, FirErrors.INCOMPATIBLE_CLASS, source.presentableString, incompatibility, context)
        }
        if (source.isPreReleaseInvisible) {
            reporter.reportOn(element.source, FirErrors.PRE_RELEASE_CLASS, source.presentableString, context)
        }
    }
}
