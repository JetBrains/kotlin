/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

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

    override fun check(declaration: FirMemberDeclaration<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirConstructor || declaration is FirTypeAlias) return

        val processed = mutableSetOf<Name>()
        val cycles = mutableSetOf<Name>()
        val graph = declaration.typeParameters.associate { param ->
            param.symbol.name to param.symbol.fir.bounds.flatMap { extractTypeParamNames(it) }.toSet()
        }
        val graphFunc = { name: Name -> graph.getOrDefault(name, emptySet()) }
        val path = mutableListOf<Name>()

        fun findCycles(
            node: Name
        ) {
            if (processed.add(node)) {
                path.add(node)
                graphFunc(node).forEach { nextNode ->
                    findCycles(nextNode)
                }
                path.removeAt(path.size - 1)
            } else {
                cycles.addAll(path.dropWhile { it != node })
            }
        }

        declaration.typeParameters.forEach { param -> findCycles(param.symbol.name) }

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
}
