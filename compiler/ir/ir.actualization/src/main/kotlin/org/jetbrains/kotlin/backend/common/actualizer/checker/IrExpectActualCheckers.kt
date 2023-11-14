/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.backend.common.actualizer.ClassActualizationInfo
import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMatchingContext
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext

internal class IrExpectActualCheckers(
    override val matchedExpectToActual: Map<IrSymbol, IrSymbol>,
    override val classActualizationInfo: ClassActualizationInfo,
    override val typeSystemContext: IrTypeSystemContext,
    override val diagnosticsReporter: IrDiagnosticReporter,
) : IrExpectActualChecker.Context {

    private val checkers: Set<IrExpectActualChecker> = setOf(
        IrExpectActualAnnotationMatchingChecker,
        IrExpectActualAnnotationConflictingDefaultArgumentValueChecker,
    )

    override val matchingContext = object : IrExpectActualMatchingContext(typeSystemContext, classActualizationInfo.actualClasses) {
        override fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol) {
            error("Must not be called")
        }

        override fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            error("Must not be called")
        }
    }

    fun check() {
        for (checker in checkers) {
            checker.check(context = this)
        }
    }
}