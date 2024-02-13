/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
 */
object FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.source

        val symbol = expression.calleeReference.toResolvedCallableSymbol()
        if (symbol == null) {
            val receiverType = expression.explicitReceiver?.resolvedType
                ?.lowerBoundIfFlexible()?.originalIfDefinitelyNotNullable()?.fullyExpandedType(context.session)
            checkMissingDependencySuperTypes(receiverType, source, reporter, context)
            return
        }

        val missingSupertype = checkMissingDependencySuperTypes(symbol.dispatchReceiverType, source, reporter, context)

        val eagerChecksAllowed = context.languageVersionSettings.getFlag(AnalysisFlags.extendedCompilerChecks)
        val unresolvedLazySupertypesByDefault = symbol is FirConstructorSymbol || symbol is FirAnonymousFunctionSymbol

        if (eagerChecksAllowed || !unresolvedLazySupertypesByDefault && !missingSupertype) {
            checkMissingDependencySuperTypes(symbol.getOwnerLookupTag()?.toSymbol(context.session), source, reporter, context)
            checkMissingDependencySuperTypes(symbol.resolvedReceiverTypeRef?.coneTypeOrNull, source, reporter, context)
        }
    }
}
