/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession

interface LLScopeSessionProvider {
    fun getScopeSession(session: LLFirSession): ScopeSession
}

internal object LLDefaultScopeSessionProvider : LLScopeSessionProvider {
    override fun getScopeSession(session: LLFirSession): ScopeSession {
        return session.getScopeSession()
    }
}