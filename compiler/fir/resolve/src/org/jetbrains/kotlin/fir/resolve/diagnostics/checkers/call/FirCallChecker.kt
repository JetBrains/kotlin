/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.call

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter

abstract class FirCallChecker {
    abstract fun check(functionCall: FirFunctionCall, reporter: DiagnosticReporter)
}