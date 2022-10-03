/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.closestPubliclyAccessibleContainer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.isPredefinedObject
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.declaration.FirJsExternalChecker.DEFINED_EXTERNALLY_PROPERTY_NAMES
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

object FirJsDefinedExternallyCallChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val reference = expression.calleeReference.resolved ?: return
        val symbol = reference.resolvedSymbol as? FirCallableSymbol<*> ?: return

        if (symbol.callableId !in DEFINED_EXTERNALLY_PROPERTY_NAMES) {
            return
        }

        val container = context.closestPubliclyAccessibleContainer?.symbol ?: return

        if (!container.isNativeObject(context) && !container.isPredefinedObject(context)) {
            reporter.reportOn(expression.source, FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, context)
        }
    }
}