/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isEnumEntry
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.allReceiverExpressions
import org.jetbrains.kotlin.fir.expressions.toReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirEnumCompanionInEnumConstructorCallChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val enumClass = when (declaration.classKind) {
            ClassKind.ENUM_CLASS -> declaration as FirRegularClass
            ClassKind.ENUM_ENTRY -> context.containingDeclarations.lastIsInstanceOrNull()
            else -> null
        } ?: return
        val companionOfEnum = enumClass.companionObjectSymbol ?: return
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        analyzeGraph(graph, companionOfEnum, context, reporter)
        if (declaration.classKind.isEnumEntry) {
            val constructor = declaration.declarations.firstIsInstanceOrNull<FirPrimaryConstructor>()
            val constructorGraph = constructor?.controlFlowGraphReference?.controlFlowGraph
            if (constructorGraph != null) {
                analyzeGraph(constructorGraph, companionOfEnum, context, reporter)
            }
        }
    }

    private fun analyzeGraph(
        graph: ControlFlowGraph,
        companionSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (node in graph.nodes) {
            if (node is CFGNodeWithSubgraphs) {
                for (subGraph in node.subGraphs) {
                    when (subGraph.kind) {
                        ControlFlowGraph.Kind.AnonymousFunctionCalledInPlace,
                        ControlFlowGraph.Kind.PropertyInitializer,
                        ControlFlowGraph.Kind.ClassInitializer -> analyzeGraph(subGraph, companionSymbol, context, reporter)
                        ControlFlowGraph.Kind.Class -> {
                            if (subGraph.declaration is FirAnonymousObject) {
                                analyzeGraph(subGraph, companionSymbol, context, reporter)
                            }
                        }
                        else -> {}
                    }
                }
            }
            val qualifiedAccess = when (node) {
                is QualifiedAccessNode -> node.fir
                is FunctionCallNode -> node.fir
                else -> continue
            }
            val matchingReceiver = qualifiedAccess.allReceiverExpressions
                .firstOrNull { it.getClassSymbol(context.session) == companionSymbol }
            if (matchingReceiver != null) {
                reporter.reportOn(
                    matchingReceiver.source ?: qualifiedAccess.source,
                    FirErrors.UNINITIALIZED_ENUM_COMPANION,
                    companionSymbol,
                    context
                )
            }
        }
    }

    private fun FirExpression.getClassSymbol(session: FirSession): FirRegularClassSymbol? {
        return when (this) {
            is FirResolvedQualifier -> {
                this.resolvedType.toRegularClassSymbol(session)
            }
            else -> (this.toReference() as? FirThisReference)?.boundSymbol
        } as? FirRegularClassSymbol
    }
}
