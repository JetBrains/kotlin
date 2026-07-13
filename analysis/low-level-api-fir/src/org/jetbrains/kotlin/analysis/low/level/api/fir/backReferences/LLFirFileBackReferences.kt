/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.backReferences

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.ENABLE_FIR_FILE_BACK_REFERENCES
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.util.toDebugLocationDescription

// TODO (marco): Potentially move it to `symbols.backReferences` and split the FIR property and the constraints/the check.

private object FirFileBackReferenceKey : FirDeclarationDataKey()

/**
 * A back reference from [this] [FirDeclaration] to the [FirFile] which (transitively) contains it, or `null` if no back reference has been
 * assigned.
 *
 * See [ENABLE_FIR_FILE_BACK_REFERENCES] for the motivation. The reference is intentionally *strong*: it is what keeps the containing file
 * (and thereby all parents of the declaration) alive while the declaration is in use.
 *
 * The back reference is assigned by [assignFirFileBackReferences]:
 * - For non-local declarations, right after the raw FIR file has been built (see `LLFirFileBuilder`).
 * - For local declarations, right after a non-local declaration's body has been (re)built (see `RawFirNonLocalDeclarationBuilder`), reusing
 *   the file back reference already present on the non-local declaration.
 *
 * The property has no compiler counterpart: in compiler mode all FIR is trivially alive, so no back references are needed.
 */
internal var FirDeclaration.backReferencedFirFile: FirFile? by FirDeclarationDataRegistry.data(FirFileBackReferenceKey)

/**
 * Assigns a [back reference][backReferencedFirFile] to [firFile] to every [FirDeclaration] in the subtree rooted at [root], except [firFile]
 * itself.
 *
 * The traversal only descends into structural children, so it stays within the FIR tree of a single file and never follows semantic (symbol)
 * references to other files. At raw-FIR-building time, declaration bodies are lazy, so this visits exactly the non-local declarations. When a
 * body is later (re)built, running this over the rebuilt declaration additionally covers all local declarations created inside the body.
 *
 * Assigning the same reference twice is harmless and idempotent.
 */
internal fun assignFirFileBackReferences(root: FirElement, firFile: FirFile) {
    if (!ENABLE_FIR_FILE_BACK_REFERENCES) return

    root.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration && element !== firFile) {
                element.backReferencedFirFile = firFile
            }

            element.acceptChildren(this)
        }
    })
}

/**
 * Assigns a [back reference][backReferencedFirFile] to every [FirDeclaration] contained in [this] [FirFile].
 *
 * @see assignFirFileBackReferences
 */
internal fun FirFile.assignBackReferencesToDeclarations() {
    assignFirFileBackReferences(root = this, firFile = this)
}

/**
 * Checks that every *source* [FirDeclaration] in [firFile] carries a correct [back reference][backReferencedFirFile] to [firFile], and that
 * any back reference which happens to be present points to [firFile] (and not to some other file).
 *
 * The requirement is scoped to [source][FirDeclarationOrigin.Source] declarations because only those are produced by raw FIR building and
 * thus covered by the [assignment algorithm][assignFirFileBackReferences]. Synthetic and plugin-generated declarations (created during
 * resolution) are intentionally out of scope for the prototype; investigating them is a separate concern (see KT-70517).
 *
 * Due to its heaviness, the function should not be used in production. It is intended for usage in tests and assertions. It may be
 * temporarily used from a production location (e.g., via a flag), hence its placement in production sources.
 *
 * The function has no compiler counterpart. In compiler mode, FIR is trivially alive and no back references exist.
 */
@TestOnly
internal fun checkFirFileBackReferences(
    firFile: FirFile,
    lazyErrorTitle: () -> String = { "FIR file back reference violation" },
) {
    if (!ENABLE_FIR_FILE_BACK_REFERENCES) return

    firFile.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (element is FirDeclaration && element !== firFile) {
                checkDeclaration(element)
            }

            element.acceptChildren(this)
        }

        private fun checkDeclaration(declaration: FirDeclaration) {
            val backReference = declaration.backReferencedFirFile
            when {
                backReference == null -> {
                    if (declaration.origin == FirDeclarationOrigin.Source) {
                        error(
                            "${lazyErrorTitle()} (missing back reference):\n" +
                                    "  Expected a back reference to the containing FIR file, but found none.\n" +
                                    "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
                        )
                    }
                }

                backReference !== firFile -> {
                    error(
                        "${lazyErrorTitle()} (wrong back reference):\n" +
                                "  Expected the back reference to point to the containing FIR file '${firFile.name}',\n" +
                                "  but it points to '${backReference.name}'.\n" +
                                "  Declaration: ${declaration::class.simpleName} at ${declaration.source.toDebugLocationDescription()}"
                    )
                }
            }
        }
    })
}
