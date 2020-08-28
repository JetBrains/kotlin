/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.ScopeSession

interface FirEffectiveVisibilityResolver : FirSessionComponent {
    fun resolveFor(
        declaration: FirMemberDeclaration,
        containingDeclarations: List<FirDeclaration>?,
        scopeSession: ScopeSession
    ): FirEffectiveVisibility
}