/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn

fun checkUnderscoreDiagnostics(
    source: FirSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isExpression: Boolean
) {
    if (source != null && (source.kind is FirRealSourceElementKind || source.kind is FirFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)) {
        with(SourceNavigator.forSource(source)) {
            if (source.getRawIdentifier()?.isUnderscore == true) {
                reporter.reportOn(
                    source,
                    if (isExpression) FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS else FirErrors.UNDERSCORE_IS_RESERVED,
                    context
                )
            }
        }
    }
}

val CharSequence.isUnderscore: Boolean
    get() = all { it == '_' }