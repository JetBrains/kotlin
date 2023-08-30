/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.visibility

object FirLocalEntityNotAllowedChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.visibility != Visibilities.Local) {
            return
        }

        val container = context.containingDeclarations.lastOrNull()

        when {
            declaration.classKind == ClassKind.OBJECT && !declaration.isCompanion ->
                reporter.reportOn(declaration.source, FirErrors.LOCAL_OBJECT_NOT_ALLOWED, declaration.name, context)
            declaration.classKind == ClassKind.INTERFACE && container !is FirClass ->
                reporter.reportOn(declaration.source, FirErrors.LOCAL_INTERFACE_NOT_ALLOWED, declaration.name, context)
            else -> {
            }
        }
    }
}
