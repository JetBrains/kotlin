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
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasBody
import org.jetbrains.kotlin.fir.declarations.isExpect

// Note that the class that contains the currently visiting declaration will *not* be in the context's containing declarations *yet*.
internal fun isInsideExpectClass(containingDeclaration: FirRegularClass, context: CheckerContext): Boolean =
    containingDeclaration.isExpect || context.containingDeclarations.asReversed().any { it is FirRegularClass && it.isExpect }

internal fun checkExpectFunction(function: FirSimpleFunction, reporter: DiagnosticReporter) {
    val source = function.source ?: return
    if (source.kind is FirFakeSourceElementKind) return
    if (function.hasBody) {
        reporter.report(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY)
    }
}
