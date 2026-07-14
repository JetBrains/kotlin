/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import org.jetbrains.kotlin.backend.common.checkers.CommonKlibDiagnosticContext
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.expressions.IrExpression

interface NativeKlibExpressionsChecker<D : IrExpression> {
    fun check(expression: D, context: CommonKlibDiagnosticContext, reporter: IrDiagnosticReporter)
}
