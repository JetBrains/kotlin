/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeUnionType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.isSubtypeOf

object FirResolvedUnionTypeRefChecker : FirResolvedTypeRefChecker(Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val source = typeRef.source ?: return
        val type = typeRef.coneType.fullyExpandedType()

        if (type is ConeUnionType) {
            checkMultipleTypeParametersCanHoldError(type, source)
        }

        type.forEachType {
            if (it is ConeUnionType && it.primaryType is ConeDynamicType) {
                reporter.reportOn(source, FirErrors.DYNAMIC_IN_UNION_TYPE)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMultipleTypeParametersCanHoldError(type: ConeUnionType, source: KtSourceElement) {
        val canPrimaryContainRichError =
            type.primaryType.let { it is ConeTypeParameterType && !it.isSubtypeOf(StandardTypes.NullableNonError, context.session) }
        val errorCompatibleTypeParameterCount =
            type.richErrorTypes.count { it is ConeTypeParameterType } + if (canPrimaryContainRichError) 1 else 0
        if (errorCompatibleTypeParameterCount > 1) {
            reporter.reportOn(source, FirErrors.MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR)
        }
    }
}
