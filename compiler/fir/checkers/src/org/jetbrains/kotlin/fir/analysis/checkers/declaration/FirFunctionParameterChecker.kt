/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isInline
import org.jetbrains.kotlin.fir.analysis.checkers.valOrVarKeyword
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOnWithSuppression
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnsignedTypeOrNullableUnsignedType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirFunctionParameterChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        checkVarargParameters(declaration, context, reporter)
        checkParameterTypes(declaration, context, reporter)
        checkUninitializedParameter(declaration, context, reporter)
        checkValOrVarParameter(declaration, context, reporter)
    }

    private fun checkParameterTypes(function: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        for (valueParameter in function.valueParameters) {
            val returnTypeRef = valueParameter.returnTypeRef
            if (returnTypeRef !is FirErrorTypeRef) continue
            // type problems on real source are already reported by ConeDiagnostic.toFirDiagnostics
            if (returnTypeRef.source?.kind == FirRealSourceElementKind) continue

            val diagnostic = returnTypeRef.diagnostic
            if (diagnostic is ConeSimpleDiagnostic && diagnostic.kind == DiagnosticKind.ValueParameterWithNoTypeAnnotation) {
                reporter.reportOnWithSuppression(
                    valueParameter,
                    FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION,
                    context
                )
            }
        }
    }

    private fun checkVarargParameters(function: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val varargParameters = function.valueParameters.filter { it.isVararg }
        if (varargParameters.size > 1) {
            for (parameter in varargParameters) {
                reporter.reportOnWithSuppression(parameter, FirErrors.MULTIPLE_VARARG_PARAMETERS, context)
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
                reporter.reportOnWithSuppression(
                    varargParameter,
                    FirErrors.FORBIDDEN_VARARG_PARAMETER_TYPE,
                    varargParameterType,
                    context
                )
            }
        }
    }

    private fun checkUninitializedParameter(function: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        for ((index, parameter) in function.valueParameters.withIndex()) {
            // Alas, CheckerContext.qualifiedAccesses stack is not available at this point.
            // Thus, manually visit default value expression and report the diagnostic on qualified accesses of interest.
            parameter.defaultValue?.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    element.acceptChildren(this)
                }

                override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
                    val namedReference = qualifiedAccessExpression.calleeReference as? FirResolvedNamedReference ?: return
                    val referredParameter = namedReference.resolvedSymbol.fir as? FirValueParameter ?: return
                    val referredParameterIndex = function.valueParameters.indexOf(referredParameter)
                    // Skip if the referred parameter is not declared in the same function.
                    if (referredParameterIndex < 0) return

                    if (index <= referredParameterIndex) {
                        reporter.reportOnWithSuppression(
                            qualifiedAccessExpression,
                            FirErrors.UNINITIALIZED_PARAMETER,
                            referredParameter.symbol,
                            context
                        )
                    }
                }
            })
        }
    }

    private fun checkValOrVarParameter(function: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (function is FirConstructor && function.isPrimary) {
            // `val/var` is valid for primary constructors, but not for secondary constructors
            return
        }

        for (valueParameter in function.valueParameters) {
            val source = valueParameter.source
            if (source?.kind is FirFakeSourceElementKind) continue
            source.valOrVarKeyword?.let {
                if (function is FirConstructor) {
                    reporter.reportOn(source, FirErrors.VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER, it, context)
                } else {
                    reporter.reportOn(source, FirErrors.VAL_OR_VAR_ON_FUN_PARAMETER, it, context)
                }
            }
        }
    }
}
