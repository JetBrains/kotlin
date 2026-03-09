/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.fir.Locality
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.locality
import org.jetbrains.kotlin.fir.resolve.dfa.Domain
import org.jetbrains.kotlin.fir.resolve.dfa.DomainReference
import org.jetbrains.kotlin.fir.resolve.dfa.RealVariable
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithSubgraphs
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableDeclarationExitNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.utils.associateByNotNull
import kotlin.to

object FirLocalsChecker {
    typealias LeakedVariables = SetMultimap<FirBasedSymbol<*>, Pair<FirStatement, KtDiagnosticFactory1<FirBasedSymbol<*>>>>

    fun analyze(graph: ControlFlowGraph): LeakedVariables {
        val leaked = setMultimapOf<FirBasedSymbol<*>, Pair<FirStatement, KtDiagnosticFactory1<FirBasedSymbol<*>>>>()
        check(graph, listOf(Scope.Empty), allowTopMostReturn = false, leaked)
        return leaked
    }

    data class Scope(
        val localDomains: Map<Domain, RealVariable>,
        val localVariables: Set<RealVariable>,
    ) {
        companion object {
            val Empty: Scope = Scope(emptyMap(), emptySet())
        }
    }

    fun check(
        graph: ControlFlowGraph,
        scopes: List<Scope>,
        allowTopMostReturn: Boolean,
        leaked: LeakedVariables,
    ) {
        val declaration = graph.declaration
        val function = declaration as? FirCallableDeclaration

        val parameters = function?.parametersWithLocality.orEmpty()
        val parametersWithLocalContract = parameters.filter { it.second.hasLocalContract }.map { it.first }
        val parametersFromLocallyScoped = when {
            function?.isLocallyScoped == true -> parameters.map { it.first }.toSet()
            else -> emptySet()
        }
        val localParameters = (parametersWithLocalContract + parametersFromLocallyScoped)
            .map { it.realVariable() }
            .associateByNotNull(
                keySelector = { graph.enterNode.flow.getDomains(it).singleOrNull() },
                valueTransform = { it }
            )

        val topScope = scopes.first()
        val localDomains = topScope.localDomains + localParameters
        val localVariables = (topScope.localVariables + localParameters.values).toMutableSet()
        val updatedScopes = listOf(Scope(localDomains, localVariables)) + scopes.drop(1)

        for (node in graph.nodes) {
            if (node is VariableDeclarationExitNode) {
                localVariables.add(node.fir.symbol.realVariable())
            }

            if (node is CFGNodeWithSubgraphs<*>) {
                for (subGraph in node.subGraphs) {
                    val subDeclaration = subGraph.declaration
                    if (subDeclaration is FirClassLikeDeclaration || subDeclaration?.evaluatedInPlace != true || subDeclaration.isLocallyScoped) {
                        check(subGraph, listOf(Scope.Empty) + updatedScopes, allowTopMostReturn = false, leaked)
                    } else {
                        check(subGraph, updatedScopes, allowTopMostReturn = true, leaked)
                    }
                }
            }

            if (!node.flowInitialized) continue
            var isTopMost = true
            for ([scopeDomains, scopeVariables] in updatedScopes) {
                for ([parameterDomain, parameterVariable] in scopeDomains) {
                    val parameterReferences =
                        node.flow.getReferences(parameterDomain).filterIsInstance<DomainReference.WithStatement>()
                    val nonAccessReferences = parameterReferences.filter { it !is DomainReference.Access }

                    for (reference in parameterReferences) {
                        if (node.fir != reference.statement) continue

                        val ignore = when (reference) {
                            is DomainReference.WithVariable -> reference.variable in scopeVariables
                            is DomainReference.Join -> isTopMost && allowTopMostReturn
                            is DomainReference.Access -> isTopMost
                            else -> false
                        }
                        if (ignore) continue

                        // dig to find the smallest statement to report
                        var statementToReport = reference.statement
                        var referenceToReport: DomainReference.WithStatement = reference
                        while (referenceToReport is DomainReference.Join) {
                            statementToReport = referenceToReport.original ?: break
                            referenceToReport = nonAccessReferences.find { it.statement == referenceToReport.original } ?: break
                        }
                        if (referenceToReport is DomainReference.WithSecondaryStatement && referenceToReport.secondaryStatement != null) {
                            statementToReport = referenceToReport.secondaryStatement!!
                        }

                        val errorKind = when (referenceToReport) {
                            is DomainReference.Call -> FirErrors.LEAKED_LOCAL_THROUGH_CALL
                            is DomainReference.Access -> FirErrors.LEAKED_LOCAL_THROUGH_CAPTURE
                            else -> FirErrors.LEAKED_LOCAL
                        }
                        leaked.put(parameterVariable.symbol, statementToReport to errorKind)
                    }
                }
                isTopMost = false
            }
        }
    }
}

private fun FirCallableSymbol<*>.realVariable(): RealVariable =
    RealVariable(this, false, null, null, resolvedReturnType)

private val FirDeclaration.isLocallyScoped: Boolean
    get() = this is FirAnonymousFunction && isLocallyScoped == true

val FirCallableDeclaration.parametersWithLocality: List<Pair<FirCallableSymbol<*>, Locality>>
    get() = buildList {
        if (this@parametersWithLocality is FirFunction) valueParameters.forEach { add(it.symbol to it.locality) }
        // receiverParameter?.let { add(it.symbol to it.locality) }
        contextParameters.forEach { add(it.symbol to it.locality) }
    }
