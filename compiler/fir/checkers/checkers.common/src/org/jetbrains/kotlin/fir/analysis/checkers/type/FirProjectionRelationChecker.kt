/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.ProjectionRelationCheckerImpl
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractArgumentsTypeRefAndSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeIllegalProjectionInExpansion
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.contains

object FirProjectionRelationChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.source?.kind is KtFakeSourceElementKind) return

        ProjectionRelationCheckerImpl.doCheck(extractArgumentsTypeRefAndSource(typeRef), typeRef.coneType.abbreviatedTypeOrSelf)

        checkForIllegalProjection(typeRef.coneType, typeRef.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkForIllegalProjection(type: ConeKotlinType, source: KtSourceElement?) {
        if (type.fullyExpandedType().contains { it is ConeErrorType && it.diagnostic is ConeIllegalProjectionInExpansion }) {
            reporter.reportOn(source, FirErrors.ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION)
        }
    }
}
