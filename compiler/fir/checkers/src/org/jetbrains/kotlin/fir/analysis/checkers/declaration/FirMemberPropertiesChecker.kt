/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoCollector
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberPropertiesChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val info = declaration.collectInitializationInfo()
        val deadEnds = declaration.collectDeadEndDeclarations()
        var reachedDeadEnd = false
        for (innerDeclaration in declaration.declarations) {
            if (innerDeclaration is FirProperty) {
                val symbol = innerDeclaration.symbol
                val isInitialized = innerDeclaration.initializer != null || info?.get(symbol)?.isDefinitelyVisited() == true
                checkProperty(declaration, innerDeclaration, isInitialized, context, reporter, !reachedDeadEnd)
            }
            reachedDeadEnd = reachedDeadEnd || deadEnds.contains(innerDeclaration)
        }
    }

    private fun FirClass.collectInitializationInfo(): PropertyInitializationInfo? {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null
        val memberPropertySymbols = declarations.mapNotNullTo(mutableSetOf()) {
            (it as? FirProperty)?.takeIf { fir -> fir.initializer == null }?.symbol
        }
        if (memberPropertySymbols.isEmpty()) return null
        // TODO: this also visits non-constructor member functions...
        // TODO: also use FirPropertyInitializationAnalyzer.PropertyReporter to report reassignments or use-before-initialization
        return PropertyInitializationInfoCollector(memberPropertySymbols, symbol).getData(graph)[graph.exitNode]?.get(NormalPath)
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

    private fun FirClass.collectDeadEndDeclarations(): Set<FirElement> {
        val controlFlowGraphReference = when (this) {
            is FirAnonymousObject -> this.controlFlowGraphReference
            is FirRegularClass -> this.controlFlowGraphReference
            else -> null
        }
        val cfg = controlFlowGraphReference?.controlFlowGraph ?: return emptySet()
        return cfg.exitNode.previousNodes
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
        return cfg.exitNode.previousNodes.find { it is BlockExitNode }?.isDead ?: cfg.exitNode.previousNodes.any { it.isDead }
    }
}
