/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.*

@OptIn(PrivateSessionConstructor::class)
class FirCliSession @PrivateSessionConstructor constructor(
    sessionProvider: FirProjectSessionProvider,
    val kind: Kind // used for debug
) : FirSession(sessionProvider) {
    enum class Kind {
        Source, Library
    }
}

class FirProjectSessionProvider : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? {
        return sessionCache[moduleData]
    }

    fun registerSession(moduleData: FirModuleData, session: FirSession) {
        sessionCache[moduleData] = session
    }

    private val sessionCache: MutableMap<FirModuleData, FirSession> = mutableMapOf()
}
