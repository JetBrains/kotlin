/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_SETTER_PARAMETER_TYPE
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor

object RedundantSetterParameterTypeChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val setter = declaration.setter ?: return
        if (setter is FirDefaultPropertyAccessor) return
        val valueParameter = setter.valueParameters.firstOrNull() ?: return
        val propertyTypeSource = declaration.returnTypeRef.source
        val setterParameterTypeSource = valueParameter.returnTypeRef.source ?: return

        if (setterParameterTypeSource.kind !is KtFakeSourceElementKind && setterParameterTypeSource != propertyTypeSource) {
            reporter.reportOn(setterParameterTypeSource, REDUNDANT_SETTER_PARAMETER_TYPE, context)
        }
    }
}
