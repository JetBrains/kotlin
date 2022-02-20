/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.fir.FirSession

internal class LLFirSessionInvalidator(private val invalidateSourcesSession: (LLFirResolvableModuleSession) -> Unit) {
    fun invalidate(session: FirSession) {
        require(session is LLFirResolvableModuleSession)
        invalidateSourcesSession(session)
    }
}