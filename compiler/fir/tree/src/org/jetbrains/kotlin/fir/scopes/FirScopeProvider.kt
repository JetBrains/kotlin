/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor

abstract class FirScopeProvider {
    abstract fun getUseSiteMemberScope(
        klass: FirClass<*>,
        substitutor: ConeSubstitutor,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope

    abstract fun getStaticMemberScopeForCallables(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope?
}
