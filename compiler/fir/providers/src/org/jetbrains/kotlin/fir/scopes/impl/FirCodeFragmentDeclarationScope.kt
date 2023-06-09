/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

class FirCodeFragmentDeclarationScope(val useSiteSession: FirSession,
                                      val firCodeFragment: FirCodeFragment,) : FirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getClassifierNames(): Set<Name> {
        TODO("Not yet implemented")
    }
}