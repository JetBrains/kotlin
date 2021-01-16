/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.name.SpecialNames

object FirFunctionNameChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source
        if (source == null || source.kind is FirFakeSourceElementKind) return
        val containingDeclaration = context.containingDeclarations.lastOrNull()
        val isNonLocal = containingDeclaration is FirFile || containingDeclaration is FirClass<*>
        if (declaration is FirSimpleFunction && declaration.name == SpecialNames.NO_NAME_PROVIDED && isNonLocal) {
            reporter.report(source, FirErrors.FUNCTION_DECLARATION_WITH_NO_NAME)
        }
    }
}
