/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isInline
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnsignedTypeOrNullableUnsignedType
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirFunctionParameterChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        checkVarargParameters(declaration, context, reporter)
    }

    private fun checkVarargParameters(function: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val varargParameters = function.valueParameters.filter { it.isVararg }
        if (varargParameters.size > 1) {
            for (parameter in varargParameters) {
                reporter.reportOn(parameter.source ?: continue, FirErrors.MULTIPLE_VARARG_PARAMETERS, context)
            }
        }

        val nullableNothingType = context.session.builtinTypes.nullableNothingType.coneType
        for (varargParameter in varargParameters) {
            val varargParameterType = varargParameter.returnTypeRef.coneType.arrayElementType() ?: continue
            if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, varargParameterType, nullableNothingType) ||
                (varargParameterType.isInline(context.session) && !varargParameterType.isUnsignedTypeOrNullableUnsignedType)
            // Note: comparing with FE1.0, we skip checking if the type is not primitive because primitive types are not inline. That
            // is any primitive values are already allowed by the inline check.
            ) {
                reporter.reportOn(
                    varargParameter.source ?: continue,
                    FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE,
                    varargParameterType,
                    context
                )
            }
        }
    }
}
