/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnsupportedDefaultValueInFunctionType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef

object FirUnsupportedDefaultValueInFunctionTypeChecker : FirValueParameterChecker() {
    override fun check(declaration: FirValueParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val diagnostic = ((declaration.defaultValue?.typeRef as? FirErrorTypeRef)?.diagnostic as? ConeUnsupportedDefaultValueInFunctionType)
        if (diagnostic != null) {
            reporter.reportOn(diagnostic.source, FirErrors.UNSUPPORTED, diagnostic.reason, context)
        }
    }
}