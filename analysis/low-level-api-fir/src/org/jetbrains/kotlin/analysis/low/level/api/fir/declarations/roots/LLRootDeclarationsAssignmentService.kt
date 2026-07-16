/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.roots.FirRootDeclarationAssignmentService

internal class LLRootDeclarationsAssignmentService : FirRootDeclarationAssignmentService {
    override fun assignRootDeclarationReferences(
        element: FirElement,
        root: FirDeclaration,
    ) {
        org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots.assignRootDeclarationReferences(element, root)
    }

    override fun assignRootDeclarationReferencesFrom(
        element: FirElement,
        from: FirDeclaration,
    ) {
        org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots.assignRootDeclarationReferencesFrom(element, from)
    }
}
