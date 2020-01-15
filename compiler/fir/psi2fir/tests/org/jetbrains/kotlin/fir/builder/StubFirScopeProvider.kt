/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider

object StubFirScopeProvider : FirScopeProvider() {
    override fun getUseSiteMemberScope(
        klass: FirClass<*>,
        substitutor: ConeSubstitutor,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope {
        error("Stub")
    }

    override fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        return null
    }
}