/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass

object FirDelegationSuperCallInEnumConstructorChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isEnumClass) {
            return
        }

        for (it in declaration.declarations) {
            if (
                it is FirConstructor && !it.isPrimary &&
                it.delegatedConstructor?.isThis == false &&
                it.delegatedConstructor?.source?.kind !is FirFakeSourceElementKind
            ) {
                reporter.reportOn(it.delegatedConstructor?.source, FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR, context)
            }
        }
    }
}
