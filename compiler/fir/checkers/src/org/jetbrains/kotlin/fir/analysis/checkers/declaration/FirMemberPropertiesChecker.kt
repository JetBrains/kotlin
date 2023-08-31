/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.checkPropertyAccesses
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
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
                val isDefinitelyAssignedInConstructor = info?.get(symbol)?.isDefinitelyVisited() == true
                checkProperty(declaration, innerDeclaration, isDefinitelyAssignedInConstructor, context, reporter, !reachedDeadEnd)
            }
            // Can't just look at each property's graph's enterNode because they may have no graph if there is no initializer.
            reachedDeadEnd = reachedDeadEnd ||
                    (innerDeclaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph?.exitNode?.isDead == true
        }
    }

    private fun FirClass.collectInitializationInfo(context: CheckerContext, reporter: DiagnosticReporter): PropertyInitializationInfo? {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null
        val memberPropertySymbols = declarations.mapNotNullTo(mutableSetOf()) {
            (it.symbol as? FirPropertySymbol)?.takeIf { symbol -> symbol.requiresInitialization(isForInitialization = true) }
        }
        if (memberPropertySymbols.isEmpty()) return null
        // TODO, KT-59803: merge with `FirPropertyInitializationAnalyzer` for fewer passes.
        val data = PropertyInitializationInfoData(memberPropertySymbols, symbol, graph)
        data.checkPropertyAccesses(isForInitialization = true, context, reporter)
        return data.getValue(graph.exitNode)[NormalPath]
    }
}

internal fun checkProperty(
    containingDeclaration: FirClass?,
    property: FirProperty,
    isDefinitelyAssigned: Boolean,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    reachable: Boolean,
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
        isDefinitelyAssigned,
        reporter,
        context,
        reachable
    )

    if (containingDeclaration != null) {
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
