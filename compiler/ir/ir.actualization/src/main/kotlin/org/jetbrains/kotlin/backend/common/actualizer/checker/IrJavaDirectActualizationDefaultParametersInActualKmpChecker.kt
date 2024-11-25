/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMap
import org.jetbrains.kotlin.backend.common.actualizer.reportJavaDirectActualizationDefaultParametersInActualFunction
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * [IrJavaDirectActualizationDefaultParametersInExpectKmpChecker]
 */
internal object IrJavaDirectActualizationDefaultParametersInActualKmpChecker : IrExpectActualChecker {
    override fun check(context: IrExpectActualChecker.Context) = with(context) {
        for ((expectSymbol, actualSymbol) in expectActualMap.expectToActual) {
            if (actualSymbol !is IrClassSymbol) continue
            if (expectSymbol !is IrClassSymbol) continue
            if (actualSymbol.owner.parent !is IrPackageFragment) continue // Top level
            if (!actualSymbol.owner.hasAnnotation(StandardClassIds.Annotations.KotlinActual)) continue

            // We need to do the manual recursive traverse instead of just iterating over the expectActualMap
            // because of additional fake-overrides added during actualization
            checkDefaultParametersInActualRecursive(actualSymbol.owner, expectActualMap, diagnosticsReporter, expectSymbol)
        }
    }
}

private fun checkDefaultParametersInActualRecursive(
    actual: IrClass,
    expectActualMap: IrExpectActualMap,
    diagnosticsReporter: IrDiagnosticReporter,
    topLevelExpect: IrClassSymbol
) {
    for (member in actual.declarations) {
        when (member) {
            // todo KT-67202 it is probably be ok to allow default parameters in fake-overrides that also existed in the expect declaration
            //  (as it's done for Kotlin-to-Kotlin actualization), but since IR doesn't build fake-overrides in expect classes,
            //  it's currently not possible to check what fake-overrides existed in the expect declaration
            is IrFunction -> {
                for (parameter in member.parameters) {
                    if (!parameter.hasDefaultValue() || actual.isAnnotationClass) continue
                    diagnosticsReporter.reportJavaDirectActualizationDefaultParametersInActualFunction(
                        actualFunction = member,
                        reportOn = expectActualMap.actualToDirectExpect[member.symbol] ?: topLevelExpect,
                    )
                }
            }
            is IrClass -> checkDefaultParametersInActualRecursive(member, expectActualMap, diagnosticsReporter, topLevelExpect)
        }
    }
}
