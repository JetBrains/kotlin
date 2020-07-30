/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*

object FirLocalEntityNotAllowedChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass || declaration.visibility != Visibilities.LOCAL) {
            return
        }

        when {
            declaration.classKind == ClassKind.OBJECT && !declaration.isCompanion -> reporter.reportLocalObjectNotAllowed(declaration.source)
            declaration.classKind == ClassKind.INTERFACE -> reporter.reportLocalInterfaceNotAllowed(declaration.source)
            else -> {
            }
        }
    }

    private fun DiagnosticReporter.reportLocalObjectNotAllowed(source: FirSourceElement?) {
        source?.let { report(FirErrors.LOCAL_OBJECT_NOT_ALLOWED.on(it)) }
    }

    private fun DiagnosticReporter.reportLocalInterfaceNotAllowed(source: FirSourceElement?) {
        source?.let { report(FirErrors.LOCAL_INTERFACE_NOT_ALLOWED.on(it)) }
    }
}