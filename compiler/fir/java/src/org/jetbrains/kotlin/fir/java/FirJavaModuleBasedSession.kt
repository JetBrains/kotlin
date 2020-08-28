/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.*

@OptIn(PrivateSessionConstructor::class)
class FirJavaModuleBasedSession @PrivateSessionConstructor constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
) : FirModuleBasedSession(moduleInfo, sessionProvider) {
    init {
        sessionProvider.registerSession(moduleInfo, this)
    }
}

@OptIn(PrivateSessionConstructor::class)
class FirLibrarySession @PrivateSessionConstructor constructor(
    override val moduleInfo: ModuleInfo,
    sessionProvider: FirProjectSessionProvider,
) : FirSession(sessionProvider) {
    init {
        sessionProvider.registerSession(moduleInfo, this)
    }
}

open class FirProjectSessionProvider(override val project: Project) : FirSessionProvider {
    override fun getSession(moduleInfo: ModuleInfo): FirSession? {
        return sessionCache[moduleInfo]
    }

    fun registerSession(moduleInfo: ModuleInfo, session: FirSession) {
        sessionCache[moduleInfo] = session
    }

    protected open val sessionCache: MutableMap<ModuleInfo, FirSession> = mutableMapOf()
}
