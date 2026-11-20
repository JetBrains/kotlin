/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.secondToLastContainer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeUnionType

/**
 * A type alias expanding to a union type has no class behind it, so it can't be used as a qualifier
 * of a companion object, nested classifier, or static member, similar to a type parameter (see [FirTypeParameterInQualifiedAccessChecker]).
 */
object FirTypeAliasToUnionTypeQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        if (!expression.isTypeAliasToUnionType()) return
        // `TA::class` is reported by `FirClassLiteralChecker`
        if (context.getClassCalls.lastOrNull()?.argument == expression) return
        // `TA::foo` is legal.
        if (context.secondToLastContainer.let { it is FirCallableReferenceAccess && it.explicitReceiver == expression }) return
        reporter.reportOn(expression.source, FirErrors.TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER)
    }
}

context(context: CheckerContext)
internal fun FirResolvedQualifier.isTypeAliasToUnionType(): Boolean {
    val typeAliasSymbol = qualifierSymbol as? FirTypeAliasSymbol ?: return false
    return typeAliasSymbol.resolvedExpandedTypeRef.coneType.fullyExpandedType() is ConeUnionType
}
