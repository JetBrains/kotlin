/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.PropertyInitializationCheckProcessor
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfo
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.canHaveAbstractDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberPropertiesChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val info = declaration.collectInitializationInfo(context, reporter)
        var reachedDeadEnd =
            (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph?.enterNode?.isDead == true
        // Order is important here, so we have to use declarations directly
        @OptIn(DirectDeclarationsAccess::class)
        for (innerDeclaration in declaration.declarations) {
            if (innerDeclaration is FirProperty) {
                val symbol = innerDeclaration.symbol
                val isDefinitelyAssignedInConstructor = info?.get(symbol)?.isDefinitelyVisited() == true
                checkProperty(declaration, symbol, isDefinitelyAssignedInConstructor, context, reporter, !reachedDeadEnd)
            }
            // Can't just look at each property's graph's enterNode because they may have no graph if there is no initializer.
            reachedDeadEnd = reachedDeadEnd ||
                    (innerDeclaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph?.exitNode?.isDead == true
        }
    }

    private fun FirClass.collectInitializationInfo(context: CheckerContext, reporter: DiagnosticReporter): VariableInitializationInfo? {
        val graph = (this as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return null
        val memberPropertySymbols = mutableSetOf<FirPropertySymbol>()
        symbol.processAllDeclaredCallables(context.session) { symbol ->
            if (symbol is FirPropertySymbol && symbol.requiresInitialization(isForInitialization = true)) {
                memberPropertySymbols += symbol
            }
        }
        if (memberPropertySymbols.isEmpty()) return null
        // TODO, KT-59803: merge with `FirPropertyInitializationAnalyzer` for fewer passes.
        val data = PropertyInitializationInfoData(memberPropertySymbols, conditionallyInitializedProperties = emptySet(), symbol, graph)
        PropertyInitializationCheckProcessor.check(data, isForInitialization = true, context, reporter)
        return data.getValue(graph.exitNode)[NormalPath]
    }
}

internal fun checkProperty(
    containingDeclaration: FirClass?,
    propertySymbol: FirPropertySymbol,
    isDefinitelyAssigned: Boolean,
    context: CheckerContext,
    reporter: DiagnosticReporter,
    reachable: Boolean,
) {
    if (propertySymbol is FirErrorPropertySymbol) {
        // We need to report diagnostics on a KtProperty, but FirErrorProperty may be backed by a KtDestructuringDeclaration.
        return
    }
    val source = propertySymbol.source ?: return
    if (source.kind is KtFakeSourceElementKind) return
    // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
    // So, our source of truth should be the full modifier list retrieved from the source.
    val modifierList = propertySymbol.source.getModifierList()

    checkPropertyInitializer(
        containingDeclaration,
        propertySymbol,
        modifierList,
        isDefinitelyAssigned,
        reporter,
        context,
        reachable
    )

    if (containingDeclaration != null) {
        val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
        val isAbstract = propertySymbol.isAbstract || hasAbstractModifier
        if (containingDeclaration.isInterface &&
            Visibilities.isPrivate(propertySymbol.visibility) &&
            !isAbstract &&
            propertySymbol.getterSymbol?.isDefault != false
        ) {
            propertySymbol.source?.let {
                reporter.reportOn(it, FirErrors.PRIVATE_PROPERTY_IN_INTERFACE, context)
            }
        }

        if (isAbstract) {
            if (containingDeclaration is FirRegularClass && !containingDeclaration.canHaveAbstractDeclaration) {
                propertySymbol.source?.let {
                    reporter.reportOn(
                        it,
                        FirErrors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS,
                        propertySymbol,
                        containingDeclaration.symbol,
                        context
                    )
                    return
                }
            }
            propertySymbol.initializerSource?.let {
                reporter.reportOn(it, FirErrors.ABSTRACT_PROPERTY_WITH_INITIALIZER, context)
            }
            propertySymbol.delegate?.source?.let {
                reporter.reportOn(it, FirErrors.ABSTRACT_DELEGATED_PROPERTY, context)
            }
        }

        val hasOpenModifier = KtTokens.OPEN_KEYWORD in modifierList
        if (hasOpenModifier &&
            containingDeclaration.isInterface &&
            !hasAbstractModifier &&
            propertySymbol.isAbstract &&
            !isInsideExpectClass(containingDeclaration, context)
        ) {
            propertySymbol.source?.let {
                reporter.reportOn(it, FirErrors.REDUNDANT_OPEN_IN_INTERFACE, context)
            }
        }
    }
}
