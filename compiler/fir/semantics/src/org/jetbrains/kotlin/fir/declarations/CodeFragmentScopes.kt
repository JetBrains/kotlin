/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCodeFragmentDeclarationScope

class TowerElementsForCodeFrament(
    val staticScope: FirScope?,
)

fun SessionHolder.collectTowerDataElementsForCodeFragment(owner: FirCodeFragment): TowerElementsForCodeFrament {

    return TowerElementsForCodeFrament(
        FirCodeFragmentDeclarationScope(session, owner)
    )
}