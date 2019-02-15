/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope

class FirCompositeScopeProvider(private val providers: List<FirScopeProvider>) : FirScopeProvider {
    override fun getDeclaredMemberScope(klass: FirRegularClass, session: FirSession): FirScope {
        return FirCompositeScope(providers.mapTo(mutableListOf()) { it.getDeclaredMemberScope(klass, session) })
    }
}