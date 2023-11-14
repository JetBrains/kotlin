/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.backend.common.actualizer.ClassActualizationInfo
import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMatchingContext
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext

internal interface IrExpectActualChecker {
    interface Context {
        val matchedExpectToActual: Map<IrSymbol, IrSymbol>
        val classActualizationInfo: ClassActualizationInfo
        val typeSystemContext: IrTypeSystemContext
        val diagnosticsReporter: IrDiagnosticReporter
        val matchingContext: IrExpectActualMatchingContext
    }

    fun check(context: Context)
}
