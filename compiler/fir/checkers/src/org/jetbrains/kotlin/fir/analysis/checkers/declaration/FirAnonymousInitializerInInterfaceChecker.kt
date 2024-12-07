/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.findClosestClassOrObject
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.utils.isInterface

object FirAnonymousInitializerInInterfaceChecker : FirAnonymousInitializerChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirAnonymousInitializer, context: CheckerContext, reporter: DiagnosticReporter) {
        val clazz = context.findClosestClassOrObject() ?: return
        if (clazz.isInterface) {
            reporter.reportOn(declaration.source, FirErrors.ANONYMOUS_INITIALIZER_IN_INTERFACE, context)
        }
    }
}
