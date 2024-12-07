/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

object UselessCallOnNotNullChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    // todo, KT-59829: add 'call may be reduced' in cases like 's?.isNullOrEmpty()' where 's: String? = ""'
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val method = expression.getCallableId() ?: return
        if (method !in triggerOn) return
        val calleeOn = expression.explicitReceiver ?: return
        if (!calleeOn.resolvedType.canBeNull(context.session)) {
            reporter.reportOn(expression.source, FirErrors.USELESS_CALL_ON_NOT_NULL, context)
        }
    }

    private fun FirQualifiedAccessExpression.getCallableId(): CallableId? {
        return calleeReference.toResolvedNamedFunctionSymbol()?.callableId
    }

    private val triggerOn = setOf(
        CallableId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("orEmpty")),
        CallableId(StandardClassIds.BASE_SEQUENCES_PACKAGE, Name.identifier("orEmpty")),
        CallableId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("orEmpty")),
        CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("orEmpty")),
        CallableId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("isNullOrBlank")),
        CallableId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("isNullOrEmpty")),
        CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("isNullOrBlank")),
        CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("isNullOrEmpty")),
    )

}
