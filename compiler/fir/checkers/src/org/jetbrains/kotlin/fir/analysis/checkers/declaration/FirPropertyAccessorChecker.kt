/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit

object FirPropertyAccessorChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        checkGetter(declaration, context, reporter)
        checkSetter(declaration, context, reporter)
    }

    private fun checkGetter(property: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val getter = property.getter ?: return
        withSuppressedDiagnostics(getter, context) {
            if (getter.visibility != property.visibility) {
                reporter.reportOn(getter.source, FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, context)
            }
        }
    }

    private fun checkSetter(property: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val setter = property.setter ?: return

        withSuppressedDiagnostics(setter, context) {
            if (property.isVal) {
                reporter.reportOn(setter.source, FirErrors.VAL_WITH_SETTER, context)
            }

            val valueSetterParameter = setter.valueParameters.first()
            if (valueSetterParameter.isVararg) {
                return
            }
            val valueSetterType = valueSetterParameter.returnTypeRef.coneType
            val valueSetterTypeSource = valueSetterParameter.returnTypeRef.source
            val propertyType = property.returnTypeRef.coneType
            if (propertyType is ConeClassErrorType || valueSetterType is ConeClassErrorType) {
                return
            }

            if (valueSetterType != propertyType) {
                withSuppressedDiagnostics(valueSetterParameter, context) {
                    reporter.reportOn(valueSetterTypeSource, FirErrors.WRONG_SETTER_PARAMETER_TYPE, propertyType, valueSetterType, context)
                }
            }

            val setterReturnType = setter.returnTypeRef.coneType
            if (propertyType is ConeClassErrorType || valueSetterType is ConeClassErrorType) {
                return
            }

            if (!setterReturnType.isUnit) {
                reporter.reportOn(setter.returnTypeRef.source, FirErrors.WRONG_SETTER_RETURN_TYPE, context)
            }
        }
    }
}
