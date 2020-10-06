/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isCompanion
import org.jetbrains.kotlin.fir.declarations.visibility
import org.jetbrains.kotlin.name.Name

object FirLocalEntityNotAllowedChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass || declaration.visibility != Visibilities.Local) {
            return
        }

        when {
            declaration.classKind == ClassKind.OBJECT && !declaration.isCompanion -> reporter.reportLocalObjectNotAllowed(declaration.source, declaration.name)
            declaration.classKind == ClassKind.INTERFACE -> reporter.reportLocalInterfaceNotAllowed(declaration.source, declaration.name)
            else -> {
            }
        }
    }

    private fun DiagnosticReporter.reportLocalObjectNotAllowed(source: FirSourceElement?, name: Name) {
        source?.let { report(FirErrors.LOCAL_OBJECT_NOT_ALLOWED.on(it, name)) }
    }

    private fun DiagnosticReporter.reportLocalInterfaceNotAllowed(source: FirSourceElement?, name: Name) {
        source?.let { report(FirErrors.LOCAL_INTERFACE_NOT_ALLOWED.on(it, name)) }
    }
}
