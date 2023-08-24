/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.delegateFieldsMap
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirJsDynamicDeclarationChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val delegatedFields = declaration.delegateFieldsMap ?: return

        for ((_, delegate) in delegatedFields) {
            @OptIn(SymbolInternals::class)
            // Accessing fir here is ok, because it still
            // belongs to the current `declaration: FirClass`,
            // and it's a shape it couldn't have been accessed directly
            val initializer = delegate.fir.initializer ?: continue

            if (initializer.resolvedType is ConeDynamicType) {
                reporter.reportOn(initializer.source, FirJsErrors.DELEGATION_BY_DYNAMIC, context)
            }
        }
    }
}
