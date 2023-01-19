/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberPropertiesChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val info = declaration.collectInitializationInfo(context, reporter)
        var reachedDeadEnd =
            (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph?.enterNode?.isDead == true
        for (innerDeclaration in declaration.declarations) {
            if (innerDeclaration is FirProperty) {
                val symbol = innerDeclaration.symbol
                val isInitialized = innerDeclaration.initializer != null || info?.get(symbol)?.isDefinitelyVisited() == true
                checkProperty(declaration, innerDeclaration, isInitialized, context, reporter, !reachedDeadEnd)
            }
            // Can't just look at each property's graph's enterNode because they may have no graph if there is no initializer.
            reachedDeadEnd = reachedDeadEnd ||
                    (innerDeclaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph?.exitNode?.isDead == true
        }
    }

    private fun FirClass.collectInitializationInfo(context: CheckerContext, reporter: DiagnosticReporter): PropertyInitializationInfo? {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null
        val memberPropertySymbols = declarations.mapNotNullTo(mutableSetOf()) {
            (it.symbol as? FirPropertySymbol)?.takeIf { symbol -> symbol.requiresInitialization }
        }
        if (memberPropertySymbols.isEmpty()) return null
        // TODO: this also visits non-constructor member functions...
        // TODO: merge with `FirPropertyInitializationAnalyzer` for fewer passes.
        val data = PropertyInitializationInfoData(memberPropertySymbols, symbol, graph)
        graph.checkPropertyAccesses(memberPropertySymbols, symbol, context, reporter, data)
        return data.getValue(graph.exitNode)[NormalPath]
    }

    private fun checkProperty(
        containingDeclaration: FirClass,
        property: FirProperty,
        isInitialized: Boolean,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reachable: Boolean
    ) {
        val source = property.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = property.source.getModifierList()

        checkPropertyInitializer(
            containingDeclaration,
            property,
            modifierList,
            isInitialized,
            reporter,
            context,
            reachable
        )
        checkExpectDeclarationVisibilityAndBody(property, source, reporter, context)

        val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
        val isAbstract = property.isAbstract || hasAbstractModifier
        if (containingDeclaration.isInterface &&
            Visibilities.isPrivate(property.visibility) &&
            !isAbstract &&
            (property.getter == null || property.getter is FirDefaultPropertyAccessor)
        ) {
            property.source?.let {
                reporter.reportOn(it, FirErrors.PRIVATE_PROPERTY_IN_INTERFACE, context)
            }
        }

        if (isAbstract) {
            if (containingDeclaration is FirRegularClass && !containingDeclaration.canHaveAbstractDeclaration) {
                property.source?.let {
                    reporter.reportOn(
                        it,
                        FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS,
                        property.symbol,
                        containingDeclaration.symbol,
                        context
                    )
                    return
                }
            }
            property.initializer?.source?.let {
                reporter.reportOn(it, FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER, context)
            }
            property.delegate?.source?.let {
                reporter.reportOn(it, FirErrors.ABSTRACT_DELEGATED_PROPERTY, context)
            }
        }

        val hasOpenModifier = KtTokens.OPEN_KEYWORD in modifierList
        if (hasOpenModifier &&
            containingDeclaration.isInterface &&
            !hasAbstractModifier &&
            property.isAbstract &&
            !isInsideExpectClass(containingDeclaration, context)
        ) {
            property.source?.let {
                reporter.reportOn(it, FirErrors.REDUNDANT_OPEN_IN_INTERFACE, context)
            }
        }
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

@OptIn(SymbolInternals::class)
val FirPropertySymbol.requiresInitialization: Boolean
    get() = this !is FirSyntheticPropertySymbol && !hasInitializer && !hasExplicitBackingField &&
            hasBackingField && fir.isCatchParameter != true

fun ControlFlowGraph.checkPropertyAccesses(
    properties: Set<FirPropertySymbol>,
    receiver: FirBasedSymbol<*>?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    data: PropertyInitializationInfoData
) {
    // NOTE: assert(properties.all { it.requiresInitialization })
    // If a property has an initializer (or does not need one), then any reads are OK while any writes are OK
    // if it's a `var` and bad if it's a `val`. `FirReassignmentAndInvisibleSetterChecker` does this without a CFG.
    if (properties.isEmpty()) return

    checkPropertyAccesses(properties, receiver, context, reporter, data, null, mutableMapOf())
}

@OptIn(SymbolInternals::class)
private fun ControlFlowGraph.checkPropertyAccesses(
    properties: Set<FirPropertySymbol>,
    receiver: FirBasedSymbol<*>?,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    data: PropertyInitializationInfoData,
    scope: FirDeclaration?,
    scopes: MutableMap<FirPropertySymbol, FirDeclaration?>,
) {
    fun FirQualifiedAccess.hasCorrectReceiver() =
        (dispatchReceiver as? FirThisReceiverExpression)?.calleeReference?.boundSymbol == receiver

    for (node in nodes) {
        when {
            // TODO: `node.isUnion` - f({ x = 1 }, { x = 2 }) - which to report?
            //  Also this is currently indistinguishable from x = 1; f({}, {}).

            node is VariableDeclarationNode -> {
                val symbol = node.fir.symbol
                if (scope != null && receiver == null && node.fir.isVal && symbol in properties) {
                    // It's OK to initialize this variable from a nested called-in-place function, but not from
                    // a non-called-in-place function or a non-anonymous-object class initializer.
                    scopes[symbol] = scope
                }
            }

            node is VariableAssignmentNode -> {
                val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: continue
                if (!symbol.fir.isVal || !node.fir.hasCorrectReceiver() || symbol !in properties) continue

                if (scope != scopes[symbol]) {
                    val error = if (receiver != null)
                        FirErrors.CAPTURED_MEMBER_VAL_INITIALIZATION
                    else
                        FirErrors.CAPTURED_VAL_INITIALIZATION
                    reporter.reportOn(node.fir.lValue.source, error, symbol, context)
                } else if (data.getValue(node).values.any { it[symbol]?.canBeRevisited() == true }) {
                    reporter.reportOn(node.fir.lValue.source, FirErrors.VAL_REASSIGNMENT, symbol, context)
                }
            }

            node is QualifiedAccessNode -> {
                val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: continue
                if (!symbol.isLateInit && node.fir.hasCorrectReceiver() && symbol in properties &&
                    data.getValue(node).values.any { it[symbol]?.isDefinitelyVisited() != true }
                ) {
                    reporter.reportOn(node.fir.source, FirErrors.UNINITIALIZED_VARIABLE, symbol, context)
                }
            }

            // In the class case, subgraphs of the exit node are member functions, which are considered to not
            // be part of initialization, so any val is considered to be initialized there and the CFG is not
            // needed. The errors on reassignments will be emitted by `FirReassignmentAndInvisibleSetterChecker`.
            node is CFGNodeWithSubgraphs<*> && (receiver == null || node !== exitNode) -> {
                for (subGraph in node.subGraphs) {
                    val newScope = subGraph.declaration?.takeIf { !it.evaluatedInPlace } ?: scope
                    subGraph.checkPropertyAccesses(properties, receiver, context, reporter, data, newScope, scopes)
                }
            }
        }
    }
}
