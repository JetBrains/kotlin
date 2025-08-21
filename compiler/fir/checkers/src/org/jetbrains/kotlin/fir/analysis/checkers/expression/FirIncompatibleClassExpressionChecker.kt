/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object FirIncompatibleClassExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return

        checkType(symbol.resolvedReturnType, expression)
        for (parameter in symbol.contextParameterSymbols) {
            checkType(parameter.resolvedReturnType, expression)
        }
        checkType(symbol.resolvedReceiverType, expression)
        if (symbol is FirFunctionSymbol) {
            for (parameter in symbol.valueParameterSymbols) {
                checkType(parameter.resolvedReturnTypeRef.coneType, expression)
            }
        }

        checkSourceElement(symbol.containerSource, expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkType(type: ConeKotlinType?, element: FirElement) {
        val classSymbol = type?.toRegularClassSymbol(context.session)
        checkSourceElement(classSymbol?.sourceElement, element)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSourceElement(
        source: SourceElement?,
        element: FirElement,
    ) {
        if (source !is DeserializedContainerSource) return

        val incompatibility = source.incompatibility
        if (incompatibility != null) {
            reporter.reportOn(element.source, FirErrors.INCOMPATIBLE_CLASS, source.presentableString, incompatibility)
        }
        if (source.preReleaseInfo.isInvisible) {
            reporter.reportOn(
                element.source,
                FirErrors.PRE_RELEASE_CLASS,
                source.presentableString,
                source.preReleaseInfo.poisoningFeatures
            )

        }
        if (source.abiStability == DeserializedContainerAbiStability.UNSTABLE) {
            reporter.reportOn(element.source, FirErrors.IR_WITH_UNSTABLE_ABI_COMPILED_CLASS, source.presentableString)
        }
    }
}
