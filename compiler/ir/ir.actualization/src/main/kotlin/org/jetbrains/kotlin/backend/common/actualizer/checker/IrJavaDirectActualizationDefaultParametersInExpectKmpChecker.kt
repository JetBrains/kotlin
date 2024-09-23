/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.reportJavaDirectActualizationDefaultParametersInExpectFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Also see: [IrJavaDirectActualizationDefaultParametersInActualKmpChecker]
 */
internal object IrJavaDirectActualizationDefaultParametersInExpectKmpChecker : IrExpectActualChecker {
    override fun check(context: IrExpectActualChecker.Context) = with(context) {
        val expectToActual = expectActualMap.expectToActual
        for ((expectSymbol, actualSymbol) in expectToActual) {
            if (actualSymbol !is IrFunctionSymbol) continue
            if (expectSymbol !is IrFunctionSymbol) continue

            val expect = expectSymbol.owner
            val actual = actualSymbol.owner

            // We are not checking fake-overrides because:
            // 1. IR doesn't build fake-overrides for expect declarations
            // 2. All fake-overrides that exist in the expect declarations automatically exist in the actual declaration.
            //    And we do check fake overrides in the actual declaration in IrJavaDirectActualizationDefaultParametersInActualKmpChecker
            if (actual.hasAnnotation(StandardClassIds.Annotations.KotlinActual)) {
                for (parameter in expect.valueParameters) {
                    if (parameter.hasDefaultValue()) {
                        diagnosticsReporter.reportJavaDirectActualizationDefaultParametersInExpectFunction(
                            expectFunction = expect,
                            reportOn = parameter.symbol
                        )
                    }
                }
            }
        }
    }
}
