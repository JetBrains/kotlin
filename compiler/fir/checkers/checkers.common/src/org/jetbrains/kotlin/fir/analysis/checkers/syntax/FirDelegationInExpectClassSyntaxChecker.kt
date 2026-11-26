/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.syntax.withSyntax

object FirDelegationInExpectClassSyntaxChecker : FirDeclarationSyntaxChecker<FirRegularClass>() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(
        element: FirRegularClass,
        source: KtSourceElement,
    ) {
        if (!element.isExpect) return

        withSyntax {
            for (superTypeRef in element.superTypeRefs) {
                val delegatedEntry = superTypeRef.source?.findDelegatedSuperTypeEntrySource() ?: continue
                reporter.reportOn(delegatedEntry, FirErrors.IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS)
            }
        }
    }
}
