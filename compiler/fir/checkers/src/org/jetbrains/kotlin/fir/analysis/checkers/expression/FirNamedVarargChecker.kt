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
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.isArrayType

object FirNamedVarargChecker : FirCallChecker() {
    override fun check(expression: FirCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirFunctionCall &&
            expression !is FirAnnotation &&
            expression !is FirDelegatedConstructorCall &&
            expression !is FirArrayOfCall) return
        val isAnnotation = expression is FirAnnotation
        val singleElementToVarargErrorFactory =
            if (isAnnotation) FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION
            else FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION
        val redundantSpreadWarningFactory =
            if (isAnnotation) FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION
            else FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION

        val allowAssignArray = context.session.languageVersionSettings.supportsFeature(
            if (isAnnotation) LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations
            else LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
        )

        fun checkArgument(argument: FirExpression, isVararg: Boolean) {
            if (argument !is FirNamedArgumentExpression) return
            if (argument.isSpread) {
                if (isVararg && (expression as? FirResolvable)?.calleeReference !is FirErrorNamedReference) {
                    reporter.reportOn(argument.expression.source, redundantSpreadWarningFactory, context)
                }
                return
            }
            val typeRef = argument.expression.typeRef
            if (typeRef is FirErrorTypeRef) return
            if (argument.expression is FirArrayOfCall) return
            if (allowAssignArray && typeRef.isArrayType) return

            reporter.reportOn(argument.expression.source, singleElementToVarargErrorFactory, context)
        }

        if (expression is FirArrayOfCall) {
            // FirArrayOfCall has the `vararg` argument expression pre-flattened and doesn't have an argument mapping.
            expression.arguments.forEach { checkArgument(it, it is FirNamedArgumentExpression) }
        } else {
            val argumentMap = expression.argumentMapping ?: return
            for ((argument, parameter) in argumentMap) {
                if (!parameter.isVararg) continue
                if (argument is FirVarargArgumentsExpression) {
                    argument.arguments.forEach { checkArgument(it, true) }
                } else {
                    checkArgument(argument, false)
                }
            }
        }
    }
}
