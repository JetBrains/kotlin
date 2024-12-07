/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.parameterName
import org.jetbrains.kotlin.fir.types.type

object FirDuplicateParameterNameInFunctionTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!typeRef.coneType.isSomeFunctionType(context.session)) return

        val nameToArgumentProjection = typeRef.coneType.typeArguments.dropLast(1).groupBy { it.type?.parameterName }

        for ((name, projections) in nameToArgumentProjection) {
            if (name != null && projections.size >= 2) {
                reporter.reportOn(typeRef.source, FirErrors.DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE, context)
            }
        }
    }
}
