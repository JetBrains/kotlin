/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.delegatedPropertySourceOrThis
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.Experimentality
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalities
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isFromEnumClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirPropertyWithExplicitBackingFieldResolvedNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.resolve.tryAccessExplicitFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol

object FirOptInUsageAccessChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val sourceKind = expression.source?.kind
        if (sourceKind is KtFakeSourceElementKind.DataClassGeneratedMembers ||
            sourceKind is KtFakeSourceElementKind.PropertyFromParameter
        ) return

        if (expression.isLhsOfAssignment()) return

        val resolvedSymbol = when (expression) {
            is FirThisReceiverExpression -> (expression.calleeReference.symbol as? FirReceiverParameterSymbol)?.resolvedType?.toClassLikeSymbol()
            else -> expression.toReference(context.session)?.toResolvedBaseSymbol()
        } ?: return

        with(FirOptInUsageBaseChecker) {
            when (expression) {
                is FirVariableAssignment -> {
                    val experimentalities = resolvedSymbol.loadExperimentalities(fromSetter = true) +
                            loadExperimentalitiesFromTypeArguments(emptyList())
                    reportNotAcceptedExperimentalities(experimentalities, expression.lValue)
                }
                is FirQualifiedAccessExpression -> {
                    val experimentalities = resolvedSymbol.loadExperimentalities(fromSetter = false) +
                            loadExperimentalitiesFromTypeArguments(expression.typeArguments) +
                            loadExperimentalitiesFromExplicitField(expression)
                    val source = expression.source?.delegatedPropertySourceOrThis()
                    reportNotAcceptedExperimentalities(experimentalities, expression, source)
                }
                is FirDelegatedConstructorCall if resolvedSymbol is FirConstructorSymbol -> {
                    val experimentalities = if (resolvedSymbol.isFromEnumClass) {
                        resolvedSymbol.loadExperimentalities(fromSetter = false)
                    } else {
                        // This is done to prevent double-reporting, as class experimentalities are reported in FirOptInUsageTypeRefChecker
                        resolvedSymbol.loadExperimentalitiesFromConstructor()
                    }
                    reportNotAcceptedExperimentalities(experimentalities, expression.calleeReference)
                }
            }
        }
    }

    context(context: CheckerContext)
    fun loadExperimentalitiesFromExplicitField(expression: FirStatement): Set<Experimentality> {
        if (expression !is FirPropertyAccessExpression) return emptySet()
        val reference = expression.calleeReference as? FirPropertyWithExplicitBackingFieldResolvedNamedReference ?: return emptySet()
        val property = reference.toResolvedPropertySymbol()?.takeIf { it.hasExplicitBackingField } ?: return emptySet()
        val field = reference.tryAccessExplicitFieldSymbol(context.inlineFunctionBodyContext?.inlineFunction, context.session)
            ?: return emptySet()

        return when (property.backingFieldSymbol) {
            field -> field.loadExperimentalities(fromSetter = false)
            else -> emptySet()
        }
    }
}
