/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

object FirAnnotationClassInheritanceChecker : FirClassChecker(MppCheckerKind.Common) {
    @SymbolInternals
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val superAnnotations = declaration.symbol.getSuperTypes(context.session)
            .mapNotNull { it.toRegularClassSymbol() }
            .filter { it.classKind == ClassKind.ANNOTATION_CLASS }

        for (anno in superAnnotations) {
            reporter.reportOn(declaration.source, FirErrors.EXTENDING_AN_ANNOTATION_CLASS, anno)
        }
    }
}
