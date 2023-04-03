/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassIds
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.StandardClassIds

object FirVolatileAnnotationChecker : FirPropertyChecker() {
    private val VOLATILE_CLASS_IDS = listOf(StandardClassIds.Annotations.Volatile, StandardClassIds.Annotations.JvmVolatile)

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.source?.kind != KtRealSourceElementKind) return

        val fieldAnnotation = declaration.annotations.getAnnotationByClassIds(VOLATILE_CLASS_IDS, context.session)
            ?: declaration.backingField?.annotations?.getAnnotationByClassIds(VOLATILE_CLASS_IDS, context.session)
            ?: return

        if (!declaration.isVar) {
            reporter.reportOn(fieldAnnotation.source, FirErrors.VOLATILE_ON_VALUE, context)
        }

        if (declaration.delegateFieldSymbol != null) {
            reporter.reportOn(fieldAnnotation.source, FirErrors.VOLATILE_ON_DELEGATE, context)
        }
    }
}
