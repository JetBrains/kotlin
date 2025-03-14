/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeCyclicTypeBound
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirCyclicTypeBoundsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirMemberDeclaration) return
        if (declaration is FirConstructor || declaration is FirTypeAlias) return
        val actualTypeParameters = declaration.typeParameters.filterNot { it is FirOuterClassTypeParameterRef }.takeIf { it.isNotEmpty() }
            ?: return

        val processed = mutableSetOf<FirTypeParameterSymbol>()
        val typeParameterCycles = mutableListOf<List<FirTypeParameterSymbol>>()
        val path = mutableListOf<FirTypeParameterSymbol>()

        fun findCycles(typeParameterSymbol: FirTypeParameterSymbol) {
            if (processed.add(typeParameterSymbol)) {
                path.add(typeParameterSymbol)
                val resolvedBounds = typeParameterSymbol.resolvedBounds.flatMap { extractTypeParamSymbols(it) }.toSet()
                resolvedBounds.forEach {
                    findCycles(it)
                }
                path.removeAt(path.size - 1)
            } else if (path.isNotEmpty()) {
                typeParameterCycles.add(path.dropWhile { it != typeParameterSymbol })
            }
        }

        actualTypeParameters.forEach { param -> findCycles(param.symbol) }

        for (typeParameterCycle in typeParameterCycles) {
            for (typeParameter in typeParameterCycle) {
                //for some reason FE 1.0 report differently for class declarations
                val targets = if (declaration is FirRegularClass) {
                    typeParameter.originalBounds().filter { typeParameterCycle.contains(extractTypeParamSymbol(it.coneType)) }.mapNotNull { it.source }
                } else {
                    listOf(typeParameter.source)
                }
                targets.forEach {
                    reporter.reportOn(it, FirErrors.CYCLIC_GENERIC_UPPER_BOUND, typeParameterCycle, context)
                }
            }
        }
    }

    private fun FirTypeParameterSymbol.originalBounds() = resolvedBounds.flatMap { it.unwrapBound() }

    private fun FirTypeRef.unwrapBound(): List<FirTypeRef> =
        if (this is FirErrorTypeRef && diagnostic is ConeCyclicTypeBound) {
            (diagnostic as ConeCyclicTypeBound).bounds
        } else {
            listOf(this)
        }


    private fun extractTypeParamSymbols(ref: FirTypeRef): List<FirTypeParameterSymbol> =
        ref.unwrapBound().mapNotNull { extractTypeParamSymbol(it.coneType) }

    private fun extractTypeParamSymbol(type: ConeKotlinType): FirTypeParameterSymbol? =
        (type.unwrapToSimpleTypeUsingLowerBound() as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol
}
