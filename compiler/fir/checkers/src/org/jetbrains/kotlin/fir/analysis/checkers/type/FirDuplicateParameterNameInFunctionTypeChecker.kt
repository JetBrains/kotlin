/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.CheckerSessionKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.type

object FirDuplicateParameterNameInFunctionTypeChecker : FirResolvedTypeRefChecker(CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers) {
    override fun check(typeRef: FirResolvedTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!typeRef.type.isSomeFunctionType(context.session)) return

        val nameToArgumentProjection = typeRef.type.typeArguments.dropLast(1).groupBy {
            val type = it.type ?: return@groupBy null
            val annotation = type.customAnnotations.getAnnotationByClassId(StandardNames.FqNames.parameterNameClassId, context.session)
            val nameEntry = annotation?.argumentMapping?.mapping?.get(StandardNames.NAME)
            (nameEntry as? FirLiteralExpression)?.value as? String
        }

        for ((name, projections) in nameToArgumentProjection) {
            if (name != null && projections.size >= 2) {
                reporter.reportOn(typeRef.source, FirErrors.DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE, context)
            }
        }
    }
}
