/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbols.id

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId
import org.jetbrains.kotlin.fir.utils.checkDistinctKeys
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.util.toDebugLocationDescription

/**
 * Checks all symbol ID constraints of "FIR as Data" (KT-84343) for the given non-unique [roots]:
 *
 * 1. All declarations must have non-unique (source-based) symbol IDs ([checkNonUniqueSymbolIds]).
 * 2. All symbol IDs must be distinct within the roots ([checkDistinctSymbolIds]).
 *
 * Due to its heaviness, the function should not be used in production. It is intended for usage in tests and assertions. It may be
 * temporarily used from a production location (e.g., via a flag), hence its placement in production sources.
 *
 * The function has no compiler counterpart. In compiler mode, FIR declarations universally have unique symbol IDs, so constraints for
 * non-unique FIR declarations do not apply.
 */
@TestOnly
internal fun checkSymbolIdConstraints(
    roots: List<FirElement>,
    lazyErrorTitle: () -> String = { "Symbol ID constraint violation" },
) {
    checkNonUniqueSymbolIds(roots, lazyErrorTitle)
    checkDistinctSymbolIds(roots, lazyErrorTitle)
}

/**
 * Checks that all [FirDeclaration]s in the given [roots] have non-unique (source-based) symbol IDs. This is the first constraint of
 * "FIR as Data" (KT-84343): "Any child of a non-unique FIR declaration must also be non-unique."
 *
 * While the constraint is formulated recursively, it suffices to check all declarations in the given roots: if every declaration is
 * non-unique, the recursive formulation is trivially satisfied.
 */
@TestOnly
private fun checkNonUniqueSymbolIds(roots: List<FirElement>, lazyErrorTitle: () -> String) {
    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val symbolId = declaration.symbol.symbolId
            if (symbolId is FirUniqueSymbolId) {
                error(
                    "${lazyErrorTitle()} (illegal unique symbol ID):\n" +
                            "  Expected a non-unique (source-based) symbol ID, but found a unique symbol ID.\n" +
                            "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
                )
            }
        }
    }

    roots.forEach { it.accept(visitor) }
}

/**
 * Checks that all [FirDeclaration]s in the given [roots] have distinct [FirSymbolId]s. This is the second constraint of "FIR as Data"
 * (KT-84343): "Within any non-unique cache root, all symbol IDs must be distinct."
 */
@TestOnly
private fun checkDistinctSymbolIds(roots: List<FirElement>, lazyErrorTitle: () -> String) {
    checkDistinctKeys(
        roots,
        keyExtractor = { it.symbol.symbolId },
        lazyErrorTitle = { _, _ -> "${lazyErrorTitle()} (duplicate symbol IDs)" },
        formatLocation = { it.source.toDebugLocationDescription() },
    )
}
