/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

object FirDelegationSuperCallInEnumConstructorChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isEnumClass) {
            return
        }

        declaration.constructors(context.session).forEach {
            if (!it.isPrimary &&
                it.resolvedDelegatedConstructorCall?.isThis == false &&
                it.resolvedDelegatedConstructorCall?.source?.kind !is KtFakeSourceElementKind
            ) {
                reporter.reportOn(it.resolvedDelegatedConstructorCall?.source, FirErrors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR, context)
            }
        }
    }
}
