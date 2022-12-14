/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isTypedEqualsInValueClassBySignature
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalities
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirTypedEqualsOptInChecker : FirEqualityOperatorCallChecker() {
    override fun check(expression: FirEqualityOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val arguments = expression.argumentList.arguments
        if (arguments.size != 2) return
        val lType = arguments[0].typeRef.coneType
        if (!lType.isValueClass(context.session)) return
        val rType = arguments[1].typeRef.coneType
        if (lType.classId != rType.classId) return
        @OptIn(SymbolInternals::class)
        val typedEquals = lType.toRegularClassSymbol(context.session)?.fir?.declarations?.firstOrNull {
            it is FirSimpleFunction && it.isTypedEqualsInValueClassBySignature(context.session)
        } ?: return
        val experimentalities = typedEquals.symbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType = lType)
        with(FirOptInUsageBaseChecker) {
            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }
}
