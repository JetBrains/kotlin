/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirArrayOfNothingQualifierChecker.isArrayOfNothing
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull

object FirArrayOfNothingTypeChecker : FirTypeRefChecker(MppCheckerKind.Common) {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        /** Ignore typealias, see [TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS] */
        if (context.containingDeclarations.lastOrNull() is FirTypeAlias) return
        val coneType = typeRef.coneTypeOrNull ?: return
        val fullyExpandedType = coneType.fullyExpandedType(context.session)

        /** Ignore vararg, see varargOfNothing.kt test */
        val isVararg = (context.containingDeclarations.lastOrNull() as? FirValueParameter)?.isVararg ?: false
        if (!isVararg && fullyExpandedType.isArrayOfNothing()) {
            reporter.reportOn(typeRef.source, FirErrors.UNSUPPORTED, "Array<Nothing> is illegal", context)
        }
    }
}
