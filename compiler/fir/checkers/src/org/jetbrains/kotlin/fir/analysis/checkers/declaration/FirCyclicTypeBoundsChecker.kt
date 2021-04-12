/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCyclicTypeBound
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirCyclicTypeBoundsChecker : FirMemberDeclarationChecker() {

    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirConstructor || declaration is FirTypeAlias) return

        val processed = mutableSetOf<Name>()
        val cycles = mutableSetOf<Name>()
        val graph = declaration.typeParameters.map { param ->
            param.symbol.name to param.symbol.fir.bounds.flatMap { extractTypeParamNames(it) }.toSet()
        }.toMap()

        declaration.typeParameters.forEach { param ->
            if (!processed.contains(param.symbol.name)) {
                findCycles(
                    persistentListOf(),
                    param.symbol.name,
                    processed,
                    mutableSetOf(),
                    cycles
                ) { name -> graph.getOrDefault(name, emptySet()) }
            }
        }

        if (cycles.isNotEmpty()) {
            declaration.typeParameters
                .filter { cycles.contains(it.symbol.name) }
                .forEach { param ->
                    //for some reason FE 1.0 report differently for class declarations
                    val targets = if (declaration is FirRegularClass) {
                        param.symbol.fir.originalBounds().filter { cycles.contains(extractTypeParamName(it.coneType)) }
                            .mapNotNull { it.source }
                    } else {
                        listOf(param.source)
                    }
                    targets.forEach {
                        reporter.reportOn(it, FirErrors.CYCLIC_GENERIC_UPPER_BOUND, context)
                    }
                }
        }
    }

    private fun FirTypeParameter.originalBounds() = bounds.flatMap { it.unwrapBound() }

    private fun FirTypeRef.unwrapBound(): List<FirTypeRef> =
        if (this is FirErrorTypeRef && diagnostic is ConeCyclicTypeBound) {
            (diagnostic as ConeCyclicTypeBound).bounds
        } else {
            listOf(this)
        }


    private fun extractTypeParamNames(ref: FirTypeRef): Set<Name> =
        ref.unwrapBound().mapNotNull { extractTypeParamName(it.coneType) }.toSet()

    private fun extractTypeParamName(type: ConeKotlinType): Name? = type.safeAs<ConeTypeParameterType>()?.lookupTag?.name

    private fun findCycles(
        path: PersistentList<Name>,
        node: Name,
        processed: MutableSet<Name>,
        visited: MutableSet<Name>,
        cycles: MutableSet<Name>,
        graph: (Name) -> Set<Name>
    ) {
        processed.add(node)
        if (visited.add(node)) {
            val newPath = path.add(node)
            graph(node).forEach { nextNode ->
                findCycles(newPath, nextNode, processed, visited, cycles, graph)
            }
        } else {
            cycles.addAll(path.dropWhile { it != node })
        }
    }

}
