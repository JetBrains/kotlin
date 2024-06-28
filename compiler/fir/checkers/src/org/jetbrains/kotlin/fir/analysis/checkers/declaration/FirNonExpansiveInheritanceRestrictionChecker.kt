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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * @see org.jetbrains.kotlin.resolve.NonExpansiveInheritanceRestrictionChecker
 */
object FirNonExpansiveInheritanceRestrictionChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.typeParameters.isEmpty()) return

        val graph = buildTypeGraph(declaration, context.session)

        val edgesInCycles = graph.expansiveEdges.filterTo(SmartSet.create()) { graph.isEdgeInCycle(it) }
        if (edgesInCycles.isEmpty()) return

        val problemNodes = edgesInCycles.flatMapTo(mutableSetOf()) { listOf(it.from, it.to) }

        for (ref in declaration.typeParameters) {
            if (problemNodes.remove(TypeParameterNode(declaration.symbol, ref.symbol))) {
                reporter.reportOn(ref.source ?: declaration.source, FirErrors.EXPANSIVE_INHERITANCE, context)
                return
            }
        }

        if (problemNodes.any { it.typeParameter.source != null }) return

        val containers = problemNodes.map { it.container }
        reporter.reportOn(declaration.source, FirErrors.EXPANSIVE_INHERITANCE_IN_JAVA, containers, context)
    }

    private fun buildTypeGraph(
        declaration: FirRegularClass,
        session: FirSession,
    ): Graph<TypeParameterNode> {
        // K1 treats type parameters inherited within inner classes from outer classes as being
        // from different classes. However, in K2, the symbol for these type parameters are the
        // same. So we have to store the pair of class symbol and type parameter symbol, so we do
        // not report errors on inherited type parameters from outer classes.
        val graph = Graph<TypeParameterNode>()

        fun addEdges(
            typeParameters: List<FirTypeParameterSymbol>,
            constituentTypes: Set<ConeKotlinType>,
            symbol: FirRegularClassSymbol,
            constituentTypeSymbol: FirRegularClassSymbol,
            constituentTypeParameterSymbol: FirTypeParameterSymbol,
            expansive: Boolean,
        ) {
            for (typeParameter in typeParameters) {
                if (typeParameter.toConeType(isNullable = false) in constituentTypes ||
                    typeParameter.toConeType(isNullable = true) in constituentTypes
                ) {
                    graph.addEdge(
                        from = TypeParameterNode(symbol, typeParameter),
                        to = TypeParameterNode(constituentTypeSymbol, constituentTypeParameterSymbol),
                        expansive = expansive,
                    )
                }
            }
        }

        val visitedSymbols = SmartSet.create<FirClassifierSymbol<*>>()
        fun visit(symbol: FirRegularClassSymbol) {
            val typeParameters = symbol.typeParameterSymbols
            if (typeParameters.isEmpty()) return

            // For each type parameter T, let ST be the set of all constituent types of all immediate supertypes of the owner of T.
            // If T appears as a constituent type of a simple type argument A in a generic type in ST, add an edge from T
            // to U, where U is the type parameter corresponding to A. The edge is non-expansive if A has the form T or T?,
            // the edge is expansive otherwise.
            for (constituentType in symbol.resolvedSuperTypes.flatMap { it.constituentTypes() }) {
                val constituentTypeSymbol = constituentType.toRegularClassSymbol(session) ?: continue
                if (visitedSymbols.add(constituentTypeSymbol)) visit(constituentTypeSymbol)

                val parameters = constituentTypeSymbol.typeParameterSymbols
                val arguments = constituentType.typeArguments.asList()
                if (parameters.size != arguments.size) continue
                val substitutor = runIf(arguments.any { it.kind != ProjectionKind.INVARIANT }) {
                    substitutorByType(parameters, arguments, session)
                }

                for ((i, typeProjection) in arguments.withIndex()) {
                    val constituentTypeParameterSymbol = parameters[i]
                    if (typeProjection.kind == ProjectionKind.INVARIANT) {
                        val constituents = typeProjection.type!!.constituentTypes()

                        addEdges(
                            typeParameters = typeParameters,
                            constituentTypes = constituents,
                            symbol = symbol,
                            constituentTypeSymbol = constituentTypeSymbol,
                            constituentTypeParameterSymbol = constituentTypeParameterSymbol,
                            expansive = typeProjection.type!!.unwrapLowerBound() !is ConeTypeParameterType,
                        )
                    } else {
                        // Furthermore, if T appears as a constituent type of an element of the B-closure of the set of lower and
                        // upper bounds of a skolem type variable Q in a skolemization of a projected generic type in ST, add an
                        // expanding edge from T to V, where V is the type parameter corresponding to Q.
                        val bounds = SmartSet.create<ConeKotlinType>()
                        constituentTypeParameterSymbol.resolvedBounds.mapNotNullTo(bounds) { substitutor!!.substituteOrNull(it.type) }
                        typeProjection.type?.let(bounds::add)
                        val boundClosure = bounds.flatMapTo(SmartSet.create()) { it.collectUpperBounds() }

                        addEdges(
                            typeParameters = typeParameters,
                            constituentTypes = boundClosure.flatMapTo(SmartSet.create()) { it.constituentTypes() },
                            symbol = symbol,
                            constituentTypeSymbol = constituentTypeSymbol,
                            constituentTypeParameterSymbol = constituentTypeParameterSymbol,
                            expansive = true,
                        )
                    }
                }
            }
        }

        visitedSymbols.add(declaration.symbol)
        visit(declaration.symbol)

        return graph
    }

    private data class TypeParameterNode(
        val container: FirRegularClassSymbol,
        val typeParameter: FirTypeParameterSymbol,
    )

    private data class ExpansiveEdge<out T>(val from: T, val to: T)

    private class Graph<T> {
        val expansiveEdges = SmartSet.create<ExpansiveEdge<T>>()
        private val edgeLists = mutableMapOf<T, MutableSet<T>>()

        fun addEdge(from: T, to: T, expansive: Boolean = false) {
            edgeLists.getOrPut(from) { SmartSet.create() }.add(to)
            if (expansive) {
                expansiveEdges.add(ExpansiveEdge(from, to))
            }
        }

        fun isEdgeInCycle(edge: ExpansiveEdge<T>) = edge.from in collectReachable(edge.to)

        private fun collectReachable(from: T): List<T> {
            val handler = object : DFS.NodeHandlerWithListResult<T, T>() {
                override fun afterChildren(current: T?) {
                    result.add(current)
                }
            }

            val neighbors = DFS.Neighbors<T> { current -> edgeLists[current] ?: emptySet() }

            DFS.dfs(listOf(from), neighbors, handler)

            return handler.result()
        }
    }
}

private fun ConeKotlinType.constituentTypes(): Set<ConeKotlinType> {
    val constituentTypes = SmartSet.create<ConeKotlinType>()
    forEachType { constituentTypes.add(it) }
    return constituentTypes
}

private fun substitutorByType(
    parameters: List<FirTypeParameterSymbol>,
    arguments: List<ConeTypeProjection>,
    session: FirSession,
): ConeSubstitutor {
    require(parameters.size == arguments.size)

    val substitution = buildMap {
        for (index in parameters.indices) {
            val argumentType = arguments[index].type
            if (argumentType != null) {
                put(parameters[index], argumentType)
            }
        }
    }

    return substitutorByMap(substitution, session)
}
