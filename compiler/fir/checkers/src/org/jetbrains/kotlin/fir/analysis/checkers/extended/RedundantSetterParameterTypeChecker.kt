/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor

object RedundantSetterParameterTypeChecker : FirMemberDeclarationChecker() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirProperty) return
        val setter = declaration.setter ?: return
        if (setter is FirDefaultPropertyAccessor) return
        val valueParameter = setter.valueParameters.firstOrNull() ?: return
        val propertyTypeSource = declaration.returnTypeRef.source
        val setterParameterTypeSource = valueParameter.returnTypeRef.source ?: return

        if (setterParameterTypeSource != propertyTypeSource) {
            reporter.report(setterParameterTypeSource, REDUNDANT_SETTER_PARAMETER_TYPE)
        }
    }
}