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
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isFromEnumClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirOptInUsageAccessChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val sourceKind = expression.source?.kind
        if (sourceKind is KtFakeSourceElementKind.DataClassGeneratedMembers ||
            sourceKind is KtFakeSourceElementKind.PropertyFromParameter
        ) return

        if (expression.isLhsOfAssignment()) return

        val resolvedSymbol = expression.toReference(context.session)?.toResolvedBaseSymbol() ?: return

        with(FirOptInUsageBaseChecker) {
            when (expression) {
                is FirVariableAssignment -> {
                    val experimentalities = resolvedSymbol.loadExperimentalities(fromSetter = true, null) +
                            loadExperimentalitiesFromTypeArguments(emptyList())
                    reportNotAcceptedExperimentalities(experimentalities, expression.lValue)
                }
                is FirQualifiedAccessExpression -> {
                    val dispatchReceiverType = expression.dispatchReceiver?.resolvedType?.fullyExpandedType()

                    val experimentalities = resolvedSymbol.loadExperimentalities(fromSetter = false, dispatchReceiverType) +
                            loadExperimentalitiesFromTypeArguments(expression.typeArguments) +
                            loadExperimentalitiesFromExplicitField(expression, dispatchReceiverType)
                    val source = expression.source?.delegatedPropertySourceOrThis()
                    reportNotAcceptedExperimentalities(experimentalities, expression, source)
                }
                is FirDelegatedConstructorCall if resolvedSymbol is FirConstructorSymbol -> {
                    val experimentalities = if (resolvedSymbol.isFromEnumClass) {
                        resolvedSymbol.loadExperimentalities(fromSetter = false, null)
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
    fun loadExperimentalitiesFromExplicitField(expression: FirStatement, dispatchReceiver: ConeKotlinType?): Set<Experimentality> {
        if (expression !is FirPropertyAccessExpression) return emptySet()
        val property = expression.calleeReference.toResolvedPropertySymbol()?.takeIf { it.hasExplicitBackingField } ?: return emptySet()

        return when (context.secondToLastContainer) {
            is FirSmartCastExpression -> property.backingFieldSymbol?.loadExperimentalities(fromSetter = false, dispatchReceiver).orEmpty()
            else -> emptySet()
        }
    }
}
