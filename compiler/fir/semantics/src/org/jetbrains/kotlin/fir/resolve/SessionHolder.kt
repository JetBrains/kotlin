/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession

interface SessionHolder {
    val session: FirSession
    val scopeSession: ScopeSession
}

data class SessionHolderImpl(override val session: FirSession, override val scopeSession: ScopeSession) : SessionHolder {
    companion object {
        fun createWithEmptyScopeSession(session: FirSession): SessionHolderImpl = SessionHolderImpl(session, ScopeSession())
    }
}