/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMap
import org.jetbrains.kotlin.backend.common.actualizer.reportJavaDirectActualWithoutExpect
import org.jetbrains.kotlin.backend.common.actualizer.reportKotlinActualAnnotationMissing
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.ENUM_CLASS_SPECIAL_MEMBER
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFromJava
import org.jetbrains.kotlin.name.StandardClassIds

internal object IrKotlinActualAnnotationOnJavaKmpChecker : IrExpectActualChecker {
    override fun check(context: IrExpectActualChecker.Context) = with(context) {
        val languageVersionSettings = diagnosticsReporter.languageVersionSettings
        if (!languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return

        for ((expectSymbol, actualSymbol) in expectActualMap.expectToActual) {
            if (actualSymbol !is IrClassSymbol) continue
            if (expectSymbol !is IrClassSymbol) continue
            if (actualSymbol.owner.parent !is IrPackageFragment) continue // Top level
            if (actualSymbol.owner.classId != expectSymbol.owner.classId) continue
            checkAnnotationRecursive(actualSymbol.owner, expectActualMap, diagnosticsReporter, expectSymbol)
        }
    }
}

private fun checkAnnotationRecursive(
    actual: IrDeclaration,
    expectActualMap: IrExpectActualMap,
    diagnosticsReporter: IrDiagnosticReporter,
    topLevelExpect: IrClassSymbol
) {
    val hasAnnotation = actual.hasAnnotation(StandardClassIds.Annotations.KotlinActual)
    val expect = expectActualMap.actualToDirectExpect[actual.symbol]
    if (hasAnnotation && expect == null) {
        diagnosticsReporter.reportJavaDirectActualWithoutExpect(actual, reportOn = topLevelExpect)
    }
    if (expect != null && !hasAnnotation && actual.isFromJava() &&
        (expect.owner as? IrDeclaration)?.origin != ENUM_CLASS_SPECIAL_MEMBER
    ) {
        diagnosticsReporter.reportKotlinActualAnnotationMissing(actual, reportOn = expect)
    }
    if (actual is IrClass) {
        for (member in actual.declarations) {
            if (!member.isFakeOverride && (member is IrFunction || member is IrClass)) {
                checkAnnotationRecursive(member, expectActualMap, diagnosticsReporter, topLevelExpect)
            }
        }
    }
}
