/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.safe.initialization.Checker.checkClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.utils.addIfNotNull

object SafeInitialisationChecker : FirRegularClassChecker() {

    private val cache = mutableSetOf<Checker.StateOfClass>()

    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val state = Checker.StateOfClass(declaration)
        val errors = state.checkClass().flatten()
        cache.add(state)
    }
}
