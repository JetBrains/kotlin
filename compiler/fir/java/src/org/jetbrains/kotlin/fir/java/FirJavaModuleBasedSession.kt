/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.*

class FirJavaModuleBasedSession(
    moduleInfo: ModuleInfo,
    override val sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope,
    dependenciesProvider: FirSymbolProvider? = null
) : FirModuleBasedSession(moduleInfo) {
    init {
        sessionProvider.sessionCache[moduleInfo] = this
        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    service<FirProvider>(),
                    JavaSymbolProvider(this, sessionProvider.project, scope),
                    FirLibrarySymbolProviderImpl(this),
                    dependenciesProvider ?: FirDependenciesSymbolProviderImpl(this)
                )
            )
        )
    }
}

class FirLibrarySession(
    moduleInfo: ModuleInfo,
    override val sessionProvider: FirProjectSessionProvider,
    scope: GlobalSearchScope
) : FirSessionBase() {
    init {
        sessionProvider.sessionCache[moduleInfo] = this
        registerComponent(
            FirSymbolProvider::class,
            FirCompositeSymbolProvider(
                listOf(
                    FirLibrarySymbolProviderImpl(this),
                    JavaSymbolProvider(this, sessionProvider.project, scope),
                    FirDependenciesSymbolProviderImpl(this)
                )
            )
        )
    }
}

class FirProjectSessionProvider(val project: Project) : FirSessionProvider {
    override fun getSession(moduleInfo: ModuleInfo): FirSession? {
        return sessionCache[moduleInfo]
    }

    val sessionCache = mutableMapOf<ModuleInfo, FirSession>()
}