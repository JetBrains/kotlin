/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeUnionType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.StandardClassIds

object FirResolvedUnionTypeRefChecker : FirResolvedTypeRefChecker(Common) {
    private val nullableNonErrorType = StandardClassIds.NonError.constructClassLikeType(isMarkedNullable = true)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        val source = typeRef.source ?: return
        val type = typeRef.coneType as? ConeUnionType
        if (type != null) {
            val canPrimaryContainRichError =
                type.primaryType.let { it is ConeTypeParameterType && !it.isSubtypeOf(nullableNonErrorType, context.session) }
            val errorCompatibleTypeParameterCount =
                type.richErrorTypes.count { it is ConeTypeParameterType } + if (canPrimaryContainRichError) 1 else 0
            if (errorCompatibleTypeParameterCount > 1) {
                reporter.reportOn(source, FirErrors.MULTIPLE_TYPE_PARAMETERS_CAN_HOLD_ERROR)
            }
        }
    }
}
