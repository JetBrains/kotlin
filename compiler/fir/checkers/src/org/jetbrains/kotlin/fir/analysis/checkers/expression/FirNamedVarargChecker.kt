/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.isArrayType

object FirNamedVarargChecker : FirCallChecker() {
    override fun check(expression: FirCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirFunctionCall && expression !is FirAnnotationCall && expression !is FirDelegatedConstructorCall) return
        val isAnnotation = expression is FirAnnotationCall
        val errorFactory =
            if (isAnnotation) FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION
            else FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION

        val allowAssignArray = context.session.languageVersionSettings.supportsFeature(
            if (isAnnotation) LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations
            else LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
        )

        fun checkArgument(argument: FirExpression) {
            if (argument !is FirNamedArgumentExpression) return
            if (argument.isSpread) return
            val typeRef = argument.expression.typeRef
            if (typeRef is FirErrorTypeRef) return
            if (argument.expression is FirArrayOfCall) return
            if (allowAssignArray && typeRef.isArrayType) return

            reporter.reportOn(argument.expression.source, errorFactory, context)
        }

        val argumentMap = expression.argumentMapping ?: return
        for ((argument, parameter) in argumentMap) {
            if (!parameter.isVararg) continue
            if (argument is FirVarargArgumentsExpression) {
                argument.arguments.forEach(::checkArgument)
            } else {
                checkArgument(argument)
            }
        }
    }


}