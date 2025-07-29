/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(data: VariableInitializationInfoData) {
        PropertyInitializationCheckProcessor.check(data, isForInitialization = false)
    }
}

val FirDeclaration.evaluatedInPlace: Boolean
    get() = symbol.evaluatedInPlace

val FirBasedSymbol<*>.evaluatedInPlace: Boolean
    get() = when (this) {
        is FirAnonymousFunctionSymbol -> invocationKind.isInPlace
        is FirAnonymousObjectSymbol -> classKind != ClassKind.ENUM_ENTRY
        is FirConstructorSymbol -> true // child of class initialization graph
        is FirFunctionSymbol, is FirClassSymbol -> false
        else -> true // property initializer, etc.
    }

fun ControlFlowGraph.nearestNonInPlaceGraph(): ControlFlowGraph =
    if (declaration?.evaluatedInPlace == true)
        enterNode.previousNodes.firstOrNull()?.owner?.nearestNonInPlaceGraph() ?: this
    else this

/**
 * [isForInitialization] means that caller is interested in member property in the scope
 * of file or class initialization section. In this case the fact that property has
 * initializer does not mean that it's safe to access this property in any place:
 *
 * ```
 * class A {
 *     val b = a // a is not initialized here
 *     val a = 10
 *     val c = a // but initialized here
 * }
 * ```
 */
@OptIn(SymbolInternals::class)
fun FirPropertySymbol.requiresInitialization(isForInitialization: Boolean): Boolean {
    return when {
        this is FirSyntheticPropertySymbol -> false
        isForInitialization -> hasDelegate || hasBackingField
        else -> hasBackingField && !hasInitializer && backingFieldSymbol?.resolvedInitializer == null && fir.isCatchParameter != true
    }
}


object PropertyInitializationCheckProcessor : VariableInitializationCheckProcessor() {
    override fun filterProperties(
        data: VariableInitializationInfoData,
        isForInitialization: Boolean
    ): Set<FirVariableSymbol<*>> {
        // If a property has an initializer (or does not need one), then any reads are OK while any writes are OK
        // if it's a `var` and bad if it's a `val`. `FirReassignmentAndInvisibleSetterChecker` does this without a CFG.
        return data.properties.filterTo(mutableSetOf()) {
            require(it is FirPropertySymbol)
            it.requiresInitialization(isForInitialization) || it in data.conditionallyInitializedProperties
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun VariableInitializationInfoData.reportCapturedInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
    ) {
        require(symbol is FirPropertySymbol)
        val capturedInitializationError = if (receiver != null)
            FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION
        else
            FirErrors.CAPTURED_VAL_INITIALIZATION

        reporter.reportOn(node.fir.lValue.source, capturedInitializationError, symbol)
    }

    override fun FirQualifiedAccessExpression.hasMatchingReceiver(data: VariableInitializationInfoData): Boolean {
        val expression = dispatchReceiver?.unwrapSmartcastExpression()
        return (expression as? FirThisReceiverExpression)?.calleeReference?.boundSymbol == data.receiver ||
                (expression as? FirResolvedQualifier)?.symbol == data.receiver
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun reportUninitializedVariable(
        expression: FirQualifiedAccessExpression,
        symbol: FirVariableSymbol<*>,
    ) {
        require(symbol is FirPropertySymbol)
        reporter.reportOn(expression.source, FirErrors.UNINITIALIZED_VARIABLE, symbol)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun reportNonInlineMemberValInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
    ) {
        require(symbol is FirPropertySymbol)
        reporter.reportOn(node.fir.lValue.source, FirErrors.NON_INLINE_MEMBER_VAL_INITIALIZATION, symbol)
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun reportValReassignment(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
    ) {
        reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol)
    }
}
