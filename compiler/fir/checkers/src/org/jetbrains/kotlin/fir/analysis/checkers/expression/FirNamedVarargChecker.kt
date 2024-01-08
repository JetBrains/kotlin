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
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.types.*

object FirNamedVarargChecker : FirCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirFunctionCall &&
            expression !is FirAnnotation &&
            expression !is FirDelegatedConstructorCall &&
            expression !is FirArrayLiteral) return
        val isAnnotation = expression is FirAnnotation
        val redundantSpreadWarningFactory =
            if (isAnnotation) FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION
            else FirErrors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION

        val allowAssignArray = context.session.languageVersionSettings.supportsFeature(
            if (isAnnotation) LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations
            else LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
        )

        fun checkArgument(argument: FirExpression, isVararg: Boolean, expectedArrayType: ConeKotlinType) {
            if (argument !is FirNamedArgumentExpression) return
            if (argument.isSpread) {
                if (isVararg && (expression as? FirResolvable)?.calleeReference !is FirErrorNamedReference) {
                    reporter.reportOn(argument.expression.source, redundantSpreadWarningFactory, context)
                }
                return
            }
            val type = argument.expression.resolvedType
            if (type is ConeErrorType) return
            if (argument.expression is FirArrayLiteral) return

            if (allowAssignArray && type.isArrayType) return

            if (isAnnotation) {
                reporter.reportOn(
                    argument.expression.source,
                    FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION,
                    context
                )
            } else {
                reporter.reportOn(
                    argument.expression.source,
                    FirErrors.ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION,
                    expectedArrayType,
                    context,
                )
            }
        }

        if (expression is FirArrayLiteral) {
            // FirArrayLiteral has the `vararg` argument expression pre-flattened and doesn't have an argument mapping.
            expression.arguments.forEach { checkArgument(it, it is FirNamedArgumentExpression, expression.resolvedType) }
        } else {
            val argumentMap = expression.resolvedArgumentMapping ?: return
            for ((argument, parameter) in argumentMap) {
                if (!parameter.isVararg) continue
                if (argument is FirVarargArgumentsExpression) {
                    argument.arguments.forEach { checkArgument(it, true, parameter.returnTypeRef.coneType) }
                } else {
                    checkArgument(argument, false, parameter.returnTypeRef.coneType)
                }
            }
        }
    }
}
