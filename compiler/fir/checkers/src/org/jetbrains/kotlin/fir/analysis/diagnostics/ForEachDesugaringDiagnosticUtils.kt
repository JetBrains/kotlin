/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn

context(context: DiagnosticContext)
fun DiagnosticReporter.reportOnGuardOrItself(
    source: KtSourceElement?,
    factory: KtDiagnosticFactory0,
    positioningStrategy: AbstractSourceElementPositioningStrategy? = null
): Unit = when (val kind = source?.kind) {
    KtFakeSourceElementKind.DesugaredForEachJump.Break if factory == FirErrors.RETURN_NOT_ALLOWED ->
        reportOn(source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, positioningStrategy)
    KtFakeSourceElementKind.DesugaredForEachJump.Continue if factory == FirErrors.RETURN_NOT_ALLOWED ->
        reportOn(source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, positioningStrategy)
    is KtFakeSourceElementKind.DesugaredForEachGuard.Break if factory == FirErrors.RETURN_NOT_ALLOWED -> kind.sources.forEach { source ->
        reportOn(source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, positioningStrategy)
    }
    is KtFakeSourceElementKind.DesugaredForEachGuard.Continue if factory == FirErrors.RETURN_NOT_ALLOWED -> kind.sources.forEach { source ->
        reportOn(source, FirErrors.BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY, positioningStrategy)
    }
    is KtFakeSourceElementKind.DesugaredForEachGuard -> kind.sources.forEach { source ->
        reportOn(source, factory, positioningStrategy)
    }
    else -> reportOn(source, factory, positioningStrategy)
}
