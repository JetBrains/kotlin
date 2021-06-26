/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession

abstract class FirScopeProvider {
    abstract fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirTypeScope

    abstract fun getStaticMemberScopeForCallables(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope?

    abstract fun getNestedClassifierScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope?

    fun getStaticScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val nestedClassifierScope = getNestedClassifierScope(klass, useSiteSession, scopeSession)
        val callableScope = getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession)

        return when {
            nestedClassifierScope != null && callableScope != null ->
                FirCompositeScope(listOf(nestedClassifierScope, callableScope))
            else -> nestedClassifierScope ?: callableScope
        }
    }
}
