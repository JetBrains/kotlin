/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findNonInterfaceSupertype
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass

object FirEnumClassSimpleChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isEnumClass) {
            return
        }

        declaration.findNonInterfaceSupertype(context)?.let {
            reporter.reportOn(it.source, FirErrors.CLASS_IN_SUPERTYPE_FOR_ENUM, context)
        }

        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(declaration.typeParameters.firstOrNull()?.source, FirErrors.TYPE_PARAMETERS_IN_ENUM, context)
        }
    }
}
