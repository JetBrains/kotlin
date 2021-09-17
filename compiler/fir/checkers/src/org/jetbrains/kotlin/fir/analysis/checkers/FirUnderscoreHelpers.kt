/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn

fun checkUnderscoreDiagnostics(
    source: KtSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isExpression: Boolean
) {
    if (source != null && (source.kind is KtRealSourceElementKind || source.kind is KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)) {
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