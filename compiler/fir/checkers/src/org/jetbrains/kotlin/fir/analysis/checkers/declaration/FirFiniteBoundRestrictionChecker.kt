/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.utils.DFS

/**
 * @see org.jetbrains.kotlin.resolve.FiniteBoundRestrictionChecker
 */
object FirFiniteBoundRestrictionChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.typeParameters.isEmpty()) return

        // TODO, KT-61103: Improve documentation on finite bounds validation, especially B-closure and constituent types definition.
        // For every projection type argument A in every generic type B<…> in the set of constituent types
        // of every type in the B-closure of the set of declared upper bounds of every type parameter T add an
        // edge from T to U, where U is the type parameter of the declaration of B<…> corresponding to the type argument A.
        // It is a compile-time error if the graph G has a cycle.
        val edges = buildTypeEdges(declaration, context.session)

        val problemNodes = edges.keys.filterTo(mutableSetOf()) { isInCycle(it, edges) }
        if (problemNodes.isEmpty()) return

        for (ref in declaration.typeParameters) {
            if (problemNodes.remove(ref.toConeType())) {
                reporter.reportOn(ref.source, FirErrors.FINITE_BOUNDS_VIOLATION, context)
                return
            }
        }

        val problemSymbols = problemNodes.mapNotNullTo(mutableSetOf()) { it.toSymbol(context.session) as? FirTypeParameterSymbol }
        if (problemSymbols.any { it.source != null }) return

        val containers = problemSymbols.map { it.containingDeclarationSymbol }
        reporter.reportOn(declaration.source, FirErrors.FINITE_BOUNDS_VIOLATION_IN_JAVA, containers, context)
    }

    private fun buildTypeEdges(declaration: FirRegularClass, session: FirSession): Map<ConeKotlinType, Set<ConeKotlinType>> {
        val edges = mutableMapOf<ConeKotlinType, MutableSet<ConeKotlinType>>()

        val visitedSymbols = mutableSetOf<FirClassifierSymbol<*>>()
        fun visit(coneType: ConeKotlinType) {
            val constituentTypes = mutableSetOf<ConeKotlinType>()
            for (type in coneType.collectUpperBounds()) {
                type.forEachType { constituentTypes.add(it) }
            }

            for (type in constituentTypes) {
                val symbol = type.toSymbol(session)
                val parameters = symbol?.typeParameterSymbols ?: continue

                if (visitedSymbols.add(symbol)) {
                    parameters.forEach { visit(it.toConeType()) }
                }
                if (parameters.size != type.typeArguments.size) continue

                for (i in parameters.indices) {
                    if (type.typeArguments[i].kind != ProjectionKind.INVARIANT) {
                        val parameter = parameters[i].toConeType()
                        edges.getOrPut(coneType) { mutableSetOf() }.add(parameter)
                        edges.getOrPut(parameter) { mutableSetOf() }
                    }
                }
            }
        }

        declaration.typeParameters.forEach { visit(it.toConeType()) }

        return edges
    }

    private fun isInCycle(start: ConeKotlinType, edges: Map<ConeKotlinType, Set<ConeKotlinType>>): Boolean {
        var containsCycle = false

        val dfsNeighbors = DFS.Neighbors<ConeKotlinType> { edges[it] ?: emptyList() }

        val dfsVisited = object : DFS.VisitedWithSet<ConeKotlinType>() {
            override fun checkAndMarkVisited(current: ConeKotlinType): Boolean {
                val added = super.checkAndMarkVisited(current)
                if (!added && current == start) {
                    containsCycle = true
                }
                return added
            }
        }

        val dfsHandler = object : DFS.AbstractNodeHandler<ConeKotlinType, Unit>() {
            override fun result() {}
        }

        DFS.dfs(listOf(start), dfsNeighbors, dfsVisited, dfsHandler)

        return containsCycle
    }
}
