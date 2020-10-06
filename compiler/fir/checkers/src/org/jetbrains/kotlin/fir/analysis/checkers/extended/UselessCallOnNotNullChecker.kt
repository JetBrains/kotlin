/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

object UselessCallOnNotNullChecker : FirQualifiedAccessChecker() {
    // todo: add 'call may be reduced' in cases like 's?.isNullOrEmpty()' where 's: String? = ""'
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val method = expression.getCallableId() ?: return
        val calleeOn = expression.explicitReceiver ?: return
        val calleePackageName = calleeOn.getPackage()
        val calleeName = method.callableName.asString()
        if ("$calleePackageName.$calleeName" !in triggerOn) return

        if (calleeOn.getNullability() == ConeNullability.NOT_NULL) {
            reporter.report(expression.source, FirErrors.USELESS_CALL_ON_NOT_NULL)
        }
    }

    private fun FirQualifiedAccessExpression.getCallableId() =
        ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)?.callableId

    private fun FirExpression.getPackage() =
        (typeRef as? FirResolvedTypeRef)?.coneType?.classId?.packageFqName.toString()

    private fun FirExpression.getNullability() = (typeRef as FirResolvedTypeRef).type.nullability


    private val triggerOn = setOf(
        "kotlin.collections.orEmpty",
        "kotlin.sequences.orEmpty",
        "kotlin.text.orEmpty",
        "kotlin.text.isNullOrEmpty",
        "kotlin.text.isNullOrEmpty",
        "kotlin.text.isNullOrBlank",
        "kotlin.isNullOrBlank",
        "kotlin.isNullOrEmpty",
        "kotlin.orEmpty"
    )

}