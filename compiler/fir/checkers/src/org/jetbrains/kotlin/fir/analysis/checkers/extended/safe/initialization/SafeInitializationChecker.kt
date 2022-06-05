/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.StateOfClass
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.cache
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

object SafeInitialisationChecker : FirRegularClassChecker() {

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {

        val state = cache.getOrPut(declaration) { StateOfClass(declaration, context).apply(StateOfClass::checkClass) }
        val errors = state.errors

        for (error in errors) {
            when (error) {
                is Error.AccessError -> {
                    val (_, field) = error.effect
                    reporter.reportOn(field.source, FirErrors.ACCESS_TO_UNINITIALIZED_VALUE, field.symbol, error.toString(), context)
                }
                is Error.InvokeError -> {
                    val (_, method) = error.effect
                    reporter.reportOn(method.source, FirErrors.INVOKE_METHOD_ON_COLD_OBJECT, error.toString(), context)
                }
                is Error.PromoteError -> {
                    val pot = error.effect.potential
                    reporter.reportOn(
                        pot.firElement.source,
                        FirErrors.VALUE_CANNOT_BE_PROMOTED,
                        error.toString(),
                        context
                    )
                }
            }
        }
    }
}
