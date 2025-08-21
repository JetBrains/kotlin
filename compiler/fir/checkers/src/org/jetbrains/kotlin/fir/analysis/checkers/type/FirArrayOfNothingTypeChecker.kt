/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.unsupportedArrayOfNothingKind
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

object FirArrayOfNothingTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        /** Ignore typealias, see [FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS] */
        if (context.containingDeclarations.lastOrNull() is FirTypeAliasSymbol) return
        val fullyExpandedType = typeRef.coneType.fullyExpandedType()

        /** Ignore vararg, see varargOfNothing.kt test */
        val isVararg = (context.containingDeclarations.lastOrNull() as? FirValueParameterSymbol)?.isVararg ?: false
        if (!isVararg) {
            val arrayOfNothingKind = fullyExpandedType.unsupportedArrayOfNothingKind(context.languageVersionSettings)
            if (arrayOfNothingKind != null) {
                reporter.reportOn(typeRef.source, FirErrors.UNSUPPORTED, "'${arrayOfNothingKind.representation}' is an invalid array type.")
            }
        }
    }
}
