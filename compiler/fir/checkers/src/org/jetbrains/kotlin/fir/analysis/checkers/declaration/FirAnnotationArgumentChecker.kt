/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.ConstantArgumentKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkConstantArguments
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.psi.KtExpression

object FirAnnotationArgumentChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirAnnotationContainer) return
        for (declarationOfAnnotation in declaration.annotations) {
            for ((arg, _) in declarationOfAnnotation.argumentMapping ?: continue) {
                val expression = (arg as? FirNamedArgumentExpression)?.expression ?: arg

                checkAnnotationArgumentWithSubElements(expression, context.session, reporter, context)
                    ?.let { reporter.reportOn(expression.source, it, context) }
            }
        }
    }

    private fun checkAnnotationArgumentWithSubElements(
        expression: FirExpression,
        session: FirSession,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ): FirDiagnosticFactory0<KtExpression>? {
        when (expression) {
            is FirArrayOfCall -> {
                var usedNonConst = false

                for (arg in expression.argumentList.arguments) {
                    val sourceForReport = arg.source

                    when (val err = checkAnnotationArgumentWithSubElements(arg, session, reporter, context)) {
                        null -> {
                            //DO NOTHING
                        }
                        else -> {
                            if (err != FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) usedNonConst = true
                            reporter.reportOn(sourceForReport, err, context)
                        }
                    }
                }

                if (usedNonConst) return FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
            }
            is FirVarargArgumentsExpression -> {
                for (arg in expression.arguments)
                    checkAnnotationArgumentWithSubElements(arg, session, reporter, context)
                        ?.let { reporter.reportOn(arg.source, it, context) }
            }
            else ->
                return when (checkConstantArguments(expression, session)) {
                    ConstantArgumentKind.NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    ConstantArgumentKind.ENUM_NOT_CONST -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
                    ConstantArgumentKind.NOT_KCLASS_LITERAL -> FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR -> FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                    ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION -> FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
                    null -> null
                }
        }
        return null
    }
}
