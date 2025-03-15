/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature.ProperUninitializedEnumEntryAccessAnalysis
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.VariableInitializationCheckProcessor
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwarePropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.cfa.util.emptyNormalPathInfo
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

object FirEnumEntryInitializationChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isEnumClass) return
        if (!context.languageVersionSettings.supportsFeature(ProperUninitializedEnumEntryAccessAnalysis)) return
        val enumEntries = declaration.declarations.filterIsInstance<FirEnumEntry>()
        if (enumEntries.isEmpty()) return
        val enumEntrySymbols = enumEntries.mapTo(mutableSetOf()) { it.symbol }
        checkClass(declaration, enumEntrySymbols, context, reporter)
        checkEnumEntries(enumEntries, context, reporter)
    }

    private fun checkClass(
        klass: FirClass,
        enumEntrySymbols: Set<FirEnumEntrySymbol>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val graph = klass.controlFlowGraphReference?.controlFlowGraph ?: return
        val data = EnumEntryInitializationInfoData(enumEntrySymbols, klass.symbol, graph)
        EnumEntryInitializationCheckProcessor.check(data, isForInitialization = true, context, reporter)
    }

    private fun checkEnumEntries(enumEntries: List<FirEnumEntry>, context: CheckerContext, reporter: DiagnosticReporter) {
        val enumEntrySymbols = enumEntries.mapTo(mutableSetOf()) { it.symbol }
        for (enumEntry in enumEntries) {
            val entryObject = (enumEntry.initializer as? FirAnonymousObjectExpression)?.anonymousObject ?: continue
            checkClass(entryObject, enumEntrySymbols, context, reporter)
            enumEntrySymbols.remove(enumEntry.symbol)
        }
    }
}

private class EnumEntryInitializationInfoData(
    override val properties: Set<FirEnumEntrySymbol>,
    override val receiver: FirBasedSymbol<*>,
    override val graph: ControlFlowGraph,
) : VariableInitializationInfoData() {
    override val conditionallyInitializedProperties: Set<FirVariableSymbol<*>> = emptySet()

    override fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo {
        return emptyNormalPathInfo()
    }
}

private object EnumEntryInitializationCheckProcessor : VariableInitializationCheckProcessor() {
    override fun filterProperties(data: VariableInitializationInfoData, isForInitialization: Boolean): Set<FirVariableSymbol<*>> {
        return data.properties
    }

    override fun VariableInitializationInfoData.reportCapturedInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {}

    override fun reportUninitializedVariable(
        reporter: DiagnosticReporter,
        node: QualifiedAccessNode,
        symbol: FirVariableSymbol<*>,
        context: CheckerContext,
    ) {
        require(symbol is FirEnumEntrySymbol)
        val source = node.fir.source
        reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_ENTRY, symbol, context)
    }

    override fun reportNonInlineMemberValInitialization(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {}

    override fun reportValReassignment(
        node: VariableAssignmentNode,
        symbol: FirVariableSymbol<*>,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {}

    override fun FirQualifiedAccessExpression.hasMatchingReceiver(data: VariableInitializationInfoData): Boolean {
        return true
    }
}
