/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isLhsOfAssignment
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isFromEnumClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.resolvedType

object FirOptInUsageAccessChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        val sourceKind = expression.source?.kind
        if (sourceKind is KtFakeSourceElementKind.DataClassGeneratedMembers ||
            sourceKind is KtFakeSourceElementKind.PropertyFromParameter
        ) return

        if (expression.isLhsOfAssignment(context)) return

        val resolvedSymbol = expression.toReference(context.session)?.toResolvedBaseSymbol() ?: return

        with(FirOptInUsageBaseChecker) {
            when {
                expression is FirVariableAssignment -> {
                    val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = true, null) +
                            loadExperimentalitiesFromTypeArguments(context, emptyList())
                    reportNotAcceptedExperimentalities(experimentalities, expression.lValue, context, reporter)
                }

                expression is FirQualifiedAccessExpression -> {
                    val dispatchReceiverType = expression.dispatchReceiver?.resolvedType?.fullyExpandedType(context.session)

                    val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType) +
                            loadExperimentalitiesFromTypeArguments(context, expression.typeArguments)
                    val source = if (expression.source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor) {
                        val property = context.containingDeclarations.lastOrNull { it is FirProperty } as? FirProperty ?: return
                        property.delegate?.source?.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor) ?: return
                    } else {
                        expression.source
                    }
                    reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter, source)
                }
                expression is FirDelegatedConstructorCall && resolvedSymbol is FirConstructorSymbol && resolvedSymbol.isFromEnumClass -> {
                    val experimentalities = resolvedSymbol.loadExperimentalities(context, fromSetter = false, null)
                    reportNotAcceptedExperimentalities(experimentalities, expression.calleeReference, context, reporter)
                }
            }
        }
    }
}
