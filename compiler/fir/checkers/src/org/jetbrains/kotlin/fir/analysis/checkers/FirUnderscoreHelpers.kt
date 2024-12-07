/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.types.*

internal fun checkUnderscoreDiagnostics(
    source: KtSourceElement?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    isExpression: Boolean
) {
    val sourceKind = source?.kind ?: return

    if (sourceKind is KtRealSourceElementKind || sourceKind is KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess) {
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

internal fun checkTypeRefForUnderscore(
    typeRef: FirTypeRef?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    if (typeRef is FirErrorTypeRef) return

    typeRef?.forEachQualifierPart { qualifierPart ->
        checkUnderscoreDiagnostics(qualifierPart.source, context, reporter, isExpression = true)

        for (typeArgument in qualifierPart.typeArgumentList.typeArguments) {
            if (typeArgument is FirTypeProjectionWithVariance) {
                checkTypeRefForUnderscore(typeArgument.typeRef, context, reporter)
            } else {
                checkUnderscoreDiagnostics(typeArgument.source, context, reporter, isExpression = true)
            }
        }
    }
}

private fun FirTypeRef.forEachQualifierPart(block: (FirQualifierPart) -> Unit) {
    val delegatedTypeRef = (this as? FirResolvedTypeRef)?.delegatedTypeRef
    (delegatedTypeRef as? FirUserTypeRef)?.qualifier?.forEach(block)
}
