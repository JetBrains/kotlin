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
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

object FirPropertyInitializationAnalyzer : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    override fun analyze(data: VariableInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        PropertyInitializationCheckProcessor.check(data, isForInitialization = false, context, reporter)
    }
}

val FirDeclaration.evaluatedInPlace: Boolean
    get() = when (this) {
        is FirAnonymousFunction -> invocationKind.isInPlace
        is FirAnonymousObject -> classKind != ClassKind.ENUM_ENTRY
        is FirConstructor -> true // child of class initialization graph
        is FirFunction, is FirClass -> false
        else -> true // property initializer, etc.
    }

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
    val hasImplicitBackingField = !hasExplicitBackingField && hasBackingField
    return when {
        this is FirSyntheticPropertySymbol -> false
        isForInitialization -> hasDelegate || hasImplicitBackingField
        else -> !hasInitializer && hasImplicitBackingField && fir.isCatchParameter != true
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

    override fun VariableInitializationInfoData.reportCapturedInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        require(symbol is FirPropertySymbol)
        val capturedInitializationError = if (receiver != null)
            FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION
        else
            FirErrors.CAPTURED_VAL_INITIALIZATION

        reporter.reportOn(node.fir.lValue.source, capturedInitializationError, symbol, context)
    }

    override fun FirQualifiedAccessExpression.hasMatchingReceiver(data: VariableInitializationInfoData): Boolean {
        val expression = dispatchReceiver?.unwrapSmartcastExpression()
        return (expression as? FirThisReceiverExpression)?.calleeReference?.boundSymbol == data.receiver ||
                (expression as? FirResolvedQualifier)?.symbol == data.receiver
    }

    override fun reportUninitializedVariable(
        reporter: DiagnosticReporter,
        node: QualifiedAccessNode,
        symbol: FirVariableSymbol<*>,
        context: CheckerContext,
    ) {
        require(symbol is FirPropertySymbol)
        reporter.reportOn(node.fir.source, FirErrors.UNINITIALIZED_VARIABLE, symbol, context)
    }

    override fun reportNonInlineMemberValInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        require(symbol is FirPropertySymbol)
        reporter.reportOn(node.fir.lValue.source, FirErrors.NON_INLINE_MEMBER_VAL_INITIALIZATION, symbol, context)
    }

    override fun reportValReassignment(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
    }
}
