/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass

object FirTypeParametersInObjectChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass || declaration.classKind != ClassKind.OBJECT) {
            return
        }

        if (declaration.typeParameters.isNotEmpty()) {
            reporter.report(declaration.source)
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(FirErrors.TYPE_PARAMETERS_IN_OBJECT.on(it)) }
    }
}