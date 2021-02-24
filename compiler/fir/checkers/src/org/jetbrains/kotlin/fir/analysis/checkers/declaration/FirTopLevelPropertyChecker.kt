/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty

// See old FE's [DeclarationsChecker]
object FirTopLevelPropertyChecker : FirFileChecker() {
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        for (topLevelDeclaration in declaration.declarations) {
            if (topLevelDeclaration is FirProperty) {
                checkProperty(topLevelDeclaration, reporter, context)
            }
        }
    }

    private fun checkProperty(property: FirProperty, reporter: DiagnosticReporter, context: CheckerContext) {
        val source = property.source ?: return
        if (source.kind is FirFakeSourceElementKind) return
        val modifierList = with(FirModifierList) { source.getModifierList() }

        checkPropertyInitializer(null, property, reporter, context)
        checkExpectDeclarationVisibilityAndBody(property, source, modifierList, reporter, context)
    }
}
