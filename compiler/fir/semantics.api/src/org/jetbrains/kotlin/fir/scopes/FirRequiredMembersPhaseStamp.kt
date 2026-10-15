/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.ScopeSessionKey

/**
 * The highest required members phase a cached scope has been requested with.
 *
 * Scopes are cached regardless of the required members phase, while it is a guarantee for the members of the entire hierarchy.
 * The first build requests the scopes of supertypes with the same phase, so it resolves the hierarchy by itself.
 * A scope built with a lower phase is stale, so its hierarchy has to be resolved once before it is reused with a higher phase.
 *
 * @see org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseWithCallableMembersInSupertypes
 */
class FirRequiredMembersPhaseStamp {
    @PublishedApi
    internal var isBuilt: Boolean = false

    @PublishedApi
    internal var phase: FirResolvePhase? = null
}

fun <ID : Any> requiredMembersPhaseStampKey(): ScopeSessionKey<ID, FirRequiredMembersPhaseStamp> {
    return object : ScopeSessionKey<ID, FirRequiredMembersPhaseStamp>() {}
}

/**
 * Must be called before the lookup of the scope cached by [id] in the same [ScopeSession].
 *
 * @param resolveOwner resolves the owner of the scope before its first build
 * @param resolveHierarchy resolves the entire hierarchy of the scope before the reuse of a stale cached scope
 */
inline fun <reified ID : Any> ScopeSession.ensureRequiredMembersPhase(
    id: ID,
    key: ScopeSessionKey<ID, FirRequiredMembersPhaseStamp>,
    requiredPhase: FirResolvePhase?,
    resolveOwner: (FirResolvePhase) -> Unit,
    resolveHierarchy: (FirResolvePhase) -> Unit,
) {
    val stamp = getOrBuild(id, key) { FirRequiredMembersPhaseStamp() }
    if (requiredPhase == null) {
        stamp.isBuilt = true
        return
    }

    val stampPhase = stamp.phase
    if (stampPhase != null && stampPhase >= requiredPhase) return

    if (stamp.isBuilt) {
        resolveHierarchy(requiredPhase)
    } else {
        resolveOwner(requiredPhase)
    }

    stamp.isBuilt = true
    stamp.phase = requiredPhase
}
