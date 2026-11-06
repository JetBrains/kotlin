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
import org.jetbrains.kotlin.fir.symbols.symbolId
import org.jetbrains.kotlin.fir.utils.checkDistinctKeys
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.util.toDebugLocationDescription

/**
 * Checks the symbol ID constraints for discardable declarations (see [FirSymbolId]) for the given discardable root declarations [roots]:
 *
 * 1. **Source-based symbol IDs:** All declarations must have source-based symbol IDs ([checkSourceBasedSymbolIds]).
 * 2. **Distinct symbol IDs:** All symbol IDs must be distinct within the root declarations ([checkDistinctSymbolIds]).
 *
 * Due to its heaviness, the function should not be used in production. It is intended for usage in tests and assertions. It may be
 * temporarily used from a production location (e.g., via a flag), hence its placement in production sources.
 *
 * The function has no compiler counterpart. In compiler mode, FIR declarations universally have unique symbol IDs, so the constraints for
 * discardable declarations do not apply.
 */
@TestOnly
internal fun checkSymbolIdConstraints(
    roots: List<FirElement>,
    lazyErrorTitle: () -> String = { "Symbol ID constraint violation" },
) {
    checkSourceBasedSymbolIds(roots, lazyErrorTitle)
    checkDistinctSymbolIds(roots, lazyErrorTitle)
}

/**
 * Checks that all [FirDeclaration]s in the given [roots] have source-based symbol IDs. This is the first symbol ID constraint for
 * discardable declarations (see [FirSymbolId]): "Any child declaration of a discardable declaration must have a source-based symbol ID."
 *
 * While the constraint is formulated recursively, it suffices to check all declarations in the given roots: if every declaration is
 * discardable, the recursive formulation is trivially satisfied.
 */
@TestOnly
private fun checkSourceBasedSymbolIds(roots: List<FirElement>, lazyErrorTitle: () -> String) {
    val visitor = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val symbolId = declaration.symbol.symbolId
            if (symbolId is FirUniqueSymbolId<*>) {
                error(
                    "${lazyErrorTitle()} (illegal unique symbol ID):\n" +
                            "  Expected a source-based symbol ID, but found a unique symbol ID.\n" +
                            "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
                )
            }
        }
    }

    roots.forEach { it.accept(visitor) }
}

/**
 * Checks that all [FirDeclaration]s in the given [roots] have distinct [FirSymbolId]s. This is the second symbol ID constraint for
 * discardable declarations (see [FirSymbolId]), checked within the given root declarations: "The symbol IDs of different declarations must
 * never be equal."
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
