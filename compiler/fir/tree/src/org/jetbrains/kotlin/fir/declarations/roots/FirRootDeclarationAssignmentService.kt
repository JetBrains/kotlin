/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.roots

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

// TODO (marco): This is a WORKAROUND for necessary assignments of root declarations from FIR compiler sources. Root declarations are only
//  really a concept in the Analysis API now, but they might become a proper compiler concept depending on how we can ensure consistent
//  back reference assignment across the board.
interface FirRootDeclarationAssignmentService : FirSessionComponent {
    fun assignRootDeclarationReferences(element: FirElement, root: FirDeclaration)
    fun assignRootDeclarationReferencesFrom(element: FirElement, from: FirDeclaration)
}

val FirSession.rootDeclarationAssignmentService: FirRootDeclarationAssignmentService? by FirSession.sessionComponentAccessor()
