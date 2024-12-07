/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession

abstract class FirScopeProvider {
    abstract fun getUseSiteMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: FirResolvePhase?,
    ): FirTypeScope

    abstract fun getStaticCallableMemberScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope?

    abstract fun getStaticCallableMemberScopeForBackend(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope?

    abstract fun getNestedClassifierScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope?

    fun getStaticScope(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return getStaticScopeImpl(klass, useSiteSession, scopeSession, this::getStaticCallableMemberScope)
    }

    fun getStaticScopeForBackend(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirContainingNamesAwareScope? {
        return getStaticScopeImpl(klass, useSiteSession, scopeSession, this::getStaticCallableMemberScopeForBackend)
    }

    private inline fun getStaticScopeImpl(
        klass: FirClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        callableMemberScope: (FirClass, FirSession, ScopeSession) -> FirContainingNamesAwareScope?
    ): FirContainingNamesAwareScope? {
        val nestedClassifierScope = getNestedClassifierScope(klass, useSiteSession, scopeSession)
        val callableScope = callableMemberScope(klass, useSiteSession, scopeSession)

        return when {
            nestedClassifierScope != null && callableScope != null ->
                FirNameAwareCompositeScope(listOf(nestedClassifierScope, callableScope))
            else -> nestedClassifierScope ?: callableScope
        }
    }
}

fun FirClass.staticScopeForBackend(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? =
    scopeProvider.getStaticScopeForBackend(this, session, scopeSession)
