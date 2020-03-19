/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.call

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.diagnostics.onSource

object FirSuperclassNotAccessibleFromInterfaceChecker : FirExpressionChecker<FirFunctionCall>() {
    override fun check(functionCall: FirFunctionCall, reporter: DiagnosticReporter) {
        TODO("Waiting for implicit receivers API")
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(Errors.SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE.onSource(it))
        }
    }
}