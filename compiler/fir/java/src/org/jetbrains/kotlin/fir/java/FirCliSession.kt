/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.name.Name

@OptIn(PrivateSessionConstructor::class)
class FirCliSession @PrivateSessionConstructor constructor(
    sessionProvider: FirProjectSessionProvider,
    kind: Kind
) : FirSession(sessionProvider, kind)

class FirProjectSessionProvider : FirSessionProvider() {
    override fun getSession(moduleData: FirModuleData): FirSession? {
        return sessionCache[moduleData]
    }

    fun getSessionByName(moduleDataName: Name): FirSession? {
        return sessionCache.asIterable().find { it.key.name == moduleDataName }?.value
    }

    fun registerSession(moduleData: FirModuleData, session: FirSession) {
        sessionCache[moduleData] = session
    }

    private val sessionCache: MutableMap<FirModuleData, FirSession> = mutableMapOf()
}
