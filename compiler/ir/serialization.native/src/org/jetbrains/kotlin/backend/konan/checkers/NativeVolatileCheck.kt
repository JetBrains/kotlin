/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import org.jetbrains.kotlin.backend.common.checkers.CommonKlibDiagnosticContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.expressions.IrCall

object NativeVolatileCheck: NativeKlibExpressionsChecker<IrCall> {
    override fun check(expression: IrCall, context: CommonKlibDiagnosticContext, reporter: IrDiagnosticReporter) {
        TODO("Not yet implemented")
    }
}
