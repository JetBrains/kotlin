/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_SETTER_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.coneType


object FirPropertyAccessorChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val setter = declaration.setter ?: return
        val valueSetterParameter = setter.valueParameters.first()
        if (valueSetterParameter.isVararg) {
            return
        }
        val valueSetterType = valueSetterParameter.returnTypeRef.coneType
        val valueSetterTypeSource = valueSetterParameter.returnTypeRef.source
        val propertyType = declaration.returnTypeRef.coneType
        if (propertyType is ConeClassErrorType || valueSetterType is ConeClassErrorType) {
            return
        }

        if (valueSetterType != propertyType) {
            withSuppressedDiagnostics(setter, context) {
                withSuppressedDiagnostics(valueSetterParameter, context) {
                    reporter.reportOn(valueSetterTypeSource, WRONG_SETTER_PARAMETER_TYPE, propertyType, valueSetterType, context)
                }
            }
        }
    }
}
