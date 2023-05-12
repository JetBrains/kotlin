/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.delegateFieldsMap
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf

object FirDelegateFieldTypeMismatchChecker : FirRegularClassChecker() {
    @SymbolInternals
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (it in declaration.superTypeRefs.indices) {
            val supertype = declaration.superTypeRefs[it]
            val field = declaration.delegateFieldsMap?.get(it)?.fir ?: continue
            val initializer = field.initializer ?: continue
            val isReportedByErrorNodeDiagnosticCollector = initializer is FirCall && initializer.calleeReference?.isError() == true

            if (
                !isReportedByErrorNodeDiagnosticCollector &&
                !initializer.typeRef.coneType.isSubtypeOf(supertype.coneType, context.session, true)
            ) {
                reporter.reportOn(
                    initializer.source,
                    FirErrors.TYPE_MISMATCH,
                    field.returnTypeRef.coneType,
                    initializer.typeRef.coneType,
                    false,
                    context,
                )
            }
        }
    }
}
