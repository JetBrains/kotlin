/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.ENABLE_FIR_BACK_REFERENCES
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.util.toDebugLocationDescription

/**
 * Checks that every [FirDeclaration] in the subtree of [root] carries a correct [root declaration reference][rootDeclaration] exactly to
 * [root] (and not to some other root).
 *
 * Due to its heaviness, the function should not be used in production. It is intended for usage in tests and assertions. It may be
 * temporarily used from a production location (e.g., via a flag), hence its placement in production sources.
 *
 * The function has no compiler counterpart. In compiler mode, FIR is trivially alive and no back references exist.
 */
@TestOnly
internal fun checkRootDeclarationReferences(
    root: FirDeclaration,
    lazyErrorTitle: () -> String = { "Back reference violation" },
) {
    if (!ENABLE_FIR_BACK_REFERENCES) return

    root.accept(object : FirVisitorVoid() {
        @TestOnly
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                checkRootDeclarationReference(element, root, lazyErrorTitle)
            }

            element.acceptChildren(this)
        }
    })
}

@TestOnly
private fun checkRootDeclarationReference(
    declaration: FirDeclaration,
    expectedRoot: FirDeclaration,
    lazyErrorTitle: () -> String,
) {
    val actualRoot = declaration.rootDeclaration
    when {
        actualRoot == null -> {
            error(
                "${lazyErrorTitle()} (missing root declaration reference):\n" +
                        "  Expected a back reference to the containing root declaration, but found none.\n" +
                        "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
            )
        }

        actualRoot !== expectedRoot -> {
            error(
                "${lazyErrorTitle()} (wrong root declaration reference):\n" +
                        "  Expected the back reference to point to the root declaration ${expectedRoot::class.simpleName} at " +
                        "${expectedRoot.source.toDebugLocationDescription()},\n" +
                        "  but it points to ${actualRoot::class.simpleName} at " +
                        "${actualRoot.source.toDebugLocationDescription()}.\n" +
                        "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
            )
        }
    }
}
