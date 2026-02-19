/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkMissingDependencySuperTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.unwrapToSimpleTypeUsingLowerBound
import org.jetbrains.kotlin.utils.SmartSet

/**
 * @see org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
 */
object FirMissingDependencySupertypeInQualifiedAccessExpressionsChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val source = expression.source

        val symbol = expression.calleeReference.toResolvedCallableSymbol()
        if (symbol == null) {
            val receiverType = expression.explicitReceiver
                ?.resolvedType
                ?.unwrapToSimpleTypeUsingLowerBound()
                ?.fullyExpandedType()

            checkMissingDependencySuperTypes(receiverType, source)
            return
        }

        val checkedSymbols = SmartSet.create<FirBasedSymbol<*>>()

        val dispatchReceiverSymbol = expression.dispatchReceiver?.resolvedType?.toSymbol()
        val missingSuperTypes = checkMissingDependencySuperTypes(dispatchReceiverSymbol, source, isEagerCheck = false)
        dispatchReceiverSymbol?.let(checkedSymbols::add)

        val lazySupertypesUnresolvedByDefault = symbol is FirConstructorSymbol || symbol is FirAnonymousFunctionSymbol
        val isEagerCheck = lazySupertypesUnresolvedByDefault || missingSuperTypes

        val ownerSymbol = symbol.getOwnerLookupTag()?.toSymbol()
        if (ownerSymbol != null && checkedSymbols.add(ownerSymbol)) {
            checkMissingDependencySuperTypes(ownerSymbol, source, isEagerCheck)
        }

        val receiverSymbol = symbol.resolvedReceiverType?.toSymbol()
        if (receiverSymbol != null && checkedSymbols.add(receiverSymbol)) {
            checkMissingDependencySuperTypes(receiverSymbol, source, isEagerCheck)
        }
    }
}
