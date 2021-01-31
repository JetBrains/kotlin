/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.WRONG_SETTER_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit


object FirGetterSetterChecker : FirPropertyChecker() {

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val setter = declaration.setter ?: return
        val valueSetterParameter = setter.valueParameters.first()
        if (valueSetterParameter.isVararg) {
            return
        }
        val setterReturnType = setter.returnTypeRef.coneType
        val valueSetterType = valueSetterParameter.returnTypeRef.coneType

        val getter = declaration.getter ?: return
        val propertyType = declaration.returnTypeRef.coneType

        if (propertyType is ConeClassErrorType || valueSetterType is ConeClassErrorType) {
            return
        }

        if (!setterReturnType.isUnit) {
            reporter.reportOn(setter.returnTypeRef.source, WRONG_SETTER_RETURN_TYPE, context)
        }

        if (getter.visibility != declaration.visibility) {
            reporter.reportOn(getter.source, GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, context)
        }
    }
}
