/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.fullyExpandedClassId
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.StandardClassIds

object FirTypeAliasExpandsToArrayOfNothingsChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val type = declaration.expandedConeType ?: return

        if (type.isMalformed(context)) {
            reporter.reportOn(declaration.expandedTypeRef.source, FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS, type, context)
        }
    }

    private fun ConeKotlinType.isMalformed(context: CheckerContext): Boolean =
        fullyExpandedClassId(context.session) == StandardClassIds.Array
                && typeArguments.singleOrNull()?.type?.fullyExpandedClassId(context.session) == StandardClassIds.Nothing
                || containsMalformedArgument(context)

    private fun ConeKotlinType.containsMalformedArgument(context: CheckerContext) =
        typeArguments.any { it.type?.isMalformed(context) == true }

}
