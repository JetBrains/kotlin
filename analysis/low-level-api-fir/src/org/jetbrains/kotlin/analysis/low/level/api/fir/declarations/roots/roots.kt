/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots

import org.jetbrains.kotlin.analysis.low.level.api.fir.ENABLE_FIR_BACK_REFERENCES
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * A **back reference** from [this] [FirDeclaration] to the *root declaration* which (transitively) contains it, or `null` if no back
 * reference has been assigned.
 *
 * The root declaration depends on the kind of FIR:
 *
 * - For **sources**, the root is the containing [FirFile].
 * - For **Kotlin libraries**, there is no FIR file, so the root is the top-level declaration.
 *
 * See [ENABLE_FIR_BACK_REFERENCES] for the motivation. The reference is intentionally *strong*: it is what keeps the root (and thereby all
 * parents of the declaration) alive while the declaration is in use.
 *
 * The root declaration references itself. While not strictly necessary for lifetime guarantees, it makes certain operations easier and more
 * consistent, such as retrieving a [FirDeclaration]'s root declaration.
 *
 * The property has no compiler counterpart: in compiler mode all FIR is trivially alive, so no back references are needed, nor are they
 * desired.
 *
 * @see assignRootDeclarationReferences
 */
internal var FirDeclaration.rootDeclaration: FirDeclaration? by FirDeclarationDataRegistry.data(RootDeclarationKey)

private object RootDeclarationKey : FirDeclarationDataKey()

/**
 * Assigns [root] as a [rootDeclaration] to every [FirDeclaration] in the subtree of [element], including [element] itself even when it is
 * the root declaration.
 *
 * The traversal only descends into structural children, so it stays within a single FIR tree and never follows semantic references.
 *
 * Assigning the same reference twice is harmless and idempotent.
 *
 * @see FirDeclaration.rootDeclaration
 */
internal fun assignRootDeclarationReferences(element: FirElement, root: FirDeclaration) {
    if (!ENABLE_FIR_BACK_REFERENCES) return

    element.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration) {
                element.rootDeclaration = root
            }

            element.acceptChildren(this)
        }
    })
}

/**
 * Assigns the [rootDeclaration] of [from] to every [FirDeclaration] in the subtree of [element], including [element] itself.
 *
 * This function should be used in cases where [from] already has an established root declaration, and [element] was generated later.
 *
 * @see assignRootDeclarationReferences
 */
internal fun assignRootDeclarationReferencesFrom(element: FirElement, from: FirDeclaration) {
    if (!ENABLE_FIR_BACK_REFERENCES) return

    val rootDeclaration = from.rootDeclaration
        ?: errorWithAttachment("Expected the parent to have an assigned root declaration.") {
            withFirEntry("element", element)
            withFirEntry("parent", from)
        }

    assignRootDeclarationReferences(element, rootDeclaration)
}

/**
 * Assigns [this] [FirFile] as a [rootDeclaration] to every [FirDeclaration] it contains (including the FIR file itself).
 *
 * @see assignRootDeclarationReferences
 */
internal fun FirFile.assignRootDeclarationReferences() {
    assignRootDeclarationReferences(element = this, root = this)
}
