/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.toFirLightSourceElement

object FirAnonymousFunctionChecker : FirExpressionChecker<FirStatement>() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirAnonymousFunction) {
            return
        }
        for (valueParameter in expression.valueParameters) {
            val source = valueParameter.source ?: continue
            if (valueParameter.defaultValue != null) {
                reporter.reportOn(source, FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE, context)
            }
            if (valueParameter.isVararg) {
                reporter.reportOn(source, FirErrors.USELESS_VARARG_ON_PARAMETER, context)
            }
        }

        checkTypeParameters(expression, reporter, context)
    }

    private fun checkTypeParameters(
        expression: FirStatement,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val source = expression.source ?: return
        source.treeStructure.typeParametersList(source.lighterASTNode)?.let { _ ->
            reporter.reportOn(
                source,
                FirErrors.TYPE_PARAMETERS_NOT_ALLOWED,
                context
            )
        }
    }
}
