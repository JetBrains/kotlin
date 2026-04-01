/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.resolve.ScopeSession

interface SessionHolder {
    val session: FirSession
}

interface ScopeSessionHolder {
    val scopeSession: ScopeSession
}

interface SessionAndScopeSessionHolder : SessionHolder, ScopeSessionHolder

inline fun <R> withSession(session: FirSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionHolder {
        override val session: FirSession
            get() = session
    }
    return block(holder)
}

inline fun <R> withSession(session: FirSession, scopeSession: ScopeSession, block: context(SessionHolder) () -> R): R {
    val holder = object : SessionAndScopeSessionHolder {
        override val session: FirSession
            get() = session

        override val scopeSession: ScopeSession
            get() = scopeSession
    }
    return block(holder)
}
