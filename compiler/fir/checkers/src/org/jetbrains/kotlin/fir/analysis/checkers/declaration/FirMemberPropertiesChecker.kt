/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoCollector
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberPropertiesChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val memberPropertySymbols = declaration.declarations.filterIsInstance<FirProperty>().map { it.symbol }.toSet()
        val initializedInConstructor =
            mutableMapOf<FirPropertySymbol, EventOccurrencesRange>().withDefault { EventOccurrencesRange.ZERO }
        val initializedInInitOrOtherProperty =
            mutableMapOf<FirPropertySymbol, EventOccurrencesRange>().withDefault { EventOccurrencesRange.ZERO }

        // If all member properties have its own initializer, we don't need to collect property initialization info at all.
        if (memberPropertySymbols.any { !it.hasInitializer }) {
            collectPropertyInitialization(
                declaration,
                context.session,
                memberPropertySymbols,
                initializedInConstructor,
                initializedInInitOrOtherProperty
            )
        }

        val deadEnds = declaration.collectDeadEndDeclarations()
        var reachedDeadEnd = false
        for (innerDeclaration in declaration.declarations) {
            if (innerDeclaration is FirProperty) {
                val symbol = innerDeclaration.symbol
                val isInitialized =
                    innerDeclaration.initializer != null ||
                            initializedInConstructor.getValue(symbol).isDefinitelyVisited() ||
                            initializedInInitOrOtherProperty.getValue(symbol).isDefinitelyVisited()
                checkProperty(declaration, innerDeclaration, isInitialized, context, reporter, !reachedDeadEnd)
            }
            reachedDeadEnd = reachedDeadEnd || deadEnds.contains(innerDeclaration)
        }
    }

    private fun collectPropertyInitialization(
        klass: FirClass,
        session: FirSession,
        memberPropertySymbols: Set<FirPropertySymbol>,
        initializedInConstructor: MutableMap<FirPropertySymbol, EventOccurrencesRange>,
        initializedInInitOrOtherProperty: MutableMap<FirPropertySymbol, EventOccurrencesRange>
    ) {
        // A property is known to be initialized only if it is initialized
        //   1) with its own initializing expression;
        //   2) at every class constructor;
        //   3) at any of class's anonymous initializers; or
        //   4) at other property's initializing expression

        // 2) Property can be initialized at constructors. Since it's unknown what constructor will be used, the property can be determined
        // as initialized only if it is initialized at every constructor. We should consider a delegated constructor, e.g.,
        //   constructor() { x = ... }
        //   constructor(...): this() { ... }  // x will be initialized via this() delegation
        // We need to topologically sort constructors so that we can process delegated ones before the use sites.

        // 3) Property can be initialized at any of class's anonymous initializers (all of initializers will be executed), e.g.,
        //   init { x = ... }
        //   ...
        //   init { y = ... }

        // 4) Property can be initialized at other property's initializing expression too, e.g.,
        //   val initX = inlineMe { x = ... } // where inlineMe returns the value of the last expression of the lambda

        // To handle the delegated constructor call, we need a cache from constructor to (analyzed) property init info.
        val constructorToData =
            mutableMapOf<FirConstructorSymbol, PropertyInitializationInfo>().withDefault { PropertyInitializationInfo.EMPTY }

        fun collectInfoFromGraph(
            graph: ControlFlowGraph,
            map: MutableMap<FirPropertySymbol, EventOccurrencesRange>,
            acc: (EventOccurrencesRange, EventOccurrencesRange) -> EventOccurrencesRange,
            delegatedConstructor: FirConstructorSymbol? = null,
        ) {
            val delegatedInfo = delegatedConstructor?.let { constructorToData.getValue(it) } ?: PropertyInitializationInfo.EMPTY

            val data = PropertyInitializationInfoCollector(memberPropertySymbols).getData(graph)
            val infoAtExitNode = data[graph.exitNode]?.get(NormalPath) ?: PropertyInitializationInfo.EMPTY

            // NB: it's not [merge], which is conducted at merging points, such as loop condition or when conditions.
            // Rather, delegated constructor call is the predecessor of the current constructor call, so we should accumulate.
            val info = delegatedInfo.plus(infoAtExitNode)

            if (graph.declaration is FirConstructor) {
                constructorToData.putIfAbsent((graph.declaration as FirConstructor).symbol, info)
            }

            for (propertySymbol in memberPropertySymbols) {
                val item = map[propertySymbol]
                if (item != null) {
                    // Accumulation:
                    //   range join for class constructors, range plus for class's anonymous initializers and property initializations
                    map[propertySymbol] = acc.invoke(item, info[propertySymbol] ?: EventOccurrencesRange.ZERO)
                } else {
                    // Initial assignment.
                    // NB: we should not use `acc` here to not weaken ranges. For example, if we visit one and only constructor where
                    // a property of interest is correctly initialized (a.k.a. [EXACTLY_ONCE]), and if `acc` is ...Range::or,
                    // merging with the default [ZERO] makes the result [AT_MOST_ONCE], which will be regarded as uninitialized.
                    map[propertySymbol] = info[propertySymbol] ?: EventOccurrencesRange.ZERO
                }
            }
        }

        val constructorGraphs = klass.constructorsSortedByDelegation(session).mapNotNull {
            it.resolvedControlFlowGraphReference?.controlFlowGraph
        }
        for (graph in constructorGraphs) {
            collectInfoFromGraph(
                graph,
                initializedInConstructor,
                EventOccurrencesRange::or,
                (graph.declaration as? FirConstructor)?.symbol?.delegatedThisConstructor
            )
        }

        val initGraphs = klass.anonymousInitializers.mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }
        for (graph in initGraphs) {
            collectInfoFromGraph(graph, initializedInInitOrOtherProperty, EventOccurrencesRange::plus)
        }

        val propertyInitGraphs = memberPropertySymbols.mapNotNull { it.controlFlowGraphReference?.controlFlowGraph }
        for (graph in propertyInitGraphs) {
            collectInfoFromGraph(graph, initializedInInitOrOtherProperty, EventOccurrencesRange::plus)
        }
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

        withSuppressedDiagnostics(property, context) { ctx ->
            checkPropertyInitializer(
                containingDeclaration,
                property,
                modifierList,
                isInitialized,
                reporter,
                ctx,
                reachable
            )
            checkExpectDeclarationVisibilityAndBody(property, source, reporter, ctx)

            val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
            val isAbstract = property.isAbstract || hasAbstractModifier
            if (containingDeclaration.isInterface &&
                Visibilities.isPrivate(property.visibility) &&
                !isAbstract &&
                (property.getter == null || property.getter is FirDefaultPropertyAccessor)
            ) {
                property.source?.let {
                    reporter.reportOn(it, FirErrors.PRIVATE_PROPERTY_IN_INTERFACE, ctx)
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
                            ctx
                        )
                        return
                    }
                }
                property.initializer?.source?.let {
                    reporter.reportOn(it, FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER, ctx)
                }
                property.delegate?.source?.let {
                    reporter.reportOn(it, FirErrors.ABSTRACT_DELEGATED_PROPERTY, ctx)
                }
            }

            val hasOpenModifier = KtTokens.OPEN_KEYWORD in modifierList
            if (hasOpenModifier &&
                containingDeclaration.isInterface &&
                !hasAbstractModifier &&
                property.isAbstract &&
                !isInsideExpectClass(containingDeclaration, ctx)
            ) {
                property.source?.let {
                    reporter.reportOn(it, FirErrors.REDUNDANT_OPEN_IN_INTERFACE, ctx)
                }
            }
        }
    }

    private fun FirClass.collectDeadEndDeclarations(): Set<FirElement> {
        val controlFlowGraphReference = when (this) {
            is FirAnonymousObject -> this.controlFlowGraphReference
            is FirRegularClass -> this.controlFlowGraphReference
            else -> null
        }
        val cfg = controlFlowGraphReference?.controlFlowGraph ?: return emptySet()
        return cfg.exitNode.incomingEdges.keys
            .map { it.fir }
            .filter { it.isDeadEnd() }
            .toSet()
    }

    /**
     * The idea is to find presumably exit node is dead one
     * 1. in the case of block expression, it is BlockExitNode
     * 2. in other cases treat any dead node that leads to exitNode as evidence of deadness
     * This is all a workaround because ideally exit node itself should be dead if it is unreachable
     */
    private fun FirElement.isDeadEnd(): Boolean {
        val cfg = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return false
        cfg.exitNode.incomingEdges.keys.find { it is BlockExitNode }?.let {
            return it.isDead
        }

        return cfg.exitNode.incomingEdges.keys.any { it.isDead }
    }
}
